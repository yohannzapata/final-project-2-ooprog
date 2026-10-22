package exceptions;

/**
 * Thrown when a username, room number or record already exists.
 */
public class DuplicateRecordException extends Exception {

    private static final long serialVersionUID = 1L;

    public DuplicateRecordException(String message) {
        super(message);
    }
}
