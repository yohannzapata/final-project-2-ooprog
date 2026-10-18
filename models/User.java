package models;

import util.PasswordUtil;

/**
 * Common data and behavior of everyone who can log in to the system.
 * Admin, Doctor and Patient each provide their own menu through showMenu().
 */
public abstract class User {

    private final String id;
    private String firstName;
    private String middleName = "";
    private String lastName;
    private String username;
    private String passwordHash;
    private String salt;
    private boolean mustChangePassword;

    protected User(String id, String firstName, String lastName, String username, String password) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        setPassword(password);
    }

    /** Used when loading from file, where only the hash and salt are known. */
    protected User(String id, String firstName, String lastName, String username,
                   String passwordHash, String salt, boolean mustChangePassword) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.mustChangePassword = mustChangePassword;
    }

    /** Shows the role-specific menu until the user logs out. */
    public abstract void showMenu();

    public abstract String getRole();

    public boolean checkPassword(String password) {
        return password != null && PasswordUtil.matches(password, salt, passwordHash);
    }

    public final void setPassword(String password) {
        this.salt = PasswordUtil.generateSalt();
        this.passwordHash = PasswordUtil.hash(password, salt);
    }

    /** Name with middle initial, e.g. "Juan S. Dela Cruz". */
    public String getFullName() {
        if (middleName.isEmpty()) {
            return firstName + " " + lastName;
        }
        return firstName + " " + Character.toUpperCase(middleName.charAt(0)) + ". " + lastName;
    }

    /** Complete name for profiles and records, e.g. "Juan Santos Dela Cruz". */
    public String getCompleteName() {
        return middleName.isEmpty() ? firstName + " " + lastName : firstName + " " + middleName + " " + lastName;
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

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName == null ? "" : middleName;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
