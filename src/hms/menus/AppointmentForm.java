package hms.menus;

import hms.data.HospitalData;
import hms.exceptions.InvalidInputException;
import hms.exceptions.RecordNotFoundException;
import hms.exceptions.ScheduleConflictException;
import hms.models.Appointment;
import hms.models.Doctor;
import hms.models.Patient;
import hms.services.AppointmentService;
import hms.util.Format;
import hms.util.InputHelper;
import hms.util.Validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Booking and rescheduling steps shared by the patient and admin menus.
 */
public final class AppointmentForm {

    private static final AppointmentService SERVICE = new AppointmentService();

    private AppointmentForm() {
    }

    public static void book(Patient patient) throws InvalidInputException, ScheduleConflictException {
        HospitalData data = HospitalData.getInstance();
        if (data.getDoctors().isEmpty()) {
            Format.info("There are no doctors in the system yet.");
            return;
        }
        DoctorManagementMenu.printTable(data.getDoctors());
        Doctor doctor = Prompts.selectDoctor();
        System.out.println("Schedule of " + doctor.getDisplayName() + ": " + doctor.getScheduleText());
        System.out.println("Consultation fee: " + Format.money(doctor.getConsultationFee()));

        LocalDate date = readDate(doctor);
        LocalTime time = readTime(doctor, patient, date, null);
        String reason = InputHelper.readText("Reason for visit (e.g. check-up, fever): ", "Reason", 100, true);

        System.out.println();
        System.out.println("Please review the appointment:");
        System.out.println("  Patient : " + patient.getFullName());
        System.out.println("  Doctor  : " + doctor.getDisplayName() + " (" + doctor.getSpecialization() + ")");
        System.out.println("  When    : " + Format.date(date) + " at " + Format.time(time));
        System.out.println("  Fee     : " + Format.money(doctor.getConsultationFee()));
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
        System.out.println("Current schedule: " + Format.date(appointment.getDate()) + " at "
                + Format.time(appointment.getTime()));
        System.out.println("Schedule of " + doctor.getDisplayName() + ": " + doctor.getScheduleText());

        LocalDate date = readDate(doctor);
        LocalTime time = readTime(doctor, patient, date, appointment.getId());
        SERVICE.reschedule(appointment, doctor, patient, date, time);
        Format.success("Appointment " + appointment.getId() + " moved to " + Format.date(date) + " at "
                + Format.time(time) + ".");
    }

    private static LocalDate readDate(Doctor doctor) {
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

    private static LocalTime readTime(Doctor doctor, Patient patient, LocalDate date, String excludeId) {
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
            try {
                SERVICE.validateSlot(doctor, patient, date, time, excludeId);
            } catch (ScheduleConflictException e) {
                throw new InvalidInputException(e.getMessage());
            }
            return time;
        });
    }
}
