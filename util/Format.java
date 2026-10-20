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

    /** Philippine peso sign. Written as an escape so the source compiles the same on every system. */
    public static final String PESO = "\u20B1";

    private Format() {
    }

    public static String money(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return PESO + df.format(amount == null ? BigDecimal.ZERO : amount);
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

    /** Three or more days in a row are shown as a range: "Mon-Fri", "Mon, Wed, Fri", "Mon-Wed, Sat". */
    public static String days(Set<DayOfWeek> days) {
        StringJoiner joiner = new StringJoiner(", ");
        DayOfWeek runStart = null;
        DayOfWeek runEnd = null;
        for (DayOfWeek day : DayOfWeek.values()) {
            if (days.contains(day)) {
                runStart = runStart == null ? day : runStart;
                runEnd = day;
            } else if (runStart != null) {
                joiner.add(dayRun(runStart, runEnd));
                runStart = null;
            }
        }
        if (runStart != null) {
            joiner.add(dayRun(runStart, runEnd));
        }
        return joiner.toString();
    }

    private static String dayRun(DayOfWeek start, DayOfWeek end) {
        int length = end.getValue() - start.getValue() + 1;
        if (length == 1) {
            return day(start);
        }
        return length == 2 ? day(start) + ", " + day(end) : day(start) + "-" + day(end);
    }

    public static String orDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
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
