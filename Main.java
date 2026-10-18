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

        while (true) {
            Format.header("HOSPITAL MANAGEMENT SYSTEM");
            System.out.println(" 1. Login");
            System.out.println(" 2. Register as a new patient");
            System.out.println(" 0. Exit");
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
                            System.out.println("Thank you for using the Hospital Management System. Goodbye!");
                            return;
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
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String username = InputHelper.readLine("Username: ").trim();
            if (username.equalsIgnoreCase(InputHelper.CANCEL_WORD)) {
                throw new OperationCancelledException();
            }
            String password = InputHelper.readPassword("Password: ");
            try {
                User user = AUTH.login(username, password);
                if (user.isMustChangePassword() && !forcePasswordChange(user)) {
                    return;
                }
                Format.success("Login successful. Welcome, " + user.getDisplayName() + "!");
                user.showMenu(); // polymorphism: Admin, Doctor or Patient menu
                Format.info("You have been logged out.");
                return;
            } catch (LoginFailedException e) {
                int left = MAX_LOGIN_ATTEMPTS - attempt;
                Format.error(e.getMessage() + (left > 0 ? " " + left + " attempt(s) left." : ""));
            }
        }
        Format.error("Too many failed attempts. Returning to the main menu.");
    }

    /** Accounts created by an admin (and the default admin) must set their own password first. */
    private static boolean forcePasswordChange(User user) {
        Format.info("You are using a temporary password. Please set a new password to continue.");
        Prompts.printPasswordRules();
        while (true) {
            try {
                String newPassword = Prompts.readNewPassword(user.getUsername(), "New password");
                AUTH.setNewPassword(user, newPassword, newPassword);
                Format.success("Password updated.");
                return true;
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
            } catch (OperationCancelledException e) {
                Format.info("Password not changed. You need to change it before using the system.");
                return false;
            }
        }
    }

    private static void register() {
        Format.subHeader("Patient Registration");
        System.out.println("Type \"" + InputHelper.CANCEL_WORD + "\" at any prompt to cancel.");
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
