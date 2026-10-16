package hms.models;

import hms.util.RecordFormat;

import java.time.LocalDate;

/**
 * The doctor's notes for one completed appointment.
 */
public class MedicalRecord {

    private final String id;
    private final String appointmentId;
    private final String patientId;
    private final String doctorId;
    private final LocalDate consultationDate;
    private String diagnosis;
    private String prescription;
    private String notes;
    private LocalDate lastUpdated;

    public MedicalRecord(String id, String appointmentId, String patientId, String doctorId,
                         LocalDate consultationDate, String diagnosis, String prescription, String notes) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.consultationDate = consultationDate;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.notes = notes;
        this.lastUpdated = LocalDate.now();
    }

    public String toRecord() {
        return RecordFormat.join(id, appointmentId, patientId, doctorId, consultationDate.toString(),
                diagnosis, prescription, notes, lastUpdated.toString());
    }

    public static MedicalRecord fromRecord(String[] f) {
        MedicalRecord r = new MedicalRecord(f[0], f[1], f[2], f[3], LocalDate.parse(f[4]), f[5], f[6], f[7]);
        r.lastUpdated = LocalDate.parse(f[8]);
        return r;
    }

    public String getId() {
        return id;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public LocalDate getConsultationDate() {
        return consultationDate;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
        this.lastUpdated = LocalDate.now();
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
        this.lastUpdated = LocalDate.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.lastUpdated = LocalDate.now();
    }

    public LocalDate getLastUpdated() {
        return lastUpdated;
    }
}
