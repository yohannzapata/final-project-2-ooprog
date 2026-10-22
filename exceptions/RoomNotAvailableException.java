package exceptions;

/**
 * Thrown when the chosen room is occupied or under maintenance.
 */
public class RoomNotAvailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public RoomNotAvailableException(String message) {
        super(message);
    }
}
