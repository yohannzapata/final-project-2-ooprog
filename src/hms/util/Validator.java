package hms.util;

import hms.exceptions.InvalidInputException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Static validation methods used by every input in the system.
 * Each method either returns the cleaned value or throws InvalidInputException
 * with a message that can be shown directly to the user.
 */
public final class Validator {

    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    public static final int MAX_AGE = 120;
    public static final int SENIOR_AGE = 60;

    // Letters (including accented letters like n with tilde), optionally separated by a space, hyphen,
    // apostrophe or period, e.g. "Ma. Clara", "Dela Cruz-Santos", "O'Brien".
    private static final Pattern NAME = Pattern.compile("^\\p{L}+(?:(?:\\. |[ '\\-.])\\p{L}+)*\\.?$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9._]{2,18}[A-Za-z0-9]$");
    private static final Pattern MOBILE = Pattern.compile("^09\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9](?:[A-Za-z0-9._%+-]*[A-Za-z0-9])?@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$");
    private static final Pattern ADDRESS = Pattern.compile("^[\\p{L}0-9 .,#'\\-/()]+$");
    private static final Pattern PWD_ID = Pattern.compile("^\\d{2}-\\d{4}-\\d{3}-\\d{7}$");
    private static final Pattern SENIOR_ID = Pattern.compile("^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$");
    private static final Pattern PRC_LICENSE = Pattern.compile("^\\d{7}$");
    private static final Pattern ROOM_NUMBER = Pattern.compile("^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)?$");
    private static final Pattern SPECIALIZATION = Pattern.compile("^\\p{L}+(?:[ \\-&/,]+\\p{L}+)*$");

    private Validator() {
    }

    // ---------------------------------------------------------------- general text

