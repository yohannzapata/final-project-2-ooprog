package services;

import data.HospitalData;
import exceptions.InvalidInputException;
import exceptions.ScheduleConflictException;
import models.Admission;
import models.Appointment;
import models.AppointmentStatus;
import models.Doctor;
import models.Patient;
import util.Format;
import util.Table;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Booking rules shared by the patient and admin menus.
 */
public class AppointmentService {

    /** Appointments can be booked up to 90 days ahead. */
    public static final int MAX_DAYS_AHEAD = 90;

    private final HospitalData data = HospitalData.getInstance();

    /**
     * An admitted patient is already under the care of the hospital, so outpatient
     * consultations are only booked after discharge.
     */
    public void checkNotAdmitted(Patient patient) throws InvalidInputException {
        Admission active = new AdmissionService().getActiveAdmission(patient.getId());
        if (active != null) {
            throw new InvalidInputException(patient.getFullName() + " is currently admitted in room "
                    + active.getRoomNumber() + ". Appointments can be booked after discharge.");
        }
    }

    /** Checks the date only, so the user gets an error before being asked for the time. */
    public void validateDate(Doctor doctor, LocalDate date) throws InvalidInputException {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new InvalidInputException("Appointment date cannot be in the past.");
        }
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new InvalidInputException("Appointments can only be booked up to " + MAX_DAYS_AHEAD
                    + " days in advance (until " + Format.date(today.plusDays(MAX_DAYS_AHEAD)) + ").");
        }
        if (!doctor.worksOn(date)) {
            throw new InvalidInputException(doctor.getDisplayName() + " has no clinic on "
                    + Format.day(date.getDayOfWeek()) + ". Available: " + Format.days(doctor.getAvailableDays()) + ".");
        }
    }

    /**
     * Full check of a slot. excludeId is the appointment being rescheduled (so it does not
     * conflict with itself), or null for a new booking.
     */
    public void validateSlot(Doctor doctor, Patient patient, LocalDate date, LocalTime time, String excludeId)
            throws InvalidInputException, ScheduleConflictException {
        validateDate(doctor, date);
        if (time.getMinute() % Appointment.SLOT_MINUTES != 0) {
            throw new InvalidInputException("Appointments start on the hour or half hour (e.g. 09:00 or 09:30).");
        }
        LocalTime lastSlot = doctor.getEndTime().minusMinutes(Appointment.SLOT_MINUTES);
        if (time.isBefore(doctor.getStartTime()) || time.isAfter(lastSlot)) {
            throw new InvalidInputException("Time must be within clinic hours: " + Format.time(doctor.getStartTime())
                    + " to " + Format.time(lastSlot) + " (last slot).");
        }
        if (!LocalDateTime.of(date, time).isAfter(LocalDateTime.now())) {
            throw new InvalidInputException("That time has already passed today. Please choose a later time.");
        }
        for (Appointment a : data.getAppointments()) {
            if (!a.isScheduled() || a.getId().equals(excludeId) || !a.getDate().equals(date)) {
                continue;
            }
            if (a.getDoctorId().equals(doctor.getId()) && a.getTime().equals(time)) {
                throw new ScheduleConflictException(doctor.getDisplayName() + " is already booked on "
                        + Format.date(date) + " at " + Format.time(time) + ".");
            }
            if (a.getPatientId().equals(patient.getId()) && a.getTime().equals(time)) {
                throw new ScheduleConflictException(patient.getFullName() + " already has an appointment on "
                        + Format.date(date) + " at " + Format.time(time) + " (" + a.getId() + ").");
            }
            if (a.getPatientId().equals(patient.getId()) && a.getDoctorId().equals(doctor.getId())) {
                throw new ScheduleConflictException(patient.getFullName() + " already has an appointment with "
                        + doctor.getDisplayName() + " on " + Format.date(date) + " (" + a.getId() + ").");
            }
        }
    }

    /** Free 30-minute slots of a doctor on a date, shown to the user before choosing a time. */
    public List<LocalTime> getAvailableSlots(Doctor doctor, LocalDate date) {
        List<LocalTime> slots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalTime lastSlot = doctor.getEndTime().minusMinutes(Appointment.SLOT_MINUTES);
        int slotCount = (int) (Duration.between(doctor.getStartTime(), lastSlot).toMinutes()
                / Appointment.SLOT_MINUTES) + 1;
        for (int i = 0; i < slotCount; i++) {
            LocalTime time = doctor.getStartTime().plusMinutes((long) i * Appointment.SLOT_MINUTES);
            if (LocalDateTime.of(date, time).isAfter(now) && !isDoctorBooked(doctor, date, time)) {
                slots.add(time);
            }
        }
        return slots;
    }

    /** The first dates, from today up to MAX_DAYS_AHEAD, on which the doctor still has a free time. */
    public List<LocalDate> getOpenDates(Doctor doctor, int limit) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i <= MAX_DAYS_AHEAD && dates.size() < limit; i++) {
            LocalDate date = today.plusDays(i);
            if (doctor.worksOn(date) && !getAvailableSlots(doctor, date).isEmpty()) {
                dates.add(date);
            }
        }
        return dates;
    }

    private boolean isDoctorBooked(Doctor doctor, LocalDate date, LocalTime time) {
        for (Appointment a : data.getAppointments()) {
            if (a.isScheduled() && a.getDoctorId().equals(doctor.getId()) && a.getDate().equals(date)
                    && a.getTime().equals(time)) {
                return true;
            }
        }
        return false;
    }

    public Appointment book(Patient patient, Doctor doctor, LocalDate date, LocalTime time, String reason)
            throws InvalidInputException, ScheduleConflictException {
        checkNotAdmitted(patient);
        validateSlot(doctor, patient, date, time, null);
        Appointment appointment = new Appointment(data.nextAppointmentId(), patient.getId(), doctor.getId(),
                date, time, reason, doctor.getConsultationFee());
        data.getAppointments().add(appointment);
        data.save();
        return appointment;
    }

    public void reschedule(Appointment appointment, Doctor doctor, Patient patient, LocalDate date, LocalTime time)
            throws InvalidInputException, ScheduleConflictException {
        checkScheduled(appointment, "rescheduled");
        if (appointment.getDate().equals(date) && appointment.getTime().equals(time)) {
            throw new InvalidInputException("The new schedule is the same as the current one.");
        }
        validateSlot(doctor, patient, date, time, appointment.getId());
        appointment.reschedule(date, time);
        data.save();
    }

    public void cancel(Appointment appointment) throws InvalidInputException {
        checkScheduled(appointment, "cancelled");
        if (!appointment.getDateTime().isAfter(LocalDateTime.now())) {
            throw new InvalidInputException("The time of appointment " + appointment.getId()
                    + " has already passed. The doctor should mark it as completed or no-show instead.");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        data.save();
    }

    /** A doctor can only complete an appointment once its scheduled time has come. */
    public void complete(Appointment appointment) throws InvalidInputException {
        checkScheduled(appointment, "marked as completed");
        if (appointment.getDateTime().isAfter(LocalDateTime.now())) {
            throw new InvalidInputException("This appointment is on " + Format.date(appointment.getDate()) + " at "
                    + Format.time(appointment.getTime()) + ". It cannot be completed before its schedule.");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        data.save();
    }

    public void markNoShow(Appointment appointment) throws InvalidInputException {
        checkScheduled(appointment, "marked as no-show");
        if (appointment.getDateTime().isAfter(LocalDateTime.now())) {
            throw new InvalidInputException("A patient can only be marked as no-show after the appointment time.");
        }
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        data.save();
    }

    private void checkScheduled(Appointment appointment, String action) throws InvalidInputException {
        if (!appointment.isScheduled()) {
            throw new InvalidInputException("Appointment " + appointment.getId() + " is already "
                    + appointment.getStatus().getLabel().toLowerCase() + " and cannot be " + action + ".");
        }
    }

    public List<Appointment> getByPatient(String patientId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : data.getAppointments()) {
            if (a.getPatientId().equals(patientId)) {
                result.add(a);
            }
        }
        result.sort(Comparator.comparing(Appointment::getDateTime));
        return result;
    }

    public List<Appointment> getByDoctor(String doctorId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : data.getAppointments()) {
            if (a.getDoctorId().equals(doctorId)) {
                result.add(a);
            }
        }
        result.sort(Comparator.comparing(Appointment::getDateTime));
        return result;
    }

    /** Scheduled appointments whose time has not come yet; these can still be rescheduled or cancelled. */
    public List<Appointment> getUpcoming(List<Appointment> appointments) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.isUpcoming()) {
                result.add(a);
            }
        }
        return result;
    }

    public List<Appointment> getAllSorted() {
        List<Appointment> result = new ArrayList<>(data.getAppointments());
        result.sort(Comparator.comparing(Appointment::getDateTime));
        return result;
    }

    public void printTable(List<Appointment> list) {
        printTable(list, true, true);
    }

    /** The patient or doctor column is left out when every row is that same person's appointment. */
    public void printTable(List<Appointment> list, boolean showPatient, boolean showDoctor) {
        if (list.isEmpty()) {
            Format.info("No appointments found.");
            return;
        }
        List<String> headers = new ArrayList<>(List.of("ID", "Date", "Time"));
        if (showPatient) {
            headers.add("Patient");
        }
        if (showDoctor) {
            headers.add("Doctor");
        }
        headers.add("Status");
        Table table = new Table(headers.toArray(new String[0]));
        for (Appointment a : list) {
            table.addRow(getRow(a, showPatient, showDoctor));
        }
        table.print();
    }

    private Object[] getRow(Appointment a, boolean showPatient, boolean showDoctor) {
        List<Object> row = new ArrayList<>(List.of(a.getId(), Format.date(a.getDate()), Format.time(a.getTime())));
        if (showPatient) {
            row.add(data.patientName(a.getPatientId()));
        }
        if (showDoctor) {
            row.add(data.doctorName(a.getDoctorId()));
        }
        row.add(a.getStatus().getLabel());
        return row.toArray();
    }

    public void printDetails(Appointment a) {
        Table.details("APPOINTMENT " + a.getId())
                .addRow("Patient", data.patientName(a.getPatientId()) + " (" + a.getPatientId() + ")")
                .addRow("Doctor", data.doctorName(a.getDoctorId()) + " (" + a.getDoctorId() + ")")
                .addRow("Schedule", Format.date(a.getDate()) + ", " + Format.time(a.getTime()))
                .addRow("Reason", a.getReason())
                .addRow("Fee", Format.money(a.getConsultationFee()))
                .addRow("Status", a.getStatus().getLabel())
                .print();
    }
}
