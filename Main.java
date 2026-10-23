import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.LoginFailedException;
import exceptions.OperationCancelledException;
import menus.PatientForm;
import menus.Prompts;
import models.Patient;
import models.User;
import services.AuthService;
import util.Format;
import util.InputHelper;

/**
 * Entry point of the Hospital Management System.
 * Shows the main menu (Login, Register, Exit) and sends each user to their role's menu.
 */
public class Main {

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private static final AuthService AUTH = new AuthService();

    public static void main(String[] args) {
        HospitalData data = HospitalData.getInstance();
        data.load();
        Format.info("Records are saved in " + data.getDataFolder());

        boolean isRunning = true;
        while (isRunning) {
            Format.header("HOSPITAL MANAGEMENT SYSTEM");
            System.out.println(" 1. Login");
            System.out.println(" 2. Register as a new patient");
            System.out.println(" 0. Exit");
            System.out.println();
            int choice = InputHelper.readMenuChoice(0, 2);
            try {
                switch (choice) {
                    case 1:
                        login();
                        break;
                    case 2:
                        register();
                        break;
                    case 0:
                        if (InputHelper.readYesNo("Exit the program?")) {
                            data.save();
                            System.out.println();
                            System.out.println("Thank you for using the Hospital Management System. Goodbye!");
                            isRunning = false;
                        }
                        break;
                    default:
                        break;
                }
            } catch (OperationCancelledException e) {
                Format.info("Cancelled. Returning to the main menu.");
            }
        }
    }

    private static void login() {
        Format.subHeader("Login");
        User user = null;
        int attempt = 0;
        boolean isLoggingIn = true;
        boolean isLocked = false;
        while (isLoggingIn) {
            attempt++;
            // Blank input is asked again and does not use up an attempt.
            String username = InputHelper.read("Username: ", s -> s.trim());
            String password = InputHelper.readPassword("Password: ");
            try {
                user = AUTH.login(username, password);
                isLoggingIn = false;
            } catch (LoginFailedException e) {
                int left = e.isAccountLocked() ? 0 : MAX_LOGIN_ATTEMPTS - attempt;
                Format.error(e.getMessage() + (left > 0 ? " " + left + " attempt(s) left." : ""));
                isLoggingIn = left > 0;
                isLocked = e.isAccountLocked();
            }
        }
        if (user == null) {
            if (isLocked) {
                Format.info("Returning to the main menu.");
            } else {
                Format.error("Too many failed attempts. Returning to the main menu.");
            }
            return;
        }
        if (user.isMustChangePassword() && !forcePasswordChange(user)) {
            return;
        }
        Format.success("Login successful. Welcome, " + user.getDisplayName() + "!");
        user.showMenu(); // polymorphism: Admin, Doctor or Patient menu
        Format.info("You have been logged out.");
    }

    /** Accounts created by an admin (and the default admin) must set their own password first. */
    private static boolean forcePasswordChange(User user) {
        Format.info("Temporary password. Please set a new one.");
        Prompts.printPasswordRules();
        boolean isChanged = false;
        boolean isAsking = true;
        while (isAsking) {
            try {
                String newPassword = Prompts.readNewPassword(user.getUsername(), "New password");
                AUTH.setNewPassword(user, newPassword, newPassword);
                Format.success("Password updated.");
                isChanged = true;
                isAsking = false;
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
            } catch (OperationCancelledException e) {
                Format.info("Password not changed. You need to change it before using the system.");
                isAsking = false;
            }
        }
        return isChanged;
    }

    private static void register() {
        Format.subHeader("Patient Registration");
        Format.cancelHint();
        try {
            Patient patient = PatientForm.create(false);
            Format.success("Registration successful! Your patient ID is " + patient.getId() + ".");
            Format.info("You can now log in as \"" + patient.getUsername() + "\".");
        } catch (DuplicateRecordException e) {
            Format.error(e.getMessage());
            Format.info("If this is you, please log in or ask the admitting section to recover your account.");
        } catch (InvalidInputException e) {
            Format.error(e.getMessage());
        }
    }
}
