package util;

import exceptions.InvalidInputException;
import exceptions.OperationCancelledException;

import java.io.Console;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Reads input from the console. Every read method keeps asking until the input is valid,
 * so the rest of the program never has to deal with bad input.
 * Typing "back" at any prompt cancels the current operation.
 */
public final class InputHelper {

    public static final String CANCEL_WORD = "back";

    private static final Scanner SCANNER = new Scanner(System.in);

    /** A parsing/validation step that may reject the input. */
    @FunctionalInterface
    public interface Parser<T> {
        T parse(String input) throws InvalidInputException;
    }

    private InputHelper() {
    }

    /** Reads one raw line. Exits cleanly if the input stream is closed (e.g. Ctrl+Z / Ctrl+D). */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return SCANNER.nextLine();
        } catch (NoSuchElementException | IllegalStateException e) {
            System.out.println();
            System.out.println("Input closed. Exiting the program.");
            System.exit(0);
            return "";
        }
    }

    private static String readCancellable(String prompt) {
        String input = readLine(prompt);
        if (input.trim().equalsIgnoreCase(CANCEL_WORD)) {
            throw new OperationCancelledException();
        }
        return input;
    }

    /** Generic loop: shows the error message and asks again until the parser accepts the input. */
    public static <T> T read(String prompt, Parser<T> parser) {
        while (true) {
            String input = readCancellable(prompt);
            try {
                return parser.parse(input);
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
            } catch (NumberFormatException e) {
                Format.error("Please enter a valid number.");
            } catch (DateTimeParseException e) {
                Format.error("Please enter a valid date/time.");
            }
        }
    }

    /**
     * Same as read() but an empty input returns null. Used for "press Enter to keep current value"
     * prompts when updating records.
     */
    public static <T> T readOptional(String prompt, Parser<T> parser) {
        while (true) {
            String input = readCancellable(prompt);
            if (input.trim().isEmpty()) {
                return null;
            }
            try {
                return parser.parse(input);
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
            } catch (NumberFormatException e) {
                Format.error("Please enter a valid number.");
            } catch (DateTimeParseException e) {
                Format.error("Please enter a valid date/time.");
            }
        }
    }

    public static int readMenuChoice(int min, int max) {
        while (true) {
            String input = readLine("Enter your choice: ").trim();
            try {
                int choice = Integer.parseInt(input);
                return Validator.validateRange(choice, min, max, "Choice");
            } catch (NumberFormatException e) {
                Format.error(input.isEmpty() ? "Please enter a choice." : "Choice must be a number from " + min + " to " + max + ".");
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
            }
        }
    }

    public static int readInt(String prompt, int min, int max, String field) {
        return read(prompt, s -> Validator.validateRange(Validator.parseInt(s, field), min, max, field));
    }

    public static String readRequired(String prompt, String field) {
        return read(prompt, s -> Validator.requireNonEmpty(s, field));
    }

    public static String readText(String prompt, String field, int maxLength, boolean required) {
        return read(prompt, s -> Validator.validateText(s, field, maxLength, required));
    }

    public static LocalDate readDate(String prompt, String field) {
        return read(prompt, s -> Validator.parseDate(s, field));
    }

    public static LocalTime readTime(String prompt, String field) {
        return read(prompt, s -> Validator.parseTime(s, field));
    }

    public static BigDecimal readAmount(String prompt, String field, BigDecimal max) {
        return read(prompt, s -> Validator.validateAmount(s, field, max));
    }

    /** Reads an ID and converts it to upper case so "p0001" and "P0001" both work. */
    public static String readId(String prompt) {
        return read(prompt, s -> Validator.requireNonEmpty(s, "ID").toUpperCase());
    }

    public static boolean readYesNo(String prompt) {
        while (true) {
            String input = readLine(prompt + " (Y/N): ");
            try {
                return Validator.parseYesNo(input);
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
            }
        }
    }

    /** Confirmation for delete/cancel actions. */
    public static boolean confirm(String action) {
        return readYesNo(action + " Are you sure?");
    }

    /** Reads a password without echoing it when a real console is available. */
    public static String readPassword(String prompt) {
        Console console = System.console();
        String password;
        if (console != null) {
            char[] chars = console.readPassword(prompt);
            if (chars == null) {
                System.exit(0);
            }
            password = new String(chars);
        } else {
            password = readLine(prompt);
        }
        if (password.trim().equalsIgnoreCase(CANCEL_WORD)) {
            throw new OperationCancelledException();
        }
        return password;
    }
}
