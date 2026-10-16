package hms.models;

import hms.util.RecordFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A consultation slot of a patient with a doctor. Each slot is 30 minutes long.
 * The consultation fee is copied from the doctor at booking time so later fee changes
 * do not affect appointments that were already booked.
 */
public class Appointment {

    public static final int SLOT_MINUTES = 30;

    private final String id;
    private final String patientId;
    private final String doctorId;
    private LocalDate date;
    private LocalTime time;
    private String reason;
    private AppointmentStatus status;
    private final BigDecimal consultationFee;

    public Appointment(String id, String patientId, String doctorId, LocalDate date, LocalTime time,
                       String reason, BigDecimal consultationFee) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.date = date;
        this.time = time;
        this.reason = reason;
        this.consultationFee = consultationFee;
        this.status = AppointmentStatus.SCHEDULED;
    }

    public LocalDateTime getDateTime() {
        return LocalDateTime.of(date, time);
    }

    public boolean isScheduled() {
        return status == AppointmentStatus.SCHEDULED;
    }

    /** True if this appointment is still scheduled and has not started yet. */
    public boolean isUpcoming() {
        return isScheduled() && getDateTime().isAfter(LocalDateTime.now());
    }

    public void reschedule(LocalDate newDate, LocalTime newTime) {
        if (!isScheduled()) {
            throw new IllegalStateException("Only scheduled appointments can be rescheduled.");
        }
        this.date = newDate;
        this.time = newTime;
    }

    public String toRecord() {
        return RecordFormat.join(id, patientId, doctorId, date.toString(), time.toString(), reason,
                status.name(), consultationFee.toPlainString());
    }

    public static Appointment fromRecord(String[] f) {
        Appointment a = new Appointment(f[0], f[1], f[2], LocalDate.parse(f[3]), LocalTime.parse(f[4]),
                f[5], new BigDecimal(f[7]));
        a.status = AppointmentStatus.valueOf(f[6]);
        return a;
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getReason() {
        return reason;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }
}
