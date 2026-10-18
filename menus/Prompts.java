package menus;

import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.OperationCancelledException;
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

    /** Looks up a record by the typed ID; throws if it does not exist or the user may not see it. */
    @FunctionalInterface
    public interface Finder<T> {
        T find(String id) throws RecordNotFoundException;
    }

    private Prompts() {
    }

    /** Keeps asking for an ID until an existing (and allowed) record is entered. */
    public static <T> T select(String prompt, Finder<T> finder) {
        while (true) {
            String id = InputHelper.readId(prompt);
            try {
                return finder.find(id);
            } catch (RecordNotFoundException e) {
                Format.error(e.getMessage());
            }
        }
    }

    public static Patient selectPatient() {
        return select("Enter patient ID: ", HospitalData.getInstance()::findPatient);
    }

    public static Doctor selectDoctor() {
        return select("Enter doctor ID: ", HospitalData.getInstance()::findDoctor);
    }

    /** Asks for a new password twice and checks the strength rules. */
    public static String readNewPassword(String username, String label) {
        while (true) {
            String password = InputHelper.readPassword(label + ": ");
            try {
                Validator.validatePassword(password, username);
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
                continue;
            }
            String confirm = InputHelper.readPassword("Confirm " + label.toLowerCase() + ": ");
            if (!password.equals(confirm)) {
                Format.error("Passwords do not match. Please try again.");
                continue;
            }
            return password;
        }
    }

    public static String readNewUsername() {
        AuthService auth = new AuthService();
        System.out.println("Username: 4-20 characters, letters/numbers/period/underscore, must start with a letter.");
        while (true) {
            String input = InputHelper.readLine("Username: ");
            if (input.trim().equalsIgnoreCase(InputHelper.CANCEL_WORD)) {
                throw new OperationCancelledException();
            }
            try {
                return auth.checkNewUsername(input);
            } catch (InvalidInputException | DuplicateRecordException e) {
                Format.error(e.getMessage());
            }
        }
    }

    /** Change password from any role's menu. The current password is asked first. */
    public static void changePassword(User user) throws InvalidInputException {
        String current = InputHelper.readPassword("Current password: ");
        if (!user.checkPassword(current)) {
            throw new InvalidInputException("Current password is incorrect.");
        }
        printPasswordRules();
        String newPassword = readNewPassword(user.getUsername(), "New password");
        new AuthService().changePassword(user, current, newPassword, newPassword);
        Format.success("Password changed.");
    }

    public static void printPasswordRules() {
        System.out.println("Password: at least 8 characters with an uppercase letter, a lowercase letter and a number.");
    }
}
