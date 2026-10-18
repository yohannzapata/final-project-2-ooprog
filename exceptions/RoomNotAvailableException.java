package exceptions;

/**
 * Thrown when the chosen room is occupied or under maintenance.
 */
public class RoomNotAvailableException extends Exception {

    public RoomNotAvailableException(String message) {
        super(message);
    }
}
