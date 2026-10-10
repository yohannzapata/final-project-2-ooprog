package hms.exceptions;

/**
 * Thrown when an input fails a validation rule.
 */
public class InvalidInputException extends Exception {

    public InvalidInputException(String message) {
        super(message);
    }
}
