package hms.menus;

import hms.exceptions.InvalidInputException;
import hms.models.Admin;
import hms.util.InputHelper;

/**
 * Main menu of the admin. Each option opens its own management menu.
 */
public class AdminMenu extends Menu {

    private final Admin admin;

    public AdminMenu(Admin admin) {
        this.admin = admin;
    }

    @Override
    protected String getTitle() {
        return "ADMIN MENU";
    }

    @Override
    protected String[] getOptions() {
        return new String[]{
                "Manage patients",
                "Manage doctors",
                "Manage appointments",
                "Manage rooms",
                "Manage admissions",
                "Manage bills",
                "Change password"
        };
    }

    @Override
    protected String getExitLabel() {
        return "Logout";
    }

    @Override
    protected boolean onExit() {
        return InputHelper.readYesNo("Log out?");
    }

    @Override
    protected void handle(int choice) throws InvalidInputException {
        switch (choice) {
            case 1:
                new PatientManagementMenu().run();
                break;
            case 2:
                new DoctorManagementMenu().run();
                break;
            case 3:
                new AppointmentManagementMenu().run();
                break;
            case 4:
                new RoomManagementMenu().run();
                break;
            case 5:
                new AdmissionManagementMenu().run();
                break;
            case 6:
                new BillingManagementMenu().run();
                break;
            case 7:
                Prompts.changePassword(admin);
                break;
            default:
                break;
        }
    }
}
