package menus;

import models.Admin;
import models.Admission;
import models.Appointment;
import models.Bill;
import models.Room;
import models.User;
import services.AdmissionService;
import services.BillingService;
import util.Format;
import util.InputHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Main menu of the admin. Each option opens its own management menu.
 */
public class AdminMenu extends Menu {

    private final Admin admin;
    private final AdmissionService admissions = new AdmissionService();
    private final BillingService billing = new BillingService();

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
        int locked = countLocked();
        if (locked > 0) {
            Format.info(locked + " account(s) locked after too many wrong passwords. Reset the password under "
                    + "Manage patients or Manage doctors to unlock.");
        }
        if (!canManageAppointments() || !canManageAdmissions() || !canManageBills()) {
            Format.info("Appointments, admissions and bills appear here once there are patients, doctors "
                    + "and rooms to work with.");
        }
        System.out.println();
    }

    private int countLocked() {
        int locked = 0;
        for (User user : data.getAllUsers()) {
            if (user.isLocked() && !(user instanceof Admin)) {
                locked++;
            }
        }
        return locked;
    }

    private boolean canManageAppointments() {
        boolean canBook = !data.getPatients().isEmpty() && !data.getDoctors().isEmpty();
        return canBook || !data.getAppointments().isEmpty();
    }

    private boolean canManageAdmissions() {
        boolean canAdmit = !data.getPatients().isEmpty() && !admissions.getAvailableRooms().isEmpty();
        return canAdmit || !data.getAdmissions().isEmpty();
    }

    private boolean canManageBills() {
        return !data.getBills().isEmpty() || !billing.getUnbilledAppointments().isEmpty()
                || !billing.getUnbilledAdmissions().isEmpty();
    }

    @Override
    protected List<Option> getOptions() {
        List<Option> options = new ArrayList<>();
        options.add(new Option("Manage patients", () -> new PatientManagementMenu().run()));
        options.add(new Option("Manage doctors", () -> new DoctorManagementMenu().run()));
        addIf(options, canManageAppointments(), "Manage appointments", () -> new AppointmentManagementMenu().run());
        options.add(new Option("Manage rooms", () -> new RoomManagementMenu().run()));
        addIf(options, canManageAdmissions(), "Manage admissions", () -> new AdmissionManagementMenu().run());
        addIf(options, canManageBills(), "Manage bills", () -> new BillingManagementMenu().run());
        options.add(new Option("Change password", () -> Prompts.changePassword(admin)));
        return options;
    }

    @Override
    protected String getExitLabel() {
        return "Logout";
    }

    @Override
    protected boolean onExit() {
        return InputHelper.readYesNo("Log out?");
    }
}
