package menus;

import exceptions.InvalidInputException;
import exceptions.OperationCancelledException;
import models.Appointment;
import models.Doctor;
import util.Format;
import util.InputHelper;
import util.Table;
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
        return "ADMIN MENU > DOCTOR MANAGEMENT";
    }

    @Override
    protected void printSummary() {
        System.out.println("Doctors on file: " + data.getDoctors().size());
        if (data.getDoctors().isEmpty()) {
            Format.info("Add a doctor to see the other options.");
        }
        System.out.println();
    }

    @Override
    protected List<Option> getOptions() {
        boolean hasDoctors = !data.getDoctors().isEmpty();
        List<Option> options = new ArrayList<>();
        options.add(new Option("Add doctor", this::addDoctor));
        addIf(options, hasDoctors, "View all doctors", () -> printTable(data.getDoctors()));
        addIf(options, hasDoctors, "Search doctors", this::search);
        addIf(options, hasDoctors, "View doctor details", this::viewDetails);
        addIf(options, hasDoctors, "Update doctor", this::updateDoctor);
        addIf(options, hasDoctors, "Reset doctor password", () -> Prompts.resetPassword(Prompts.chooseDoctor()));
        addIf(options, !getDeletableDoctors().isEmpty(), "Delete doctor", this::deleteDoctor);
        return options;
    }

    /** Details typed for a new doctor, kept together so they can be reviewed before saving. */
    private static final class Details {
        private String firstName;
        private String middleName;
        private String lastName;
        private String license;
        private String specialization;
        private String contact;
        private BigDecimal fee;
        private Set<DayOfWeek> days;
        private LocalTime start;
        private LocalTime end;
    }

    private void addDoctor() {
        // A typo found during the review is fixed by filling in the details again, before anything is saved.
        Details d = null;
        boolean isConfirmed = false;
        while (!isConfirmed) {
            d = readDetails();
            printReview(d);
            isConfirmed = InputHelper.readYesNo("Are these details correct?");
            if (!isConfirmed && !InputHelper.readYesNo("Fill in the details again?")) {
                throw new OperationCancelledException();
            }
        }

        System.out.println();
        System.out.println("Login account for the doctor");
        String username = Prompts.readNewUsername();
        Prompts.printPasswordRules();
        String password = Prompts.readNewPassword(username, "Temporary password");

        Doctor doctor = new Doctor(data.nextDoctorId(), d.firstName, d.lastName, username, password, d.specialization,
                d.license, d.contact, d.fee, d.days, d.start, d.end);
        doctor.setMiddleName(d.middleName);
        doctor.setMustChangePassword(true);
        data.getDoctors().add(doctor);
        data.save();
        Format.success(doctor.getDisplayName() + " added with ID " + doctor.getId() + ".");
        Format.info("The doctor must change the temporary password on first login.");
    }

    private Details readDetails() {
        Details d = new Details();
        d.firstName = InputHelper.read("First name: ", s -> Validator.validateName(s, "First name"));
        d.middleName = InputHelper.read("Middle name (type \"none\" if none): ",
                s -> InputHelper.isNone(s) ? "" : Validator.validateName(s, "Middle name"));
        d.lastName = InputHelper.read("Last name: ", s -> Validator.validateName(s, "Last name"));
        d.license = InputHelper.read("PRC license number (7 digits): ", s -> checkUniqueLicense(
                Validator.validateLicenseNumber(s), null));
        d.specialization = InputHelper.read("Specialization (e.g. Pediatrics): ", Validator::validateSpecialization);
        d.contact = InputHelper.read("Mobile number (09XXXXXXXXX): ", Validator::validateContactNumber);
        d.fee = InputHelper.readAmount("Consultation fee (" + Format.PESO + "): ", "Consultation fee", MAX_FEE);
        d.days = InputHelper.read("Available days (e.g. MON,WED,FRI or MON-FRI): ", Validator::parseDays);
        LocalTime start = InputHelper.read("Clinic start time (HH:mm): ", s -> validateStartTime(Validator.parseTime(s, "Start time")));
        d.start = start;
        d.end = InputHelper.read("Clinic end time (HH:mm): ", s -> validateEndTime(start, Validator.parseTime(s, "End time")));
        return d;
    }

    private static void printReview(Details d) {
        String name = d.middleName.isEmpty() ? d.firstName + " " + d.lastName
                : d.firstName + " " + d.middleName + " " + d.lastName;
        System.out.println();
        Table.details("PLEASE REVIEW THE NEW DOCTOR")
                .addRow("Name", "Dr. " + name)
                .addRow("PRC license", d.license)
                .addRow("Specialization", d.specialization)
                .addRow("Mobile number", d.contact)
                .addRow("Consultation", Format.money(d.fee))
                .addRow("Schedule", Format.days(d.days) + " " + Format.time(d.start) + " - " + Format.time(d.end))
                .print();
    }

    /** Checked as soon as the start time is typed, so the user does not find out only after the end time. */
    private static LocalTime validateStartTime(LocalTime start) throws InvalidInputException {
        if (start.getMinute() % Appointment.SLOT_MINUTES != 0) {
            throw new InvalidInputException("Clinic hours must start on the hour or half hour (e.g. 08:00 or 08:30).");
        }
        if (start.isAfter(LocalTime.of(22, 30))) {
            throw new InvalidInputException("Clinic hours must start by 22:30 so there is at least 1 hour before midnight.");
        }
        return start;
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

    /** Searches until something is found or the user stops. */
    private void search() {
        boolean isSearching = true;
        while (isSearching) {
            String keyword = InputHelper.readRequired("Enter name, ID or specialization: ", "Search keyword")
                    .toLowerCase(Locale.ROOT);
            List<Doctor> results = findDoctors(keyword);
            printTable(results);
            isSearching = results.isEmpty() && InputHelper.readYesNo("Search again?");
        }
    }

    private List<Doctor> findDoctors(String keyword) {
        List<Doctor> results = new ArrayList<>();
        for (Doctor d : data.getDoctors()) {
            if (d.getId().toLowerCase(Locale.ROOT).contains(keyword)
                    || d.getFullName().toLowerCase(Locale.ROOT).contains(keyword)
                    || d.getSpecialization().toLowerCase(Locale.ROOT).contains(keyword)) {
                results.add(d);
            }
        }
        return results;
    }

    private void viewDetails() {
        printDetails(Prompts.chooseDoctor());
    }

    private void updateDoctor() {
        Doctor doctor = Prompts.chooseDoctor();
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

        // The schedule is asked again (instead of losing every answer) if it would leave
        // booked patients outside the new clinic days or hours.
        Set<DayOfWeek> newDays = doctor.getAvailableDays();
        LocalTime newStart = doctor.getStartTime();
        LocalTime newEnd = doctor.getEndTime();
        boolean isScheduleValid = false;
        while (!isScheduleValid) {
            Set<DayOfWeek> days = InputHelper.readOptional("Available days [" + Format.days(doctor.getAvailableDays())
                    + "]: ", Validator::parseDays);
            LocalTime start = InputHelper.readOptional("Clinic start time [" + doctor.getStartTime() + "]: ",
                    s -> validateStartTime(Validator.parseTime(s, "Start time")));
            LocalTime startForEnd = start == null ? doctor.getStartTime() : start;
            LocalTime end = InputHelper.readOptional("Clinic end time [" + doctor.getEndTime() + "]: ",
                    s -> validateEndTime(startForEnd, Validator.parseTime(s, "End time")));
            newDays = days == null ? doctor.getAvailableDays() : days;
            newStart = startForEnd;
            newEnd = end == null ? doctor.getEndTime() : end;
            try {
                validateEndTime(newStart, newEnd);
                checkScheduleConflicts(doctor, newDays, newStart, newEnd);
                isScheduleValid = true;
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
                Format.info("Please enter the schedule again.");
            }
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

    /** A schedule change must not leave booked patients outside the new clinic days or hours. */
    private void checkScheduleConflicts(Doctor doctor, Set<DayOfWeek> days, LocalTime start, LocalTime end)
            throws InvalidInputException {
        int affected = 0;
        for (Appointment a : data.getAppointments()) {
            if (a.isUpcoming() && a.getDoctorId().equals(doctor.getId())
                    && (!days.contains(a.getDate().getDayOfWeek()) || a.getTime().isBefore(start)
                    || a.getTime().plusMinutes(Appointment.SLOT_MINUTES).isAfter(end))) {
                affected++;
            }
        }
        if (affected > 0) {
            throw new InvalidInputException("The new schedule conflicts with " + affected
                    + " upcoming appointment(s). Keep the schedule, or reschedule or cancel those appointments first.");
        }
    }

    /**
     * Past appointments that are still "Scheduled" also count: only this doctor can mark them
     * completed or no-show, so deleting the doctor would leave them stuck. Returns null if there are none.
     */
    private String getDeleteBlocker(Doctor doctor) {
        int scheduled = 0;
        for (Appointment a : data.getAppointments()) {
            if (a.getDoctorId().equals(doctor.getId()) && a.isScheduled()) {
                scheduled++;
            }
        }
        if (scheduled == 0) {
            return null;
        }
        return doctor.getDisplayName() + " has " + scheduled + " scheduled appointment(s) and cannot be deleted. "
                + "Cancel them, or have the doctor mark past ones as completed or no-show first.";
    }

    private List<Doctor> getDeletableDoctors() {
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : data.getDoctors()) {
            if (getDeleteBlocker(d) == null) {
                result.add(d);
            }
        }
        return result;
    }

    private void deleteDoctor() {
        System.out.println("Only doctors without scheduled appointments can be deleted:");
        Doctor doctor = Prompts.chooseDoctor(getDeletableDoctors(), id -> {
            Doctor found = data.findDoctor(id);
            String blocker = getDeleteBlocker(found);
            if (blocker != null) {
                throw new InvalidInputException(blocker);
            }
            return found;
        });
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
        Table table = new Table("ID", "Name", "Specialization", "Fee", "Schedule").alignRight(3).maxWidth(4, 40);
        for (Doctor d : doctors) {
            table.addRow(d.getId(), d.getDisplayName(), d.getSpecialization(), Format.money(d.getConsultationFee()),
                    d.getScheduleText());
        }
        table.print();
    }

    public static void printDetails(Doctor d) {
        Table.details("DOCTOR " + d.getId())
                .addRow("Name", "Dr. " + d.getCompleteName())
                .addRow("Specialization", d.getSpecialization())
                .addRow("PRC license", d.getLicenseNumber())
                .addRow("Mobile number", d.getContactNumber())
                .addRow("Consultation", Format.money(d.getConsultationFee()))
                .addRow("Schedule", d.getScheduleText())
                .addRow("Username", d.getUsername())
                .addRow("Account", PatientForm.accountStatus(d))
                .print();
    }
}
