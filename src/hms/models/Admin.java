package hms.models;

import hms.menus.AdminMenu;

/**
 * Hospital staff that manages patients, doctors, rooms, admissions and bills.
 */
public class Admin extends User {

    public Admin(String id, String firstName, String lastName, String username, String password) {
        super(id, firstName, lastName, username, password);
    }

    @Override
    public void showMenu() {
        new AdminMenu(this).run();
    }

    @Override
    public String getRole() {
        return "Admin";
    }
}
