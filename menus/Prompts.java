package menus;

import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.RecordNotFoundException;
import models.Doctor;
import models.Patient;
import models.User;
import services.AuthService;
import util.Format;
import util.InputHelper;
import util.Validator;

/**
 * Prompts that are used by more than one menu.
 */
public final class Prompts {

    /** Tries allowed for the current password before the change is stopped. */
    public static final int MAX_PASSWORD_ATTEMPTS = 3;

    /**
     * Looks up a record by the typed ID. Throws if it does not exist, or InvalidInputException
     * with the reason if it exists but cannot be used for this action (e.g. a bill that is already paid).
     */
    @FunctionalInterface
    public interface Finder<T> {
        T find(String id) throws RecordNotFoundException, InvalidInputException;
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
            } catch (RecordNotFoundException | InvalidInputException e) {
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

    public static Patient selectPatient() {
        return select("Enter patient ID: ", HospitalData.getInstance()::findPatient);
    }

    public static Doctor selectDoctor() {
        return select("Enter doctor ID: ", HospitalData.getInstance()::findDoctor);
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

    public static void printPasswordRules() {
        System.out.println("Password: at least 8 characters with an uppercase letter, a lowercase letter and a number.");
    }
}
