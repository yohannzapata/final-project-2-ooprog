package exceptions;

/**
 * Thrown when an input fails a validation rule.
 */
public class InvalidInputException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
