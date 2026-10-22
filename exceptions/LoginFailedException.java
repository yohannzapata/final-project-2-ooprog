package exceptions;

/**
 * Thrown when the username or password is incorrect.
 */
public class LoginFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    public LoginFailedException(String message) {
        super(message);
    }
}
