package menus;

import exceptions.InvalidInputException;
import models.Appointment;
import models.Doctor;
import util.Format;
import util.InputHelper;
import util.Validator;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Admin: add, view, search, update and delete doctors. Adding a doctor also creates their login.
 */
public class DoctorManagementMenu extends Menu {

    public static final BigDecimal MAX_FEE = new BigDecimal("50000");

    @Override
    protected String getTitle() {
        return "DOCTOR MANAGEMENT";
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "Add doctor",
                "View all doctors",
                "Search doctors",
                "View doctor details",
                "Update doctor",
                "Delete doctor"
        };
    }

    @Override
    protected void handle(int choice) throws InvalidInputException {
        switch (choice) {
            case 1:
                addDoctor();
                break;
            case 2:
                printTable(data.getDoctors());
                break;
            case 3:
                search();
                break;
            case 4:
                printDetails(Prompts.selectDoctor());
                break;
            case 5:
                updateDoctor();
                break;
            case 6:
                deleteDoctor();
                break;
            default:
                break;
        }
    }

    private void addDoctor() {
        String firstName = InputHelper.read("First name: ", s -> Validator.validateName(s, "First name"));
        String middleName = InputHelper.read("Middle name (type \"none\" if none): ",
                s -> InputHelper.isNone(s) ? "" : Validator.validateName(s, "Middle name"));
        String lastName = InputHelper.read("Last name: ", s -> Validator.validateName(s, "Last name"));
        String license = InputHelper.read("PRC license number (7 digits): ", s -> checkUniqueLicense(
                Validator.validateLicenseNumber(s), null));
        String specialization = InputHelper.read("Specialization (e.g. Pediatrics): ", Validator::validateSpecialization);
        String contact = InputHelper.read("Mobile number (09XXXXXXXXX): ", Validator::validateContactNumber);
        BigDecimal fee = InputHelper.readAmount("Consultation fee (" + Format.PESO + "): ", "Consultation fee", MAX_FEE);
        Set<DayOfWeek> days = InputHelper.read("Available days (e.g. MON,WED,FRI or MON-FRI): ", Validator::parseDays);
        LocalTime start = InputHelper.read("Clinic start time (HH:mm): ", s -> Validator.parseTime(s, "Start time"));
        LocalTime end = InputHelper.read("Clinic end time (HH:mm): ", s -> validateEndTime(start, Validator.parseTime(s, "End time")));

        System.out.println();
        System.out.println("Login account for the doctor");
        String username = Prompts.readNewUsername();
        Prompts.printPasswordRules();
        String password = Prompts.readNewPassword(username, "Temporary password");

        Doctor doctor = new Doctor(data.nextDoctorId(), firstName, lastName, username, password, specialization,
                license, contact, fee, days, start, end);
        doctor.setMiddleName(middleName);
        doctor.setMustChangePassword(true);
        data.getDoctors().add(doctor);
        data.save();
        Format.success(doctor.getDisplayName() + " added with ID " + doctor.getId() + ".");
        Format.info("The doctor must change the temporary password on first login.");
    }

    /** Clinic hours must allow at least one hour of consultations. */
    private static LocalTime validateEndTime(LocalTime start, LocalTime end) throws InvalidInputException {
        if (!end.isAfter(start)) {
            throw new InvalidInputException("End time must be later than the start time (" + start + ").");
        }
        if (end.isBefore(start.plusHours(1))) {
            throw new InvalidInputException("Clinic hours must be at least 1 hour long.");
        }
        if (start.getMinute() % Appointment.SLOT_MINUTES != 0 || end.getMinute() % Appointment.SLOT_MINUTES != 0) {
            throw new InvalidInputException("Clinic hours must start and end on the hour or half hour.");
        }
        return end;
    }

    private String checkUniqueLicense(String license, String excludeId) throws InvalidInputException {
        for (Doctor d : data.getDoctors()) {
            if (!d.getId().equals(excludeId) && d.getLicenseNumber().equals(license)) {
                throw new InvalidInputException("PRC license " + license + " is already registered to "
                        + d.getDisplayName() + ".");
            }
        }
        return license;
    }

    private void search() {
        String keyword = InputHelper.readRequired("Enter name, ID or specialization: ", "Search keyword")
                .toLowerCase(Locale.ROOT);
        List<Doctor> results = new ArrayList<>();
        for (Doctor d : data.getDoctors()) {
            if (d.getId().toLowerCase(Locale.ROOT).contains(keyword)
                    || d.getFullName().toLowerCase(Locale.ROOT).contains(keyword)
                    || d.getSpecialization().toLowerCase(Locale.ROOT).contains(keyword)) {
                results.add(d);
            }
        }
        printTable(results);
    }

    private void updateDoctor() throws InvalidInputException {
        Doctor doctor = Prompts.selectDoctor();
        System.out.println("Updating " + doctor.getDisplayName() + ". Type \"" + InputHelper.KEEP_WORD
                + "\" to keep the current value.");
        String first = InputHelper.readOptional("First name [" + doctor.getFirstName() + "]: ",
                s -> Validator.validateName(s, "First name"));
        String middle = InputHelper.readOptional("Middle name [" + Format.orDash(doctor.getMiddleName())
                + "] (type \"none\" to remove): ",
                s -> InputHelper.isNone(s) ? "" : Validator.validateName(s, "Middle name"));
        String last = InputHelper.readOptional("Last name [" + doctor.getLastName() + "]: ",
                s -> Validator.validateName(s, "Last name"));
        String spec = InputHelper.readOptional("Specialization [" + doctor.getSpecialization() + "]: ",
                Validator::validateSpecialization);
        String contact = InputHelper.readOptional("Mobile number [" + doctor.getContactNumber() + "]: ",
                Validator::validateContactNumber);
        BigDecimal fee = InputHelper.readOptional("Consultation fee [" + Format.money(doctor.getConsultationFee()) + "]: ",
                s -> Validator.validateAmount(s, "Consultation fee", MAX_FEE));
        Set<DayOfWeek> days = InputHelper.readOptional("Available days [" + Format.days(doctor.getAvailableDays())
                + "]: ", Validator::parseDays);
        LocalTime start = InputHelper.readOptional("Clinic start time [" + doctor.getStartTime() + "]: ",
                s -> Validator.parseTime(s, "Start time"));
        LocalTime newStart = start == null ? doctor.getStartTime() : start;
        LocalTime end = InputHelper.readOptional("Clinic end time [" + doctor.getEndTime() + "]: ",
                s -> validateEndTime(newStart, Validator.parseTime(s, "End time")));
        LocalTime newEnd = end == null ? doctor.getEndTime() : end;
        validateEndTime(newStart, newEnd);
        Set<DayOfWeek> newDays = days == null ? doctor.getAvailableDays() : days;

        // Do not allow a schedule change that would leave booked patients outside the new clinic hours.
        int affected = 0;
        for (Appointment a : data.getAppointments()) {
            if (a.isUpcoming() && a.getDoctorId().equals(doctor.getId())
                    && (!newDays.contains(a.getDate().getDayOfWeek()) || a.getTime().isBefore(newStart)
                    || a.getTime().plusMinutes(Appointment.SLOT_MINUTES).isAfter(newEnd))) {
                affected++;
            }
        }
        if (affected > 0) {
            throw new InvalidInputException("The new schedule conflicts with " + affected
                    + " upcoming appointment(s). Reschedule or cancel them first.");
        }

        if (first != null) {
            doctor.setFirstName(first);
        }
        if (middle != null) {
            doctor.setMiddleName(middle);
        }
        if (last != null) {
            doctor.setLastName(last);
        }
        if (spec != null) {
            doctor.setSpecialization(spec);
        }
        if (contact != null) {
            doctor.setContactNumber(contact);
        }
        if (fee != null) {
            doctor.setConsultationFee(fee);
        }
        doctor.setAvailableDays(newDays);
        doctor.setClinicHours(newStart, newEnd);
        data.save();
        Format.success("Doctor record updated.");
        if (fee != null) {
            Format.info("The new fee applies to new bookings only. Existing appointments keep their original fee.");
        }
    }

    private void deleteDoctor() throws InvalidInputException {
        Doctor doctor = Prompts.selectDoctor();
        // Past appointments that are still "Scheduled" also count: only this doctor can mark them
        // completed or no-show, so deleting the doctor would leave them stuck.
        int scheduled = 0;
        for (Appointment a : data.getAppointments()) {
            if (a.getDoctorId().equals(doctor.getId()) && a.isScheduled()) {
                scheduled++;
            }
        }
        if (scheduled > 0) {
            throw new InvalidInputException(doctor.getDisplayName() + " has " + scheduled
                    + " scheduled appointment(s) and cannot be deleted. Cancel them, or have the doctor mark "
                    + "past ones as completed or no-show first.");
        }
        if (!InputHelper.confirm("Delete " + doctor.getDisplayName() + " (" + doctor.getId() + ")?")) {
            Format.info("Delete cancelled.");
            return;
        }
        data.getDoctors().remove(doctor);
        data.save();
        Format.success("Doctor deleted. Past appointments and medical records are kept.");
    }

    public static void printTable(List<Doctor> doctors) {
        if (doctors.isEmpty()) {
            Format.info("No doctors found.");
            return;
        }
        String fmt = "%-6s %-26s %-20s %-14s %-30s%n";
        System.out.printf(fmt, "ID", "Name", "Specialization", "Fee", "Schedule");
        Format.line(98);
        for (Doctor d : doctors) {
            System.out.printf(fmt, d.getId(), Format.fit(d.getDisplayName(), 26), Format.fit(d.getSpecialization(), 20),
                    Format.money(d.getConsultationFee()), d.getScheduleText());
        }
    }

    public static void printDetails(Doctor d) {
        System.out.println("Doctor ID      : " + d.getId());
        System.out.println("Name           : Dr. " + d.getCompleteName());
        System.out.println("Specialization : " + d.getSpecialization());
        System.out.println("PRC License    : " + d.getLicenseNumber());
        System.out.println("Mobile number  : " + d.getContactNumber());
        System.out.println("Consultation   : " + Format.money(d.getConsultationFee()));
        System.out.println("Schedule       : " + d.getScheduleText());
        System.out.println("Username       : " + d.getUsername());
    }
}
