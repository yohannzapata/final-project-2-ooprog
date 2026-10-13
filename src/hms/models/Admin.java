package hms.models;

import hms.menus.AdminMenu;
import hms.util.RecordFormat;

/**
 * Hospital staff that manages patients, doctors, rooms, admissions and bills.
 */
public class Admin extends User {

    public Admin(String id, String firstName, String lastName, String username, String password) {
        super(id, firstName, lastName, username, password);
    }

    private Admin(String id, String firstName, String lastName, String username,
                  String passwordHash, String salt, boolean mustChangePassword) {
        super(id, firstName, lastName, username, passwordHash, salt, mustChangePassword);
    }

    @Override
    public void showMenu() {
        new AdminMenu(this).run();
    }

    @Override
    public String getRole() {
        return "Admin";
    }

    public String toRecord() {
        return RecordFormat.join(getId(), getFirstName(), getLastName(), getUsername(),
                getPasswordHash(), getSalt(), String.valueOf(isMustChangePassword()));
    }

    public static Admin fromRecord(String[] f) {
        return new Admin(f[0], f[1], f[2], f[3], f[4], f[5], Boolean.parseBoolean(f[6]));
    }
}
