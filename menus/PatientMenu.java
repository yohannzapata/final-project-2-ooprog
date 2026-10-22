package menus;

import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import exceptions.ScheduleConflictException;
import models.Admission;
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
        BigDecimal balance = BigDecimal.ZERO;
        for (Bill b : billing.getByPatient(patient.getId())) {
            if (!b.isPaid()) {
                balance = balance.add(b.getTotalAmount());
            }
        }
        System.out.println("Welcome, " + patient.getFullName() + " (" + patient.getId() + ")");
        List<Appointment> upcoming = getUpcoming();
        System.out.println("Upcoming appointments: " + upcoming.size() + " | Unpaid balance: " + Format.money(balance));
        if (!upcoming.isEmpty()) {
            // The list is sorted by date and time, so the first one is the next visit.
            Appointment next = upcoming.get(0);
            System.out.println("Next appointment: " + Format.date(next.getDate()) + " at " + Format.time(next.getTime())
                    + " with " + data.doctorName(next.getDoctorId()) + " (" + next.getId() + ")");
        }
        Admission active = admissions.getActiveAdmission(patient.getId());
        if (active != null) {
            Format.info("You are admitted in room " + active.getRoomNumber()
                    + ". Booking and rescheduling open again after discharge.");
        } else if (data.getDoctors().isEmpty()) {
            Format.info("No doctors are available for booking yet.");
        }
        System.out.println();
    }

    @Override
    protected List<Option> getOptions() {
        boolean isAdmitted = admissions.getActiveAdmission(patient.getId()) != null;
        boolean hasDoctors = !data.getDoctors().isEmpty();
        boolean hasUpcoming = !getUpcoming().isEmpty();
        List<Option> options = new ArrayList<>();
        options.add(new Option("View my profile", () -> PatientForm.printProfile(patient)));
        options.add(new Option("Update my profile", this::updateProfile));
        addIf(options, hasDoctors, "View available doctors", () -> DoctorManagementMenu.printTable(data.getDoctors()));
        addIf(options, hasDoctors && !isAdmitted, "Book an appointment", () -> AppointmentForm.book(patient));
        addIf(options, !getMyAppointments().isEmpty(), "View my appointments",
                () -> appointments.printTable(getMyAppointments(), false, true));
        addIf(options, hasUpcoming && !isAdmitted, "Reschedule an appointment", this::reschedule);
        addIf(options, hasUpcoming, "Cancel an appointment", this::cancel);
        addIf(options, !getMyRecords().isEmpty(), "View my medical records", this::viewRecords);
        addIf(options, !admissions.getByPatient(patient.getId()).isEmpty(), "View my admissions",
                () -> admissions.printTable(admissions.getByPatient(patient.getId())));
        addIf(options, !billing.getByPatient(patient.getId()).isEmpty(), "View my bills", this::viewBills);
        options.add(new Option("Change password", () -> Prompts.changePassword(patient)));
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

    private void updateProfile() {
        Format.info("To correct your name, birthdate or sex, please visit the admitting/records section.");
        PatientForm.update(patient, false);
        Format.success("Profile updated.");
    }

    private List<Appointment> getMyAppointments() {
        return appointments.getByPatient(patient.getId());
    }

    private List<Appointment> getUpcoming() {
        return appointments.getUpcoming(getMyAppointments());
    }

    /** Asks for one of the patient's upcoming appointments; anything else is asked again with the reason. */
    private Appointment selectUpcoming() {
        appointments.printTable(getUpcoming(), false, true);
        return Prompts.select("Enter appointment ID: ", id -> {
            Appointment a = data.findAppointment(id);
            if (!a.getPatientId().equals(patient.getId())) {
                throw new RecordNotFoundException("No appointment found with ID " + id + " in your records.");
            }
            if (!a.isUpcoming()) {
                throw new InvalidInputException("Appointment " + a.getId() + " is "
                        + (a.isScheduled() ? "already past its time" : a.getStatus().getLabel().toLowerCase())
                        + ". Only upcoming appointments can be changed.");
            }
            return a;
        });
    }

    private void reschedule() throws InvalidInputException, ScheduleConflictException, RecordNotFoundException {
        AppointmentForm.reschedule(selectUpcoming());
    }

    private void cancel() throws InvalidInputException {
        Appointment a = selectUpcoming();
        if (!InputHelper.confirm("Cancel your appointment with " + data.doctorName(a.getDoctorId()) + " on "
                + Format.date(a.getDate()) + " at " + Format.time(a.getTime()) + "?")) {
            Format.info("Your appointment was not cancelled.");
            return;
        }
        appointments.cancel(a);
        Format.success("Appointment " + a.getId() + " cancelled.");
    }

    private List<MedicalRecord> getMyRecords() {
        List<MedicalRecord> records = new ArrayList<>();
        for (MedicalRecord r : data.getMedicalRecords()) {
            if (r.getPatientId().equals(patient.getId())) {
                records.add(r);
            }
        }
        records.sort(Comparator.comparing(MedicalRecord::getConsultationDate).reversed());
        return records;
    }

    private void viewRecords() {
        DoctorMenu.printRecords(getMyRecords(), false, true);
        if (InputHelper.readYesNo("View full details of a record?")) {
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
        billing.printTable(billing.getByPatient(patient.getId()));
        if (InputHelper.readYesNo("View a bill statement?")) {
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
