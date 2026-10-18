package exceptions;

/**
 * Thrown when a doctor or patient already has an appointment at that date and time.
 */
public class ScheduleConflictException extends Exception {

    public ScheduleConflictException(String message) {
        super(message);
    }
}
