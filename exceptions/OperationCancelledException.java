package exceptions;

/**
 * Thrown by InputHelper when the user types "back" at a prompt.
 * Unchecked so that every menu can simply catch it and return.
 */
public class OperationCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperationCancelledException() {
        super("Operation cancelled.");
    }
}
