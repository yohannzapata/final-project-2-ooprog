package menus;

import data.HospitalData;
import exceptions.DuplicateRecordException;
import exceptions.InvalidInputException;
import exceptions.OperationCancelledException;
import exceptions.RecordNotFoundException;
import exceptions.RoomNotAvailableException;
import exceptions.ScheduleConflictException;
import util.Format;
import util.InputHelper;

/**
 * Base class of every menu: prints the options, reads a choice and handles errors,
 * so each menu only has to implement what its options do.
 */
public abstract class Menu {

    protected final HospitalData data = HospitalData.getInstance();

    protected abstract String getTitle();

    /** Labels of options 1..n. Option 0 is always back/logout. */
    protected abstract String[] getOptions();

    protected String getExitLabel() {
        return "Back";
    }

    protected abstract void handle(int choice) throws InvalidInputException, RecordNotFoundException,
            DuplicateRecordException, ScheduleConflictException, RoomNotAvailableException;

    /** Optional line(s) printed under the title, e.g. a greeting or quick counts. */
    protected void printSummary() {
    }

    /** Called when the user chooses 0. Return false to stay in the menu. */
    protected boolean onExit() {
        return true;
    }

    public void run() {
        boolean isRunning = true;
        while (isRunning) {
            Format.header(getTitle());
            printSummary();
            String[] options = getOptions();
            printOptions(options);
            int choice = InputHelper.readMenuChoice(0, options.length);
            if (choice == 0) {
                isRunning = !onExit();
            } else {
                runOption(options, choice);
            }
        }
    }

    private void printOptions(String[] options) {
        for (int i = 0; i < options.length; i++) {
            System.out.printf("%2d. %s%n", i + 1, options[i]);
        }
        System.out.printf("%2d. %s%n", 0, getExitLabel());
        System.out.println("Tip: type \"" + InputHelper.CANCEL_WORD + "\" at any prompt to cancel.");
    }

    private void runOption(String[] options, int choice) {
        Format.subHeader(options[choice - 1]);
        try {
            handle(choice);
        } catch (OperationCancelledException e) {
            Format.info("Cancelled. Nothing was changed.");
        } catch (InvalidInputException | RecordNotFoundException | DuplicateRecordException
                 | ScheduleConflictException | RoomNotAvailableException e) {
            Format.error(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            Format.error(e.getMessage());
        }
    }
}
