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
 * Typing "back" at any prompt cancels the current operation. Empty input is never accepted.
 */
public final class InputHelper {

    public static final String CANCEL_WORD = "back";
    /** Typed on update forms to keep the current value. */
    public static final String KEEP_WORD = "keep";
    /** Typed for optional details the person does not have, e.g. no middle name. */
    public static final String NONE_WORD = "none";

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

    /** Reads a line that is not blank. "back" cancels the current operation. */
    private static String readNonBlank(String prompt) {
        String input = readLine(prompt);
        boolean isBlank = input.trim().isEmpty();
        while (isBlank) {
            Format.error("This field cannot be empty.");
            input = readLine(prompt);
            isBlank = input.trim().isEmpty();
        }
        if (input.trim().equalsIgnoreCase(CANCEL_WORD)) {
            throw new OperationCancelledException();
        }
        return input;
    }

    /** Generic loop: shows the error message and asks again until the parser accepts the input. */
    public static <T> T read(String prompt, Parser<T> parser) {
        T result = null;
        boolean isValid = false;
        while (!isValid) {
            String input = readNonBlank(prompt);
            try {
                result = parser.parse(input);
                isValid = true;
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
            } catch (NumberFormatException e) {
                Format.error("Please enter a valid number.");
            } catch (DateTimeParseException e) {
                Format.error("Please enter a valid date/time.");
            }
        }
        return result;
    }

    /**
     * Same as read() but typing "keep" returns null. Used when updating records, where "keep"
     * leaves the current value unchanged. Empty input is not accepted.
     */
    public static <T> T readOptional(String prompt, Parser<T> parser) {
        return read(prompt, s -> isKeep(s) ? null : parser.parse(s));
    }

    public static boolean isKeep(String input) {
        return input.trim().equalsIgnoreCase(KEEP_WORD);
    }

    public static boolean isNone(String input) {
        return input.trim().equalsIgnoreCase(NONE_WORD);
    }

    public static int readMenuChoice(int min, int max) {
        int choice = 0;
        boolean isValidChoice = false;
        while (!isValidChoice) {
            String input = readLine("Enter your choice: ").trim();
            try {
                choice = Validator.validateRange(Validator.parseInt(input, "Choice"), min, max, "Choice");
                isValidChoice = true;
            } catch (InvalidInputException e) {
                Format.error(input.isEmpty() ? "Please enter a choice." : e.getMessage());
            }
        }
        return choice;
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
        boolean answer = false;
        boolean isAnswered = false;
        while (!isAnswered) {
            String input = readLine(prompt + " (Y/N): ");
            try {
                answer = Validator.parseYesNo(input);
                isAnswered = true;
            } catch (InvalidInputException e) {
                Format.error(e.getMessage());
            }
        }
        return answer;
    }

    /** Confirmation for delete/cancel actions. */
    public static boolean confirm(String action) {
        return readYesNo(action + " Are you sure?");
    }

    /**
     * Reads a password. In a real terminal nothing is shown while typing, so the prompt says so;
     * otherwise people think the program stopped accepting input.
     */
    public static String readPassword(String prompt) {
        String password = readPasswordOnce(prompt);
        boolean isEmpty = password.isEmpty();
        while (isEmpty) {
            Format.error("Password cannot be empty.");
            password = readPasswordOnce(prompt);
            isEmpty = password.isEmpty();
        }
        if (password.trim().equalsIgnoreCase(CANCEL_WORD)) {
            throw new OperationCancelledException();
        }
        return password;
    }

    private static String readPasswordOnce(String prompt) {
        Console console = System.console();
        if (console == null) {
            return readLine(prompt);
        }
        String label = prompt.trim();
        if (label.endsWith(":")) {
            label = label.substring(0, label.length() - 1);
        }
        char[] chars = console.readPassword("%s", label + " (hidden as you type): ");
        if (chars == null) {
            System.exit(0);
        }
        return new String(chars);
    }
}
