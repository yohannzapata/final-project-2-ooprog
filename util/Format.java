package util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Helpers for printing values and headers in a consistent way.
 */
public final class Format {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private Format() {
    }

    public static String money(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return "PHP " + df.format(amount == null ? BigDecimal.ZERO : amount);
    }

    public static String date(LocalDate date) {
        return date == null ? "-" : date.format(DISPLAY_DATE);
    }

    public static String time(LocalTime time) {
        return time == null ? "-" : time.format(DISPLAY_TIME);
    }

    public static String day(DayOfWeek day) {
        return day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    public static String days(Set<DayOfWeek> days) {
        StringJoiner joiner = new StringJoiner(", ");
        for (DayOfWeek day : days) {
            joiner.add(day(day));
        }
        return joiner.toString();
    }

    public static String orDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    /** Cuts long text so table columns stay aligned. */
    public static String fit(String value, int width) {
        String text = orDash(value);
        return text.length() <= width ? text : text.substring(0, width - 3) + "...";
    }

    public static void header(String title) {
        String line = "=".repeat(Math.max(40, title.length() + 8));
        System.out.println();
        System.out.println(line);
        int pad = (line.length() - title.length()) / 2;
        System.out.println(" ".repeat(Math.max(0, pad)) + title);
        System.out.println(line);
    }

    public static void subHeader(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    public static void line(int width) {
        System.out.println("-".repeat(width));
    }

    public static void success(String message) {
        System.out.println("[OK] " + message);
    }

    public static void error(String message) {
        System.out.println("[!] " + message);
    }

    public static void info(String message) {
        System.out.println("[i] " + message);
    }
}