    public static String requireNonEmpty(String value, String field) throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(field + " is required.");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public static String validateText(String value, String field, int maxLength, boolean required)
            throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            if (required) {
                throw new InvalidInputException(field + " is required.");
            }
            return "";
        }
        String text = value.trim().replaceAll("\\s+", " ");
        if (text.length() > maxLength) {
            throw new InvalidInputException(field + " must not exceed " + maxLength + " characters.");
        }
        if (!text.matches(".*[\\p{L}\\p{N}].*")) {
            throw new InvalidInputException(field + " must contain letters or numbers.");
        }
        return text;
    }

    public static String validateName(String value, String field) throws InvalidInputException {
        String name = requireNonEmpty(value, field);
        if (name.length() < 2 || name.length() > 50) {
            throw new InvalidInputException(field + " must be 2 to 50 characters long.");
        }
        if (!NAME.matcher(name).matches()) {
            throw new InvalidInputException(field
                    + " may only contain letters, spaces, hyphens, apostrophes and periods.");
        }
        // Only fix the casing when everything was typed in one case ("JUAN" or "juan").
        // Mixed case like "McArthur" or "de la Cruz" is kept as the user typed it.
        if (name.equals(name.toUpperCase(Locale.ROOT)) || name.equals(name.toLowerCase(Locale.ROOT))) {
            return toTitleCase(name);
        }
        return name;
    }

    /** Capitalizes the first letter of each word, e.g. "juan dela cruz" becomes "Juan Dela Cruz". */
    private static String toTitleCase(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(capitalizeNext ? Character.toUpperCase(c) : Character.toLowerCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
                capitalizeNext = true;
            }
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- account

    public static String validateUsername(String value) throws InvalidInputException {
        String username = requireNonEmpty(value, "Username");
        if (username.contains(" ")) {
            throw new InvalidInputException("Username must not contain spaces.");
        }
        if (username.length() < 4 || username.length() > 20) {
            throw new InvalidInputException("Username must be 4 to 20 characters long.");
        }
        if (!USERNAME.matcher(username).matches()) {
            throw new InvalidInputException("Username must start with a letter, end with a letter or number, "
                    + "and may only contain letters, numbers, periods and underscores.");
        }
        if (username.contains("..") || username.contains("__") || username.contains("._")
                || username.contains("_.")) {
            throw new InvalidInputException("Username must not have consecutive periods or underscores.");
        }
        return username.toLowerCase(Locale.ROOT);
    }

    public static void validatePassword(String password, String username) throws InvalidInputException {
        if (password == null || password.isEmpty()) {
            throw new InvalidInputException("Password is required.");
        }
        if (password.length() < 8) {
            throw new InvalidInputException("Password must be at least 8 characters long.");
        }
        if (password.length() > 64) {
            throw new InvalidInputException("Password must not exceed 64 characters.");
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidInputException("Password must not contain spaces.");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new InvalidInputException("Password must contain at least one uppercase letter.");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            throw new InvalidInputException("Password must contain at least one lowercase letter.");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new InvalidInputException("Password must contain at least one number.");
        }
        if (username != null && !username.isEmpty()
                && password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            throw new InvalidInputException("Password must not contain your username.");
        }
    }

    // ---------------------------------------------------------------- contact details

    /** Accepts 09XXXXXXXXX (spaces/dashes allowed) and returns it without spaces. */
    public static String validateContactNumber(String value) throws InvalidInputException {
        String raw = requireNonEmpty(value, "Contact number");
        if (!raw.matches("^[0-9 ()\\-]+$")) {
            throw new InvalidInputException("Contact number must contain digits only.");
        }
        String digits = raw.replaceAll("[ ()\\-]", "");
        if (!MOBILE.matcher(digits).matches()) {
            throw new InvalidInputException("Contact number must be 11 digits and start with 09 (e.g. 09171234567).");
        }
        return digits;
    }

    public static String validateEmail(String value, boolean required) throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            if (required) {
                throw new InvalidInputException("Email is required.");
            }
            return "";
        }
        String email = value.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254) {
            throw new InvalidInputException("Email must not exceed 254 characters.");
        }
        if (email.contains("..") || !EMAIL.matcher(email).matches()) {
            throw new InvalidInputException("Email must follow the format name@domain.com.");
        }
        return email;
    }

    public static String validateAddress(String value) throws InvalidInputException {
        String address = requireNonEmpty(value, "Address");
        if (address.length() < 5 || address.length() > 150) {
            throw new InvalidInputException("Address must be 5 to 150 characters long.");
        }
        if (!ADDRESS.matcher(address).matches()) {
            throw new InvalidInputException("Address contains invalid characters.");
        }
        if (!address.matches(".*\\p{L}.*")) {
            throw new InvalidInputException("Address must contain a street, barangay or city name.");
        }
        return address;
    }

    public static String validateSex(String value) throws InvalidInputException {
        String sex = requireNonEmpty(value, "Sex").toUpperCase(Locale.ROOT);
        switch (sex) {
            case "M":
            case "MALE":
                return "Male";
            case "F":
            case "FEMALE":
                return "Female";
            default:
                throw new InvalidInputException("Sex must be M (Male) or F (Female).");
        }
    }

    // ---------------------------------------------------------------- government IDs

    /** PWD ID number issued by the LGU, format RR-PPMM-BBB-NNNNNNN. */
    public static String validatePwdId(String value) throws InvalidInputException {
        String id = requireNonEmpty(value, "PWD ID number").replace(" ", "");
        if (!PWD_ID.matcher(id).matches()) {
            throw new InvalidInputException("PWD ID number must follow the format RR-PPMM-BBB-NNNNNNN "
                    + "(e.g. 13-7604-000-0001234).");
        }
        return id;
    }

    /** OSCA Senior Citizen ID. Formats differ per city, so only basic rules are checked. */
    public static String validateSeniorId(String value) throws InvalidInputException {
        String id = requireNonEmpty(value, "Senior Citizen ID number").replace(" ", "").toUpperCase(Locale.ROOT);
        if (id.length() < 4 || id.length() > 20 || !SENIOR_ID.matcher(id).matches()
                || !id.matches(".*\\d.*")) {
            throw new InvalidInputException("Senior Citizen ID number must be 4 to 20 letters/numbers "
                    + "(dashes allowed) and contain at least one number.");
        }
        return id;
    }

    /** PhilHealth Identification Number: 12 digits, stored as XX-XXXXXXXXX-X. */
    public static String validatePhilHealthNumber(String value) throws InvalidInputException {
        String raw = requireNonEmpty(value, "PhilHealth number");
        String digits = raw.replaceAll("[ \\-]", "");
        if (!digits.matches("^\\d{12}$")) {
            throw new InvalidInputException("PhilHealth number must be 12 digits (e.g. 12-345678901-2).");
        }
        return digits.substring(0, 2) + "-" + digits.substring(2, 11) + "-" + digits.substring(11);
    }

    /** PRC license number of a physician (7 digits). */
    public static String validateLicenseNumber(String value) throws InvalidInputException {
        String license = requireNonEmpty(value, "PRC license number").replace(" ", "");
        if (!PRC_LICENSE.matcher(license).matches()) {
            throw new InvalidInputException("PRC license number must be exactly 7 digits.");
        }
        if (license.matches("^0+$")) {
            throw new InvalidInputException("PRC license number is not valid.");
        }
        return license;
    }

    // ---------------------------------------------------------------- numbers

    public static int parseInt(String value, String field) throws InvalidInputException {
        String text = requireNonEmpty(value, field);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InvalidInputException(field + " must be a whole number.");
        }
    }

    public static int validateRange(int value, int min, int max, String field) throws InvalidInputException {
        if (value < min || value > max) {
            throw new InvalidInputException(field + " must be from " + min + " to " + max + ".");
        }
        return value;
    }

    /** Parses a peso amount. Must be greater than zero, at most the given maximum, with up to 2 decimals. */
    public static BigDecimal validateAmount(String value, String field, BigDecimal max) throws InvalidInputException {
        String text = requireNonEmpty(value, field).replace(",", "").replace("PHP", "").replace("Php", "").trim();
        BigDecimal amount;
        try {
            amount = new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new InvalidInputException(field + " must be a number (e.g. 1500 or 1500.50).");
        }
        if (amount.signum() <= 0) {
            throw new InvalidInputException(field + " must be greater than 0.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidInputException(field + " can only have up to 2 decimal places.");
        }
        if (max != null && amount.compareTo(max) > 0) {
            throw new InvalidInputException(field + " must not exceed " + Format.money(max) + ".");
        }
        return amount.setScale(2);
    }

    // ---------------------------------------------------------------- dates and times

    public static LocalDate parseDate(String value, String field) throws InvalidInputException {
        String text = requireNonEmpty(value, field);
        try {
            return LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException(field + " must be a valid date in the format yyyy-MM-dd (e.g. 2026-10-21).");
        }
    }

    public static LocalTime parseTime(String value, String field) throws InvalidInputException {
        String text = requireNonEmpty(value, field);
        try {
            return LocalTime.parse(text, TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException(field + " must be a valid 24-hour time in the format HH:mm (e.g. 14:30).");
        }
    }

    public static LocalDate validateBirthdate(LocalDate birthdate) throws InvalidInputException {
        LocalDate today = LocalDate.now();
        if (birthdate.isAfter(today)) {
            throw new InvalidInputException("Birthdate cannot be in the future.");
        }
        int age = Period.between(birthdate, today).getYears();
        if (age > MAX_AGE) {
            throw new InvalidInputException("Age must be from 0 to " + MAX_AGE + " years.");
        }
        return birthdate;
    }

    public static LocalDate validateNotPast(LocalDate date, String field) throws InvalidInputException {
        if (date.isBefore(LocalDate.now())) {
            throw new InvalidInputException(field + " cannot be in the past.");
        }
        return date;
    }

    public static LocalDate validateNotFuture(LocalDate date, String field) throws InvalidInputException {
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidInputException(field + " cannot be in the future.");
        }
        return date;
    }

    /**
     * Parses doctor schedule days such as "MON,WED,FRI", "Mon-Fri" or "TUE, THU, SAT".
     */
    public static Set<DayOfWeek> parseDays(String value) throws InvalidInputException {
        String text = requireNonEmpty(value, "Available days").toUpperCase(Locale.ROOT).replace(" ", "");
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String part : text.split(",")) {
            if (part.isEmpty()) {
                throw new InvalidInputException("Available days has an empty entry. Use a format like MON,WED,FRI.");
            }
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length != 2) {
                    throw new InvalidInputException("Invalid day range \"" + part + "\". Use a format like MON-FRI.");
                }
                DayOfWeek start = toDay(range[0]);
                DayOfWeek end = toDay(range[1]);
                if (end.getValue() < start.getValue()) {
                    throw new InvalidInputException("Day range must go forward within the week (e.g. MON-FRI).");
                }
                for (int d = start.getValue(); d <= end.getValue(); d++) {
                    days.add(DayOfWeek.of(d));
                }
            } else {
                DayOfWeek day = toDay(part);
                if (!days.add(day)) {
                    throw new InvalidInputException(Format.day(day) + " was entered more than once.");
                }
            }
        }
        return days;
    }

    private static DayOfWeek toDay(String text) throws InvalidInputException {
        for (DayOfWeek day : DayOfWeek.values()) {
            String full = day.name();
            if (text.equals(full) || (text.length() >= 3 && full.startsWith(text))) {
                return day;
            }
        }
        throw new InvalidInputException("\"" + text + "\" is not a valid day. Use MON, TUE, WED, THU, FRI, SAT or SUN.");
    }

    // ---------------------------------------------------------------- hospital specific

    public static String validateRoomNumber(String value) throws InvalidInputException {
        String room = requireNonEmpty(value, "Room number").toUpperCase(Locale.ROOT);
        if (room.length() > 10 || !ROOM_NUMBER.matcher(room).matches()) {
            throw new InvalidInputException("Room number must be up to 10 letters/numbers with at most one dash "
                    + "(e.g. 201 or ICU-01).");
        }
        return room;
    }

    public static String validateSpecialization(String value) throws InvalidInputException {
        String spec = requireNonEmpty(value, "Specialization");
        if (spec.length() < 3 || spec.length() > 50) {
            throw new InvalidInputException("Specialization must be 3 to 50 characters long.");
        }
        if (!SPECIALIZATION.matcher(spec).matches()) {
            throw new InvalidInputException("Specialization may only contain letters, spaces, hyphens, and \"&\".");
        }
        return spec.equals(spec.toLowerCase(Locale.ROOT)) ? toTitleCase(spec) : spec;
    }

    public static boolean parseYesNo(String value) throws InvalidInputException {
        String answer = requireNonEmpty(value, "Answer").toUpperCase(Locale.ROOT);
        if (answer.equals("Y") || answer.equals("YES")) {
            return true;
        }
        if (answer.equals("N") || answer.equals("NO")) {
            return false;
        }
        throw new InvalidInputException("Please enter Y or N.");
    }
}
