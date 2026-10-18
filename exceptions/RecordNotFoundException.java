package exceptions;

/**
 * Thrown when a record with the given ID does not exist.
 */
public class RecordNotFoundException extends Exception {

    public RecordNotFoundException(String message) {
        super(message);
    }
}
