package menus;

import data.HospitalData;
import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import exceptions.ScheduleConflictException;
import models.Appointment;
import models.Doctor;
import models.Patient;
import services.AppointmentService;
import util.Format;
import util.InputHelper;
import util.Table;
import util.Validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Booking and rescheduling steps shared by the patient and admin menus.
 */
public final class AppointmentForm {

    private static final AppointmentService SERVICE = new AppointmentService();
    /** How many open dates are listed before the date is asked. */
    public static final int OPEN_DATES_SHOWN = 5;

    private AppointmentForm() {
    }

    public static void book(Patient patient) throws InvalidInputException, ScheduleConflictException {
        HospitalData data = HospitalData.getInstance();
        SERVICE.checkNotAdmitted(patient);
        Format.cancelHint();
        if (data.getDoctors().isEmpty()) {
            Format.info("There are no doctors in the system yet.");
            return;
        }
        // A doctor with no free time in the booking window is asked again, instead of a date that can never be found.
        Doctor doctor = Prompts.chooseDoctor(data.getDoctors(), id -> {
            Doctor found = data.findDoctor(id);
            if (SERVICE.getOpenDates(found, 1).isEmpty()) {
                throw new InvalidInputException(found.getDisplayName() + " has no open times in the next "
                        + AppointmentService.MAX_DAYS_AHEAD + " days. Choose another doctor.");
            }
            return found;
        });
        System.out.println("Schedule of " + doctor.getDisplayName() + ": " + doctor.getScheduleText());
        System.out.println("Consultation fee: " + Format.money(doctor.getConsultationFee()));

        LocalDate date = readDate(doctor);
        LocalTime time = readTime(doctor, patient, date, null);
        String reason = InputHelper.readText("Reason for visit (e.g. check-up, fever): ", "Reason", 100, true);

        System.out.println();
        Table.details("PLEASE REVIEW THE APPOINTMENT")
                .addRow("Patient", patient.getFullName())
                .addRow("Doctor", doctor.getDisplayName() + " (" + doctor.getSpecialization() + ")")
                .addRow("When", Format.date(date) + " at " + Format.time(time))
                .addRow("Reason", reason)
                .addRow("Fee", Format.money(doctor.getConsultationFee()))
                .print();
        if (!InputHelper.readYesNo("Confirm booking?")) {
            Format.info("Booking cancelled.");
            return;
        }
        Appointment appointment = SERVICE.book(patient, doctor, date, time, reason);
        Format.success("Appointment booked. Your appointment ID is " + appointment.getId() + ".");
    }

    public static void reschedule(Appointment appointment) throws InvalidInputException, ScheduleConflictException,
            RecordNotFoundException {
        HospitalData data = HospitalData.getInstance();
        if (!appointment.isScheduled()) {
            throw new InvalidInputException("Only scheduled appointments can be rescheduled. This one is "
                    + appointment.getStatus().getLabel() + ".");
        }
        Doctor doctor = data.findDoctor(appointment.getDoctorId());
        Patient patient = data.findPatient(appointment.getPatientId());
        SERVICE.checkNotAdmitted(patient);
        System.out.println("Current schedule: " + Format.date(appointment.getDate()) + " at "
                + Format.time(appointment.getTime()));
        System.out.println("Schedule of " + doctor.getDisplayName() + ": " + doctor.getScheduleText());

        LocalDate date = readDate(doctor);
        LocalTime time = readTime(doctor, patient, date, appointment);
        SERVICE.reschedule(appointment, doctor, patient, date, time);
        Format.success("Appointment " + appointment.getId() + " moved to " + Format.date(date) + " at "
                + Format.time(time) + ".");
    }

    /** Lists the next dates with free times, so the user does not have to guess a date. */
    private static void printOpenDates(Doctor doctor, List<LocalDate> dates) {
        System.out.println("Next open dates for " + doctor.getDisplayName() + ":");
        Table table = new Table("Date", "Day", "Open times").alignRight(2);
        for (LocalDate date : dates) {
            table.addRow(date.toString(), Format.day(date.getDayOfWeek()), SERVICE.getAvailableSlots(doctor, date).size());
        }
        table.print();
    }

    private static LocalDate readDate(Doctor doctor) throws InvalidInputException {
        List<LocalDate> openDates = SERVICE.getOpenDates(doctor, OPEN_DATES_SHOWN);
        if (openDates.isEmpty()) {
            throw new InvalidInputException(doctor.getDisplayName() + " has no open times in the next "
                    + AppointmentService.MAX_DAYS_AHEAD + " days.");
        }
        printOpenDates(doctor, openDates);
        return InputHelper.read("Appointment date (yyyy-MM-dd): ", s -> {
            LocalDate date = Validator.parseDate(s, "Appointment date");
            SERVICE.validateDate(doctor, date);
            if (SERVICE.getAvailableSlots(doctor, date).isEmpty()) {
                throw new InvalidInputException(doctor.getDisplayName() + " is fully booked on " + Format.date(date)
                        + ". Please choose another date.");
            }
            return date;
        });
    }

    /** current is the appointment being rescheduled, or null for a new booking. */
    private static LocalTime readTime(Doctor doctor, Patient patient, LocalDate date, Appointment current) {
        String excludeId = current == null ? null : current.getId();
        List<LocalTime> slots = SERVICE.getAvailableSlots(doctor, date);
        System.out.println("Available times on " + Format.date(date) + ":");
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            line.append(String.format("%-7s", slots.get(i)));
            if ((i + 1) % 8 == 0) {
                System.out.println("  " + line);
                line.setLength(0);
            }
        }
        if (line.length() > 0) {
            System.out.println("  " + line);
        }
        return InputHelper.read("Appointment time (HH:mm): ", s -> {
            LocalTime time = Validator.parseTime(s, "Appointment time");
            if (current != null && current.getDate().equals(date) && current.getTime().equals(time)) {
                throw new InvalidInputException("That is the current schedule. Choose a different time.");
            }
            try {
                SERVICE.validateSlot(doctor, patient, date, time, excludeId);
            } catch (ScheduleConflictException e) {
                throw new InvalidInputException(e.getMessage());
            }
            return time;
        });
    }
}
