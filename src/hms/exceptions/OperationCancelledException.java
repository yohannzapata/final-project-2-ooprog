package hms.exceptions;

/**
 * Thrown by InputHelper when the user types "back" at a prompt.
 * Unchecked so that every menu can simply catch it and return.
 */
public class OperationCancelledException extends RuntimeException {

    public OperationCancelledException() {
        super("Operation cancelled.");
    }
}
