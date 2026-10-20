package menus;

import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import exceptions.ScheduleConflictException;
import models.Appointment;
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

    @Override
    protected String getTitle() {
        return "APPOINTMENT MANAGEMENT";
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "View all appointments",
                "View appointments on a date",
                "View appointment details",
                "Book appointment for a patient",
                "Reschedule appointment",
                "Cancel appointment"
        };
    }

    @Override
    protected void handle(int choice) throws InvalidInputException, RecordNotFoundException, ScheduleConflictException {
        switch (choice) {
            case 1:
                service.printTable(service.getAllSorted());
                break;
            case 2:
                viewByDate();
                break;
            case 3:
                service.printDetails(Prompts.select("Enter appointment ID: ", data::findAppointment));
                break;
            case 4:
                AppointmentForm.book(Prompts.selectPatient());
                break;
            case 5:
                AppointmentForm.reschedule(Prompts.select("Enter appointment ID: ", data::findAppointment));
                break;
            case 6:
                cancel();
                break;
            default:
                break;
        }
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

    private void cancel() throws InvalidInputException {
        Appointment appointment = Prompts.select("Enter appointment ID: ", data::findAppointment);
        service.printDetails(appointment);
        if (!appointment.isScheduled()) {
            throw new InvalidInputException("Only scheduled appointments can be cancelled.");
        }
        if (!appointment.isUpcoming()) {
            throw new InvalidInputException("This appointment's time has already passed and can no longer be "
                    + "cancelled. The doctor should mark it as completed or no-show.");
        }
        if (!InputHelper.confirm("Cancel this appointment?")) {
            Format.info("The appointment was not cancelled.");
            return;
        }
        service.cancel(appointment);
        Format.success("Appointment " + appointment.getId() + " cancelled.");
    }
}
