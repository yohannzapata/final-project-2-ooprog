package exceptions;

/**
 * Thrown when a username, room number or record already exists.
 */
public class DuplicateRecordException extends Exception {

    public DuplicateRecordException(String message) {
        super(message);
    }
}
