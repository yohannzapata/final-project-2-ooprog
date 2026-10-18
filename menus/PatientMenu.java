package menus;

import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import exceptions.ScheduleConflictException;
import models.Appointment;
import models.Bill;
import models.MedicalRecord;
import models.Patient;
import services.AdmissionService;
import services.AppointmentService;
import services.BillingService;
import util.Format;
import util.InputHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Patient menu: a patient can only see and change their own records.
 */
public class PatientMenu extends Menu {

    private final Patient patient;
    private final AppointmentService appointments = new AppointmentService();
    private final BillingService billing = new BillingService();
    private final AdmissionService admissions = new AdmissionService();

    public PatientMenu(Patient patient) {
        this.patient = patient;
    }

    @Override
    protected String getTitle() {
        return "PATIENT MENU";
    }

    @Override
    protected void printSummary() {
        int upcoming = 0;
        for (Appointment a : appointments.getByPatient(patient.getId())) {
            if (a.isUpcoming()) {
                upcoming++;
            }
        }
        BigDecimal balance = BigDecimal.ZERO;
        for (Bill b : billing.getByPatient(patient.getId())) {
            if (!b.isPaid()) {
                balance = balance.add(b.getTotalAmount());
            }
        }
        System.out.println("Welcome, " + patient.getFullName() + " (" + patient.getId() + ")");
        System.out.println("Upcoming appointments: " + upcoming + " | Unpaid balance: " + Format.money(balance));
        System.out.println();
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "View my profile",
                "Update my profile",
                "View available doctors",
                "Book an appointment",
                "View my appointments",
                "Reschedule an appointment",
                "Cancel an appointment",
                "View my medical records",
                "View my admissions",
                "View my bills",
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
    protected void handle(int choice) throws InvalidInputException, RecordNotFoundException,
            DuplicateRecordException, ScheduleConflictException {
        switch (choice) {
            case 1:
                PatientForm.printProfile(patient);
                break;
            case 2:
                Format.info("To correct your name, birthdate or sex, please visit the admitting/records section.");
                PatientForm.update(patient, false);
                Format.success("Profile updated.");
                break;
            case 3:
                DoctorManagementMenu.printTable(data.getDoctors());
                break;
            case 4:
                AppointmentForm.book(patient);
                break;
            case 5:
                appointments.printTable(appointments.getByPatient(patient.getId()));
                break;
            case 6:
                reschedule();
                break;
            case 7:
                cancel();
                break;
            case 8:
                viewRecords();
                break;
            case 9:
                admissions.printTable(admissions.getByPatient(patient.getId()));
                break;
            case 10:
                viewBills();
                break;
            case 11:
                Prompts.changePassword(patient);
                break;
            default:
                break;
        }
    }

    private Appointment findOwnAppointment(String id) throws RecordNotFoundException {
        Appointment a = data.findAppointment(id);
        if (!a.getPatientId().equals(patient.getId())) {
            throw new RecordNotFoundException("No appointment found with ID " + id + " in your records.");
        }
        return a;
    }

    private List<Appointment> getUpcoming() {
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : appointments.getByPatient(patient.getId())) {
            if (a.isUpcoming()) {
                list.add(a);
            }
        }
        return list;
    }

    private void reschedule() throws InvalidInputException, ScheduleConflictException, RecordNotFoundException {
        List<Appointment> upcoming = getUpcoming();
        if (upcoming.isEmpty()) {
            Format.info("You have no upcoming appointments to reschedule.");
            return;
        }
        appointments.printTable(upcoming);
        Appointment a = Prompts.select("Enter appointment ID: ", this::findOwnAppointment);
        AppointmentForm.reschedule(a);
    }

    private void cancel() throws InvalidInputException {
        List<Appointment> upcoming = getUpcoming();
        if (upcoming.isEmpty()) {
            Format.info("You have no upcoming appointments to cancel.");
            return;
        }
        appointments.printTable(upcoming);
        Appointment a = Prompts.select("Enter appointment ID: ", this::findOwnAppointment);
        if (!a.isScheduled()) {
            throw new InvalidInputException("Only scheduled appointments can be cancelled. This one is "
                    + a.getStatus().getLabel() + ".");
        }
        if (!InputHelper.confirm("Cancel your appointment with " + data.doctorName(a.getDoctorId()) + " on "
                + Format.date(a.getDate()) + " at " + Format.time(a.getTime()) + "?")) {
            Format.info("Your appointment was not cancelled.");
            return;
        }
        appointments.cancel(a);
        Format.success("Appointment " + a.getId() + " cancelled.");
    }

    private void viewRecords() {
        List<MedicalRecord> records = new ArrayList<>();
        for (MedicalRecord r : data.getMedicalRecords()) {
            if (r.getPatientId().equals(patient.getId())) {
                records.add(r);
            }
        }
        records.sort(Comparator.comparing(MedicalRecord::getConsultationDate).reversed());
        DoctorMenu.printRecords(records);
        if (!records.isEmpty() && InputHelper.readYesNo("View full details of a record?")) {
            MedicalRecord r = Prompts.select("Enter record ID: ", id -> {
                MedicalRecord found = data.findMedicalRecord(id);
                if (!found.getPatientId().equals(patient.getId())) {
                    throw new RecordNotFoundException("No medical record found with ID " + id + " in your records.");
                }
                return found;
            });
            DoctorMenu.printRecord(r);
        }
    }

    private void viewBills() {
        List<Bill> bills = billing.getByPatient(patient.getId());
        billing.printTable(bills);
        if (!bills.isEmpty() && InputHelper.readYesNo("View a bill statement?")) {
            Bill b = Prompts.select("Enter bill ID: ", id -> {
                Bill found = data.findBill(id);
                if (!found.getPatientId().equals(patient.getId())) {
                    throw new RecordNotFoundException("No bill found with ID " + id + " in your records.");
                }
                return found;
            });
            billing.printStatement(b);
        }
    }
}
