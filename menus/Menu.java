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

import java.util.List;

/**
 * Base class of every menu: prints the options, reads a choice and handles errors,
 * so each menu only has to list its options and what they do.
 * Options that need records (e.g. "Delete patient") are only listed while such records exist,
 * so the user never picks an option that can only answer "nothing found".
 */
public abstract class Menu {

    /** What happens when an option is chosen. */
    @FunctionalInterface
    protected interface Action {
        void run() throws InvalidInputException, RecordNotFoundException, DuplicateRecordException,
                ScheduleConflictException, RoomNotAvailableException;
    }

    /** One numbered line of a menu. */
    protected static final class Option {
        private final String label;
        private final Action action;

        public Option(String label, Action action) {
            this.label = label;
            this.action = action;
        }
    }

    protected final HospitalData data = HospitalData.getInstance();

    protected abstract String getTitle();

    /** Options that can be used right now, numbered from 1. Option 0 is always back/logout. */
    protected abstract List<Option> getOptions();

    protected String getExitLabel() {
        return "Back";
    }

    /** Optional line(s) printed under the title, e.g. a greeting or quick counts. */
    protected void printSummary() {
    }

    /** Called when the user chooses 0. Return false to stay in the menu. */
    protected boolean onExit() {
        return true;
    }

    /** Adds the option only when it can be used, e.g. "Delete patient" only when there are patients. */
    protected static void addIf(List<Option> options, boolean available, String label, Action action) {
        if (available) {
            options.add(new Option(label, action));
        }
    }

    public void run() {
        boolean isRunning = true;
        while (isRunning) {
            Format.header(getTitle());
            printSummary();
            List<Option> options = getOptions();
            printOptions(options);
            int choice = InputHelper.readMenuChoice(0, options.size());
            if (choice == 0) {
                isRunning = !onExit();
            } else {
                runOption(options.get(choice - 1));
            }
        }
    }

    private void printOptions(List<Option> options) {
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("%2d. %s%n", i + 1, options.get(i).label);
        }
        System.out.printf("%2d. %s%n", 0, getExitLabel());
        System.out.println();
    }

    private void runOption(Option option) {
        Format.subHeader(option.label);
        try {
            option.action.run();
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
