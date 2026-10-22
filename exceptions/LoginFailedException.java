package exceptions;

/**
 * Thrown when the username or password is incorrect, or the account is locked.
 */
public class LoginFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final boolean accountLocked;

    public LoginFailedException(String message) {
        this(message, false);
    }

    public LoginFailedException(String message, boolean accountLocked) {
        super(message);
        this.accountLocked = accountLocked;
    }

    /** True when the account is locked, so trying again is pointless until the admin resets the password. */
    public boolean isAccountLocked() {
        return accountLocked;
    }
}
