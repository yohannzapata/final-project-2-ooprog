package hms.models;

/**
 * Common data and behavior of everyone who can log in to the system.
 * Admin, Doctor and Patient each provide their own menu through showMenu().
 */
public abstract class User {

    private final String id;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private boolean mustChangePassword;

    protected User(String id, String firstName, String lastName, String username, String password) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
    }

    /** Shows the role-specific menu until the user logs out. */
    public abstract void showMenu();

    public abstract String getRole();

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Name shown in lists and headers; doctors override this to add "Dr.". */
    public String getDisplayName() {
        return getFullName();
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
