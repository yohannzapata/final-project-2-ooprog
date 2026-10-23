package menus;

import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import exceptions.RoomNotAvailableException;
import models.Doctor;
import models.Patient;
import models.User;
import services.AuthService;
import util.Format;
import util.InputHelper;
import util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Prompts that are used by more than one menu.
 */
public final class Prompts {

    /** Tries allowed for the current password before the change is stopped. */
    public static final int MAX_PASSWORD_ATTEMPTS = 3;

    /** Longer lists are not printed in full before asking for an ID; the user searches by name instead. */
    public static final int LIST_LIMIT = 15;

    // IDs look like P0001 or D0001; names never contain digits, so the two cannot be confused.
    private static final Pattern ID_FORMAT = Pattern.compile("^[A-Za-z]{1,3}\\d+$");

    /**
     * Looks up a record by the typed ID. Throws if it does not exist, or InvalidInputException
     * (RoomNotAvailableException for a room) with the reason if it exists but cannot be used for this
     * action, e.g. a bill that is already paid or a room that is occupied.
     */
    @FunctionalInterface
    public interface Finder<T> {
        T find(String id) throws RecordNotFoundException, InvalidInputException, RoomNotAvailableException;
    }

    private Prompts() {
    }

    /** Keeps asking for an ID until a record that can be used is entered, or the user types "back". */
    public static <T> T select(String prompt, Finder<T> finder) {
        T record = null;
        boolean isFound = false;
        boolean isHintShown = false;
        while (!isFound) {
            String id = InputHelper.readId(prompt);
            try {
                record = finder.find(id);
                isFound = true;
            } catch (RecordNotFoundException | InvalidInputException | RoomNotAvailableException e) {
                Format.error(e.getMessage());
                if (!isHintShown) {
                    Format.info("Try again with one from the list, or type \"" + InputHelper.CANCEL_WORD
                            + "\" to cancel.");
                    isHintShown = true;
                }
            }
        }
        return record;
    }

    /**
     * Shows the patients to choose from and asks for an ID. Typing part of a name instead lists the
     * matching patients, so a long list does not have to be scrolled through.
     */
    public static Patient choosePatient(List<Patient> candidates, Finder<Patient> finder) {
        return choose("patient", candidates, Patient::getCompleteName, PatientManagementMenu::printTable, finder);
    }

    public static Doctor chooseDoctor(List<Doctor> candidates, Finder<Doctor> finder) {
        return choose("doctor", candidates, Doctor::getCompleteName, DoctorManagementMenu::printTable, finder);
    }

    public static Patient choosePatient() {
        HospitalData data = HospitalData.getInstance();
        return choosePatient(data.getPatients(), data::findPatient);
    }

    public static Doctor chooseDoctor() {
        HospitalData data = HospitalData.getInstance();
        return chooseDoctor(data.getDoctors(), data::findDoctor);
    }

    private static <T> T choose(String noun, List<T> candidates, Function<T, String> nameOf,
                                Consumer<List<T>> printer, Finder<T> finder) {
        if (candidates.size() <= LIST_LIMIT) {
            printer.accept(candidates);
        } else {
            Format.info(candidates.size() + " " + noun + "s on file. Type part of a name to find the ID.");
        }
        T record = null;
        boolean isFound = false;
        boolean isHintShown = false;
        while (!isFound) {
            String input = InputHelper.read("Enter " + noun + " ID or name: ", String::trim);
            try {
                if (ID_FORMAT.matcher(input).matches()) {
                    record = finder.find(input.toUpperCase(Locale.ROOT));
                    isFound = true;
                } else {
                    printNameMatches(noun, candidates, nameOf, printer, input);
                }
            } catch (RecordNotFoundException | InvalidInputException | RoomNotAvailableException e) {
                Format.error(e.getMessage());
                if (!isHintShown) {
                    Format.info("Try again with one from the list, or type \"" + InputHelper.CANCEL_WORD
                            + "\" to cancel.");
                    isHintShown = true;
                }
            }
        }
        return record;
    }

    private static <T> void printNameMatches(String noun, List<T> candidates, Function<T, String> nameOf,
                                             Consumer<List<T>> printer, String input) throws InvalidInputException {
        String keyword = input.toLowerCase(Locale.ROOT);
        List<T> matches = new ArrayList<>();
        for (T item : candidates) {
            if (nameOf.apply(item).toLowerCase(Locale.ROOT).contains(keyword)) {
                matches.add(item);
            }
        }
        if (matches.isEmpty()) {
            throw new InvalidInputException("No " + noun + " in the list matches \"" + input + "\".");
        }
        printer.accept(matches);
        Format.info("Enter the ID of the " + noun + " you mean.");
    }

    /** Asks for a new password twice and checks the strength rules. */
    public static String readNewPassword(String username, String label) {
        String password = "";
        boolean isAccepted = false;
        while (!isAccepted) {
            password = InputHelper.readPassword(label + ": ");
            try {
                Validator.validatePassword(password, username);
                String confirm = InputHelper.readPassword("Confirm " + label.toLowerCase() + ": ");
                if (!password.equals(confirm)) {
                    throw new InvalidInputException("Passwords do not match. Please try again.");
                }
                isAccepted = true;
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
            }
        }
        return password;
    }

    public static String readNewUsername() {
        AuthService auth = new AuthService();
        System.out.println("Username: 4-20 characters, letters/numbers/period/underscore, must start with a letter.");
        String username = "";
        boolean isAccepted = false;
        while (!isAccepted) {
            String input = InputHelper.read("Username: ", s -> s);
            try {
                username = auth.checkNewUsername(input);
                isAccepted = true;
            } catch (InvalidInputException | DuplicateRecordException e) {
                Format.error(e.getMessage());
            }
        }
        return username;
    }

    /**
     * Change password from any role's menu. The current password is asked first, with a few tries,
     * and a new password that breaks a rule is asked again instead of ending the change.
     */
    public static void changePassword(User user) throws InvalidInputException {
        String current = "";
        int attemptsLeft = MAX_PASSWORD_ATTEMPTS;
        boolean isVerified = false;
        boolean isAsking = true;
        while (isAsking) {
            current = InputHelper.readPassword("Current password: ");
            isVerified = user.checkPassword(current);
            attemptsLeft--;
            isAsking = !isVerified && attemptsLeft > 0;
            if (isAsking) {
                Format.error("Current password is incorrect. " + attemptsLeft + " attempt(s) left.");
            }
        }
        if (!isVerified) {
            throw new InvalidInputException("Current password is incorrect. Your password was not changed.");
        }
        printPasswordRules();
        AuthService auth = new AuthService();
        boolean isChanged = false;
        while (!isChanged) {
            String newPassword = readNewPassword(user.getUsername(), "New password");
            try {
                auth.changePassword(user, current, newPassword, newPassword);
                isChanged = true;
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
            }
        }
        Format.success("Password changed.");
    }

    /** Admin action: gives a doctor or patient a temporary password they must change at their next login. */
    public static void resetPassword(User user) throws InvalidInputException {
        System.out.println("Username: " + user.getUsername());
        if (!InputHelper.confirm("Reset the password of " + user.getDisplayName() + "?")) {
            Format.info("Password not reset.");
            return;
        }
        printPasswordRules();
        String temporary = readNewPassword(user.getUsername(), "Temporary password");
        new AuthService().resetPassword(user, temporary);
        Format.success("Password reset. Give the temporary password to " + user.getDisplayName()
                + "; it must be changed at the next login.");
    }

    public static void printPasswordRules() {
        System.out.println("Password rules: 8+ characters, with upper, lower and a number.");
    }
}
