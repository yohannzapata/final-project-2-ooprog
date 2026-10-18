package models;

import util.RecordFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A patient's stay in a room. The room's daily rate is copied on admission so a later
 * rate change does not affect patients who are already admitted.
 */
public class Admission {

    private final String id;
    private final String patientId;
    private final String roomNumber;
    private final LocalDate admissionDate;
    private LocalDate dischargeDate;
    private final String reason;
    private final BigDecimal dailyRate;

    public Admission(String id, String patientId, String roomNumber, LocalDate admissionDate,
                     String reason, BigDecimal dailyRate) {
        this.id = id;
        this.patientId = patientId;
        this.roomNumber = roomNumber;
        this.admissionDate = admissionDate;
        this.reason = reason;
        this.dailyRate = dailyRate;
    }

    public boolean isDischarged() {
        return dischargeDate != null;
    }

    public String getStatusLabel() {
        return isDischarged() ? "Discharged" : "Admitted";
    }

    /**
     * Number of days charged. Hospitals charge per day, and a same-day discharge
     * still counts as one day.
     */
    public long getDaysStayed() {
        LocalDate end = isDischarged() ? dischargeDate : LocalDate.now();
        return Math.max(1, ChronoUnit.DAYS.between(admissionDate, end));
    }

    public void discharge(LocalDate date) {
        if (isDischarged()) {
            throw new IllegalStateException("Patient is already discharged.");
        }
        if (date.isBefore(admissionDate)) {
            throw new IllegalArgumentException("Discharge date cannot be before the admission date.");
        }
        this.dischargeDate = date;
    }

    public String toRecord() {
        return RecordFormat.join(id, patientId, roomNumber, admissionDate.toString(),
                dischargeDate == null ? "" : dischargeDate.toString(), reason, dailyRate.toPlainString());
    }

    public static Admission fromRecord(String[] f) {
        Admission a = new Admission(f[0], f[1], f[2], LocalDate.parse(f[3]), f[5], new BigDecimal(f[6]));
        if (!f[4].isEmpty()) {
            a.dischargeDate = LocalDate.parse(f[4]);
        }
        return a;
    }

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public LocalDate getDischargeDate() {
        return dischargeDate;
    }

    public String getReason() {
        return reason;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }
}
