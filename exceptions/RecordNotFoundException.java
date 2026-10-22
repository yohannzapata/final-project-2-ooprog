package exceptions;

/**
 * Thrown when a record with the given ID does not exist.
 */
public class RecordNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public RecordNotFoundException(String message) {
        super(message);
    }
}
