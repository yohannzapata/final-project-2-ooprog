package menus;

import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import exceptions.ScheduleConflictException;
import models.Appointment;
import models.Patient;
import services.AdmissionService;
import services.AppointmentService;
import util.Format;
import util.InputHelper;
import util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin: view all appointments and book, reschedule or cancel them for patients.
 */
public class AppointmentManagementMenu extends Menu {

    private final AppointmentService service = new AppointmentService();
    private final AdmissionService admissions = new AdmissionService();

    @Override
    protected String getTitle() {
        return "ADMIN MENU > APPOINTMENT MANAGEMENT";
    }

    @Override
    protected void printSummary() {
        System.out.println("Appointments: " + data.getAppointments().size() + " | Upcoming: "
                + service.getUpcoming(data.getAppointments()).size());
        System.out.println();
    }

    @Override
    protected List<Option> getOptions() {
        boolean hasAppointments = !data.getAppointments().isEmpty();
        boolean hasUpcoming = !service.getUpcoming(data.getAppointments()).isEmpty();
        boolean canBook = !admissions.getAdmittablePatients().isEmpty() && !data.getDoctors().isEmpty();
        List<Option> options = new ArrayList<>();
        addIf(options, hasAppointments, "View all appointments", () -> service.printTable(service.getAllSorted()));
        addIf(options, hasAppointments, "View appointments on a date", this::viewByDate);
        addIf(options, hasAppointments, "View appointment details", this::viewDetails);
        addIf(options, canBook, "Book appointment for a patient", this::book);
        addIf(options, hasUpcoming, "Reschedule appointment", this::reschedule);
        addIf(options, hasUpcoming, "Cancel appointment", this::cancel);
        return options;
    }

    private void viewByDate() {
        LocalDate target = InputHelper.read("Date (yyyy-MM-dd, or \"today\"): ",
                s -> Validator.parseDateOrToday(s, "Date"));
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : service.getAllSorted()) {
            if (a.getDate().equals(target)) {
                result.add(a);
            }
        }
        System.out.println("Appointments on " + Format.date(target) + ":");
        service.printTable(result);
    }

    private void viewDetails() {
        service.printTable(service.getAllSorted());
        service.printDetails(Prompts.select("Enter appointment ID: ", data::findAppointment));
    }

    private void book() throws InvalidInputException, ScheduleConflictException {
        // Admitted patients cannot book, so only the others are listed; an admitted patient's ID is
        // asked again here, before the rest of the booking is filled in.
        Patient patient = Prompts.choosePatient(admissions.getAdmittablePatients(), id -> {
            Patient found = data.findPatient(id);
            service.checkNotAdmitted(found);
            return found;
        });
        AppointmentForm.book(patient);
    }

    /** Only upcoming appointments can be rescheduled or cancelled; anything else is asked again. */
    private Appointment selectUpcoming() {
        service.printTable(service.getUpcoming(service.getAllSorted()));
        return Prompts.select("Enter appointment ID: ", id -> {
            Appointment found = data.findAppointment(id);
            if (!found.isUpcoming()) {
                throw new InvalidInputException("Appointment " + found.getId() + " is "
                        + (found.isScheduled() ? "already past its time" : found.getStatus().getLabel().toLowerCase())
                        + ". Only upcoming appointments can be changed.");
            }
            return found;
        });
    }

    private void reschedule() throws InvalidInputException, ScheduleConflictException, RecordNotFoundException {
        AppointmentForm.reschedule(selectUpcoming());
    }

    private void cancel() throws InvalidInputException {
        Appointment appointment = selectUpcoming();
        service.printDetails(appointment);
        if (!InputHelper.confirm("Cancel this appointment?")) {
            Format.info("The appointment was not cancelled.");
            return;
        }
        service.cancel(appointment);
        Format.success("Appointment " + appointment.getId() + " cancelled.");
    }
}
