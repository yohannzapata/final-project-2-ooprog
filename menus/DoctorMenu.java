package menus;

import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import models.Appointment;
import models.AppointmentStatus;
import models.Doctor;
import models.MedicalRecord;
import models.Patient;
import services.AppointmentService;
import util.Format;
import util.InputHelper;
import util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Doctor menu: own appointments, own patients and their medical records only.
 */
public class DoctorMenu extends Menu {

    private final Doctor doctor;
    private final AppointmentService appointments = new AppointmentService();

    public DoctorMenu(Doctor doctor) {
        this.doctor = doctor;
    }

    @Override
    protected String getTitle() {
        return "DOCTOR MENU";
    }

    @Override
    protected void printSummary() {
        int today = 0;
        for (Appointment a : appointments.getByDoctor(doctor.getId())) {
            if (a.isScheduled() && a.getDate().equals(LocalDate.now())) {
                today++;
            }
        }
        System.out.println("Welcome, " + doctor.getDisplayName() + " (" + doctor.getSpecialization() + ")");
        System.out.println("You have " + today + " scheduled appointment(s) today.");
        System.out.println();
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "View today's appointments",
                "View upcoming appointments",
                "View all my appointments",
                "Mark appointment as completed",
                "Mark appointment as no-show",
                "View my patients",
                "View patient details and history",
                "Add medical record",
                "View medical records",
                "Update medical record",
                "Delete medical record",
                "View my profile",
                "Change password"
        };
    }

    @Override
    protected String getExitLabel() {
        return "Logout";
    }

    @Override
    protected boolean onExit() {
        return InputHelper.readYesNo("Log out?");
    }

    @Override
    protected void handle(int choice) throws InvalidInputException, DuplicateRecordException {
        switch (choice) {
            case 1:
                viewToday();
                break;
            case 2:
                viewUpcoming();
                break;
            case 3:
                appointments.printTable(appointments.getByDoctor(doctor.getId()));
                break;
            case 4:
                completeAppointment();
                break;
            case 5:
                markNoShow();
                break;
            case 6:
                PatientManagementMenu.printTable(getMyPatients());
                break;
            case 7:
                viewPatientHistory();
                break;
            case 8:
                addRecord();
                break;
            case 9:
                viewRecords();
                break;
            case 10:
                updateRecord();
                break;
            case 11:
                deleteRecord();
                break;
            case 12:
                DoctorManagementMenu.printDetails(doctor);
                break;
            case 13:
                Prompts.changePassword(doctor);
                break;
            default:
                break;
        }
    }

    // ---------------------------------------------------------------- appointments

    private Appointment findOwnAppointment(String id) throws RecordNotFoundException {
        Appointment a = data.findAppointment(id);
        if (!a.getDoctorId().equals(doctor.getId())) {
            // Same message as a missing ID so other doctors' appointments are not revealed.
            throw new RecordNotFoundException("No appointment found with ID " + id + " in your schedule.");
        }
        return a;
    }

    private void viewToday() {
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : appointments.getByDoctor(doctor.getId())) {
            if (a.getDate().equals(LocalDate.now())) {
                list.add(a);
            }
        }
        System.out.println("Appointments today, " + Format.date(LocalDate.now()) + ":");
        appointments.printTable(list);
    }

    private void viewUpcoming() {
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : appointments.getByDoctor(doctor.getId())) {
            if (a.isScheduled() && !a.getDate().isBefore(LocalDate.now())) {
                list.add(a);
            }
        }
        appointments.printTable(list);
    }

    private void completeAppointment() throws InvalidInputException, DuplicateRecordException {
        Appointment a = Prompts.select("Enter appointment ID: ", this::findOwnAppointment);
        appointments.printDetails(a);
        appointments.complete(a);
        Format.success("Appointment " + a.getId() + " marked as completed.");
        if (InputHelper.readYesNo("Add the medical record for this consultation now?")) {
            createRecord(a);
        }
    }

    private void markNoShow() throws InvalidInputException {
        Appointment a = Prompts.select("Enter appointment ID: ", this::findOwnAppointment);
        appointments.printDetails(a);
        if (!a.isScheduled() || a.isUpcoming()) {
            // Let the service give the exact reason before asking for confirmation.
            appointments.markNoShow(a);
        }
        if (!InputHelper.confirm("Mark this patient as no-show?")) {
            Format.info("Nothing was changed.");
            return;
        }
        appointments.markNoShow(a);
        Format.success("Appointment " + a.getId() + " marked as no-show.");
    }

    // ---------------------------------------------------------------- patients

    /** Patients who have at least one appointment with this doctor. */
    private List<Patient> getMyPatients() {
        Set<String> ids = new LinkedHashSet<>();
        for (Appointment a : appointments.getByDoctor(doctor.getId())) {
            ids.add(a.getPatientId());
        }
        List<Patient> result = new ArrayList<>();
        for (Patient p : data.getPatients()) {
            if (ids.contains(p.getId())) {
                result.add(p);
            }
        }
        return result;
    }

    private Patient findOwnPatient(String id) throws RecordNotFoundException {
        for (Patient p : getMyPatients()) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        throw new RecordNotFoundException("No patient with ID " + id + " among your patients.");
    }

    private void viewPatientHistory() {
        Patient patient = Prompts.select("Enter patient ID: ", this::findOwnPatient);
        PatientForm.printProfile(patient);
        Format.subHeader("Appointments with you");
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : appointments.getByDoctor(doctor.getId())) {
            if (a.getPatientId().equals(patient.getId())) {
                list.add(a);
            }
        }
        appointments.printTable(list);
        Format.subHeader("Medical records you wrote");
        printRecords(getMyRecords(patient.getId()));
    }

    // ---------------------------------------------------------------- medical records

    private MedicalRecord findOwnRecord(String id) throws RecordNotFoundException {
        MedicalRecord r = data.findMedicalRecord(id);
        if (!r.getDoctorId().equals(doctor.getId())) {
            throw new RecordNotFoundException("No medical record found with ID " + id + " among your records.");
        }
        return r;
    }

    private List<MedicalRecord> getMyRecords(String patientId) {
        List<MedicalRecord> result = new ArrayList<>();
        for (MedicalRecord r : data.getMedicalRecords()) {
            if (r.getDoctorId().equals(doctor.getId()) && (patientId == null || r.getPatientId().equals(patientId))) {
                result.add(r);
            }
        }
        result.sort(Comparator.comparing(MedicalRecord::getConsultationDate).reversed());
        return result;
    }

    private MedicalRecord findRecordForAppointment(String appointmentId) {
        for (MedicalRecord r : data.getMedicalRecords()) {
            if (r.getAppointmentId().equals(appointmentId)) {
                return r;
            }
        }
        return null;
    }

    private void addRecord() throws InvalidInputException, DuplicateRecordException {
        List<Appointment> pending = new ArrayList<>();
        for (Appointment a : appointments.getByDoctor(doctor.getId())) {
            if (a.getStatus() == AppointmentStatus.COMPLETED && findRecordForAppointment(a.getId()) == null) {
                pending.add(a);
            }
        }
        if (pending.isEmpty()) {
            Format.info("All your completed appointments already have medical records.");
            return;
        }
        System.out.println("Completed appointments without a medical record:");
        appointments.printTable(pending);
        Appointment a = Prompts.select("Enter appointment ID: ", this::findOwnAppointment);
        createRecord(a);
    }

    private void createRecord(Appointment a) throws InvalidInputException, DuplicateRecordException {
        if (a.getStatus() != AppointmentStatus.COMPLETED) {
            throw new InvalidInputException("Medical records can only be added to completed appointments.");
        }
        MedicalRecord existing = findRecordForAppointment(a.getId());
        if (existing != null) {
            throw new DuplicateRecordException("Appointment " + a.getId() + " already has medical record "
                    + existing.getId() + ". Use Update instead.");
        }
        System.out.println("Patient: " + data.patientName(a.getPatientId()) + " | Consultation: " + Format.date(a.getDate()));
        String diagnosis = InputHelper.readText("Diagnosis: ", "Diagnosis", 200, true);
        String prescription = InputHelper.readText("Prescription (e.g. Paracetamol 500mg, 1 tab every 6 hrs; "
                + "Enter if none): ", "Prescription", 300, false);
        String notes = InputHelper.readText("Notes / advice (optional): ", "Notes", 500, false);

        MedicalRecord record = new MedicalRecord(data.nextMedicalRecordId(), a.getId(), a.getPatientId(),
                doctor.getId(), a.getDate(), diagnosis, prescription.isEmpty() ? "None" : prescription, notes);
        data.getMedicalRecords().add(record);
        data.save();
        Format.success("Medical record " + record.getId() + " saved.");
    }

    private void viewRecords() {
        List<MedicalRecord> records = getMyRecords(null);
        printRecords(records);
        if (!records.isEmpty() && InputHelper.readYesNo("View full details of a record?")) {
            printRecord(Prompts.select("Enter record ID: ", this::findOwnRecord));
        }
    }

    private void updateRecord() {
        MedicalRecord r = Prompts.select("Enter record ID: ", this::findOwnRecord);
        printRecord(r);
        System.out.println("Press Enter to keep the current value.");
        String diagnosis = InputHelper.readOptional("Diagnosis: ",
                s -> Validator.validateText(s, "Diagnosis", 200, true));
        String prescription = InputHelper.readOptional("Prescription: ",
                s -> Validator.validateText(s, "Prescription", 300, true));
        String notes = InputHelper.readOptional("Notes (type \"none\" to clear): ",
                s -> s.trim().equalsIgnoreCase("none") ? "" : Validator.validateText(s, "Notes", 500, true));
        if (diagnosis == null && prescription == null && notes == null) {
            Format.info("No changes made.");
            return;
        }
        if (diagnosis != null) {
            r.setDiagnosis(diagnosis);
        }
        if (prescription != null) {
            r.setPrescription(prescription);
        }
        if (notes != null) {
            r.setNotes(notes);
        }
        data.save();
        Format.success("Medical record " + r.getId() + " updated.");
    }

    private void deleteRecord() {
        MedicalRecord r = Prompts.select("Enter record ID: ", this::findOwnRecord);
        printRecord(r);
        if (!InputHelper.confirm("Delete medical record " + r.getId() + "?")) {
            Format.info("Delete cancelled.");
            return;
        }
        data.getMedicalRecords().remove(r);
        data.save();
        Format.success("Medical record deleted.");
    }

    // ---------------------------------------------------------------- printing

    static void printRecords(List<MedicalRecord> records) {
        if (records.isEmpty()) {
            Format.info("No medical records found.");
            return;
        }
        HospitalData data = HospitalData.getInstance();
        String fmt = "%-7s %-13s %-22s %-24s %-30s%n";
        System.out.printf(fmt, "ID", "Date", "Patient", "Doctor", "Diagnosis");
        Format.line(98);
        for (MedicalRecord r : records) {
            System.out.printf(fmt, r.getId(), Format.date(r.getConsultationDate()),
                    Format.fit(data.patientName(r.getPatientId()), 22), Format.fit(data.doctorName(r.getDoctorId()), 24),
                    Format.fit(r.getDiagnosis(), 30));
        }
    }

    static void printRecord(MedicalRecord r) {
        HospitalData data = HospitalData.getInstance();
        System.out.println("Record ID      : " + r.getId());
        System.out.println("Patient        : " + data.patientName(r.getPatientId()) + " (" + r.getPatientId() + ")");
        System.out.println("Doctor         : " + data.doctorName(r.getDoctorId()));
        System.out.println("Consultation   : " + Format.date(r.getConsultationDate()) + " (" + r.getAppointmentId() + ")");
        System.out.println("Diagnosis      : " + r.getDiagnosis());
        System.out.println("Prescription   : " + r.getPrescription());
        System.out.println("Notes          : " + Format.orDash(r.getNotes()));
        System.out.println("Last updated   : " + Format.date(r.getLastUpdated()));
    }
}
