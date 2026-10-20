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
import java.time.LocalDateTime;
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
        for (Appointment a : getMyAppointments()) {
            if (a.isScheduled() && a.getDate().equals(LocalDate.now())) {
                today++;
            }
        }
        System.out.println("Welcome, " + doctor.getDisplayName() + " (" + doctor.getSpecialization() + ")");
        System.out.println("You have " + today + " scheduled appointment(s) today.");
        int due = getDueAppointments().size();
        if (due > 0) {
            Format.info(due + " appointment(s) are past their time. Mark them as completed or no-show.");
        }
        System.out.println();
    }

    @Override
    protected List<Option> getOptions() {
        boolean hasAppointments = !getMyAppointments().isEmpty();
        boolean hasDue = !getDueAppointments().isEmpty();
        boolean hasRecords = !getMyRecords(null).isEmpty();
        List<Option> options = new ArrayList<>();
        addIf(options, !getTodayAppointments().isEmpty(), "View today's appointments", this::viewToday);
        addIf(options, !getUpcomingAppointments().isEmpty(), "View upcoming appointments",
                () -> appointments.printTable(getUpcomingAppointments()));
        addIf(options, hasAppointments, "View all my appointments", () -> appointments.printTable(getMyAppointments()));
        addIf(options, hasDue, "Mark appointment as completed", this::completeAppointment);
        addIf(options, hasDue, "Mark appointment as no-show", this::markNoShow);
        addIf(options, hasAppointments, "View my patients", () -> PatientManagementMenu.printTable(getMyPatients()));
        addIf(options, hasAppointments, "View patient details and history", this::viewPatientHistory);
        addIf(options, !getAppointmentsWithoutRecord().isEmpty(), "Add medical record", this::addRecord);
        addIf(options, hasRecords, "View medical records", this::viewRecords);
        addIf(options, hasRecords, "Update medical record", this::updateRecord);
        addIf(options, hasRecords, "Delete medical record", this::deleteRecord);
        options.add(new Option("View my profile", () -> DoctorManagementMenu.printDetails(doctor)));
        options.add(new Option("Change password", () -> Prompts.changePassword(doctor)));
        return options;
    }

    @Override
    protected String getExitLabel() {
        return "Logout";
    }

    @Override
    protected boolean onExit() {
        return InputHelper.readYesNo("Log out?");
    }

    // ---------------------------------------------------------------- appointments

    private List<Appointment> getMyAppointments() {
        return appointments.getByDoctor(doctor.getId());
    }

    private List<Appointment> getTodayAppointments() {
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : getMyAppointments()) {
            if (a.getDate().equals(LocalDate.now())) {
                list.add(a);
            }
        }
        return list;
    }

    private List<Appointment> getUpcomingAppointments() {
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : getMyAppointments()) {
            if (a.isScheduled() && !a.getDate().isBefore(LocalDate.now())) {
                list.add(a);
            }
        }
        return list;
    }

    /** Scheduled appointments whose time has come; only these can be marked completed or no-show. */
    private List<Appointment> getDueAppointments() {
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : getMyAppointments()) {
            if (a.isScheduled() && !a.getDateTime().isAfter(LocalDateTime.now())) {
                list.add(a);
            }
        }
        return list;
    }

    private Appointment findOwnAppointment(String id) throws RecordNotFoundException {
        Appointment a = data.findAppointment(id);
        if (!a.getDoctorId().equals(doctor.getId())) {
            // Same message as a missing ID so other doctors' appointments are not revealed.
            throw new RecordNotFoundException("No appointment found with ID " + id + " in your schedule.");
        }
        return a;
    }

    /** Asks for one of the due appointments; a future or finished one is asked again with the reason. */
    private Appointment selectDueAppointment() {
        System.out.println("Appointments past their time:");
        appointments.printTable(getDueAppointments());
        return Prompts.select("Enter appointment ID: ", id -> {
            Appointment a = findOwnAppointment(id);
            if (!a.isScheduled()) {
                throw new InvalidInputException("Appointment " + a.getId() + " is already "
                        + a.getStatus().getLabel().toLowerCase() + ".");
            }
            if (a.getDateTime().isAfter(LocalDateTime.now())) {
                throw new InvalidInputException("Appointment " + a.getId() + " is on " + Format.date(a.getDate())
                        + " at " + Format.time(a.getTime()) + " and cannot be marked before its schedule.");
            }
            return a;
        });
    }

    private void viewToday() {
        System.out.println("Appointments today, " + Format.date(LocalDate.now()) + ":");
        appointments.printTable(getTodayAppointments());
    }

    private void completeAppointment() throws InvalidInputException, DuplicateRecordException {
        Appointment a = selectDueAppointment();
        appointments.printDetails(a);
        if (!InputHelper.confirm("Mark this appointment as completed?")) {
            Format.info("Nothing was changed.");
            return;
        }
        appointments.complete(a);
        Format.success("Appointment " + a.getId() + " marked as completed.");
        if (InputHelper.readYesNo("Add the medical record for this consultation now?")) {
            createRecord(a);
        }
    }

    private void markNoShow() throws InvalidInputException {
        Appointment a = selectDueAppointment();
        appointments.printDetails(a);
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
        for (Appointment a : getMyAppointments()) {
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
        Patient patient = Prompts.choosePatient(getMyPatients(), this::findOwnPatient);
        PatientForm.printProfile(patient);
        Format.subHeader("Appointments with you");
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : getMyAppointments()) {
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

    private List<Appointment> getAppointmentsWithoutRecord() {
        List<Appointment> pending = new ArrayList<>();
        for (Appointment a : getMyAppointments()) {
            if (a.getStatus() == AppointmentStatus.COMPLETED && findRecordForAppointment(a.getId()) == null) {
                pending.add(a);
            }
        }
        return pending;
    }

    private void addRecord() throws InvalidInputException, DuplicateRecordException {
        System.out.println("Completed appointments without a medical record:");
        appointments.printTable(getAppointmentsWithoutRecord());
        Appointment a = Prompts.select("Enter appointment ID: ", id -> {
            Appointment found = findOwnAppointment(id);
            if (found.getStatus() != AppointmentStatus.COMPLETED) {
                throw new InvalidInputException("Medical records can only be added to completed appointments.");
            }
            MedicalRecord existing = findRecordForAppointment(found.getId());
            if (existing != null) {
                throw new InvalidInputException("Appointment " + found.getId() + " already has medical record "
                        + existing.getId() + ". Use Update instead.");
            }
            return found;
        });
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
        String prescription = InputHelper.read("Prescription (e.g. Paracetamol 500mg, 1 tab every 6 hrs; "
                + "type \"none\" if none): ",
                s -> InputHelper.isNone(s) ? "" : Validator.validateText(s, "Prescription", 300, true));
        String notes = InputHelper.read("Notes / advice (type \"none\" if none): ",
                s -> InputHelper.isNone(s) ? "" : Validator.validateText(s, "Notes", 500, true));

        MedicalRecord record = new MedicalRecord(data.nextMedicalRecordId(), a.getId(), a.getPatientId(),
                doctor.getId(), a.getDate(), diagnosis, prescription.isEmpty() ? "None" : prescription, notes);
        data.getMedicalRecords().add(record);
        data.save();
        Format.success("Medical record " + record.getId() + " saved.");
    }

    private void viewRecords() {
        List<MedicalRecord> records = getMyRecords(null);
        printRecords(records);
        if (InputHelper.readYesNo("View full details of a record?")) {
            printRecord(Prompts.select("Enter record ID: ", this::findOwnRecord));
        }
    }

    private void updateRecord() {
        printRecords(getMyRecords(null));
        MedicalRecord r = Prompts.select("Enter record ID: ", this::findOwnRecord);
        printRecord(r);
        System.out.println("Type \"" + InputHelper.KEEP_WORD + "\" to keep the current value.");
        String diagnosis = InputHelper.readOptional("Diagnosis: ",
                s -> Validator.validateText(s, "Diagnosis", 200, true));
        String prescription = InputHelper.readOptional("Prescription (type \"none\" if none): ",
                s -> InputHelper.isNone(s) ? "None" : Validator.validateText(s, "Prescription", 300, true));
        String notes = InputHelper.readOptional("Notes (type \"none\" to clear): ",
                s -> InputHelper.isNone(s) ? "" : Validator.validateText(s, "Notes", 500, true));
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
        printRecords(getMyRecords(null));
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
