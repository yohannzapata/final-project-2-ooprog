package hms.exceptions;

/**
 * Thrown when the username or password is incorrect.
 */
public class LoginFailedException extends Exception {

    public LoginFailedException(String message) {
        super(message);
    }
}
