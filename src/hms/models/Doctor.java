package hms.models;

import hms.menus.DoctorMenu;
import hms.util.Format;
import hms.util.RecordFormat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * A physician of the hospital. Patients book appointments within the doctor's
 * available days and clinic hours.
 */
public class Doctor extends User {

    private String specialization;
    private String licenseNumber;
    private String contactNumber;
    private BigDecimal consultationFee;
    private Set<DayOfWeek> availableDays;
    private LocalTime startTime;
    private LocalTime endTime;

    public Doctor(String id, String firstName, String lastName, String username, String password,
                  String specialization, String licenseNumber, String contactNumber, BigDecimal consultationFee,
                  Set<DayOfWeek> availableDays, LocalTime startTime, LocalTime endTime) {
        super(id, firstName, lastName, username, password);
        this.specialization = specialization;
        this.licenseNumber = licenseNumber;
        this.contactNumber = contactNumber;
        this.consultationFee = consultationFee;
        this.availableDays = EnumSet.copyOf(availableDays);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    private Doctor(String[] f) {
        super(f[0], f[1], f[2], f[3], f[4], f[5], Boolean.parseBoolean(f[6]));
        this.specialization = f[7];
        this.licenseNumber = f[8];
        this.contactNumber = f[9];
        this.consultationFee = new BigDecimal(f[10]);
        this.availableDays = EnumSet.noneOf(DayOfWeek.class);
        for (String day : f[11].split(",")) {
            availableDays.add(DayOfWeek.valueOf(day));
        }
        this.startTime = LocalTime.parse(f[12]);
        this.endTime = LocalTime.parse(f[13]);
    }

    @Override
    public void showMenu() {
        new DoctorMenu(this).run();
    }

    @Override
    public String getRole() {
        return "Doctor";
    }

    @Override
    public String getDisplayName() {
        return "Dr. " + getFullName();
    }

    public boolean worksOn(LocalDate date) {
        return availableDays.contains(date.getDayOfWeek());
    }

    public String getScheduleText() {
        return Format.days(availableDays) + " " + Format.time(startTime) + " - " + Format.time(endTime);
    }

    public String toRecord() {
        StringJoiner days = new StringJoiner(",");
        for (DayOfWeek day : availableDays) {
            days.add(day.name());
        }
        return RecordFormat.join(getId(), getFirstName(), getLastName(), getUsername(),
                getPasswordHash(), getSalt(), String.valueOf(isMustChangePassword()),
                specialization, licenseNumber, contactNumber, consultationFee.toPlainString(),
                days.toString(), startTime.toString(), endTime.toString());
    }

    public static Doctor fromRecord(String[] f) {
        return new Doctor(f);
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        if (consultationFee == null || consultationFee.signum() <= 0) {
            throw new IllegalArgumentException("Consultation fee must be greater than 0.");
        }
        this.consultationFee = consultationFee;
    }

    public Set<DayOfWeek> getAvailableDays() {
        return EnumSet.copyOf(availableDays);
    }

    public void setAvailableDays(Set<DayOfWeek> availableDays) {
        if (availableDays == null || availableDays.isEmpty()) {
            throw new IllegalArgumentException("A doctor must have at least one available day.");
        }
        this.availableDays = EnumSet.copyOf(availableDays);
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setClinicHours(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
