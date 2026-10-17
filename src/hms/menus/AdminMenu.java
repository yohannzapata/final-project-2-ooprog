package hms.menus;

import hms.exceptions.InvalidInputException;
import hms.models.Admin;
import hms.models.Admission;
import hms.models.Appointment;
import hms.models.Bill;
import hms.models.Room;
import hms.util.InputHelper;

import java.time.LocalDate;

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
    protected void printSummary() {
        int todayAppointments = 0;
        for (Appointment a : data.getAppointments()) {
            if (a.isScheduled() && a.getDate().equals(LocalDate.now())) {
                todayAppointments++;
            }
        }
        int availableRooms = 0;
        for (Room r : data.getRooms()) {
            if (r.isAvailable()) {
                availableRooms++;
            }
        }
        int admitted = 0;
        for (Admission a : data.getAdmissions()) {
            if (!a.isDischarged()) {
                admitted++;
            }
        }
        int unpaid = 0;
        for (Bill b : data.getBills()) {
            if (!b.isPaid()) {
                unpaid++;
            }
        }
        System.out.println("Logged in as " + admin.getFullName() + " (" + admin.getRole() + ")");
        System.out.printf("Patients: %d | Doctors: %d | Today's appointments: %d%n",
                data.getPatients().size(), data.getDoctors().size(), todayAppointments);
        System.out.printf("Admitted: %d | Available rooms: %d of %d | Unpaid bills: %d%n",
                admitted, availableRooms, data.getRooms().size(), unpaid);
        System.out.println();
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
