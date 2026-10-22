package exceptions;

/**
 * Thrown when a doctor or patient already has an appointment at that date and time.
 */
public class ScheduleConflictException extends Exception {

    private static final long serialVersionUID = 1L;

    public ScheduleConflictException(String message) {
        super(message);
    }
}
