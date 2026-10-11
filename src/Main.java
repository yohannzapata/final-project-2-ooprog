import hms.data.HospitalData;
import hms.exceptions.DuplicateRecordException;
import hms.exceptions.LoginFailedException;
import hms.exceptions.OperationCancelledException;
import hms.menus.PatientForm;
import hms.models.Patient;
import hms.models.User;
import hms.services.AuthService;
import hms.util.Format;
import hms.util.InputHelper;

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
        }
    }
}
