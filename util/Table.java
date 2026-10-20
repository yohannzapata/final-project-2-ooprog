package util;

import java.util.ArrayList;
import java.util.List;

/**
 * Prints records as a bordered table, so every list and detail screen looks the same:
 * <pre>
 * +---------+----------------+
 * | ID      | Name           |
 * +---------+----------------+
 * | P0001   | Juan Dela Cruz |
 * +---------+----------------+
 * </pre>
 * Columns are as wide as their longest value, up to a maximum, so values in lists are only
 * shortened with "..." when they are very long; on detail screens long values wrap instead. Plain + - | characters are used because box-drawing
 * characters do not show correctly in every Windows terminal.
 */
public final class Table {

    /** Longest value shown in a column before it is shortened. */
    public static final int DEFAULT_MAX_WIDTH = 30;
    /** Values on detail screens (addresses, schedules, notes) get more room. */
    public static final int DETAIL_MAX_WIDTH = 60;

    private final String title;
    private final String[] headers;
    private final int columnCount;
    private final boolean[] rightAligned;
    private final int[] maxWidths;
    // A null entry draws a line between groups of rows.
    private final List<String[]> rows = new ArrayList<>();
    // Detail screens continue long values (addresses, notes) on the next lines instead of cutting them off.
    private final boolean isWrapping;

    /** A list table with a header row. */
    public Table(String... headers) {
        this(null, headers, headers.length);
    }

    private Table(String title, String[] headers, int columnCount) {
        this.title = title;
        this.isWrapping = headers == null;
        this.headers = headers;
        this.columnCount = columnCount;
        this.rightAligned = new boolean[columnCount];
        this.maxWidths = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            maxWidths[i] = DEFAULT_MAX_WIDTH;
        }
    }

    /** A two-column "label | value" table under a title, for the details of one record. */
    public static Table details(String title) {
        Table table = new Table(title, null, 2);
        table.maxWidths[1] = DETAIL_MAX_WIDTH;
        return table;
    }

    /** Right-aligns a column, e.g. amounts, so the digits line up. */
    public Table alignRight(int column) {
        rightAligned[column] = true;
        return this;
    }

    public Table maxWidth(int column, int width) {
        maxWidths[column] = width;
        return this;
    }

    /** Adds one row; empty or missing values are shown as "-". */
    public Table addRow(Object... values) {
        String[] row = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            Object value = i < values.length ? values[i] : null;
            row[i] = value == null || value.toString().isEmpty() ? "-" : value.toString();
        }
        if (isWrapping && row[1].length() > maxWidths[1]) {
            addWrapped(row[0], row[1]);
        } else {
            rows.add(row);
        }
        return this;
    }

    private void addWrapped(String label, String value) {
        List<String> lines = wrap(value, maxWidths[1]);
        for (int i = 0; i < lines.size(); i++) {
            rows.add(new String[]{i == 0 ? label : "", lines.get(i)});
        }
    }

    /** Splits text into lines of at most the given width, breaking between words. */
    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        lines.add(line.toString());
        return lines;
    }

    /** Draws a line between groups of rows, e.g. before the total of a bill. */
    public Table addSeparator() {
        rows.add(null);
        return this;
    }

    public void print() {
        int[] widths = new int[columnCount];
        if (headers != null) {
            fitWidths(widths, headers);
        }
        for (String[] row : rows) {
            if (row != null) {
                fitWidths(widths, row);
            }
        }
        String border = border(widths);
        // A title longer than the columns widens the last column so the title fits on one line.
        if (title != null && title.length() > border.length() - 4) {
            widths[columnCount - 1] += title.length() - (border.length() - 4);
            border = border(widths);
        }
        if (title != null) {
            System.out.println("+" + "-".repeat(border.length() - 2) + "+");
            System.out.println("| " + pad(title, border.length() - 4, false) + " |");
        }
        System.out.println(border);
        if (headers != null) {
            printRow(headers, widths);
            System.out.println(border);
        }
        for (String[] row : rows) {
            if (row == null) {
                System.out.println(border);
            } else {
                printRow(row, widths);
            }
        }
        System.out.println(border);
    }

    private void fitWidths(int[] widths, String[] cells) {
        for (int i = 0; i < columnCount; i++) {
            widths[i] = Math.max(widths[i], Math.min(cells[i].length(), maxWidths[i]));
        }
    }

    private static String border(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int width : widths) {
            sb.append("-".repeat(width + 2)).append('+');
        }
        return sb.toString();
    }

    private void printRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < columnCount; i++) {
            sb.append(' ').append(pad(shorten(cells[i], widths[i]), widths[i], rightAligned[i])).append(" |");
        }
        System.out.println(sb);
    }

    private static String shorten(String value, int width) {
        return value.length() <= width ? value : value.substring(0, width - 3) + "...";
    }

    private static String pad(String value, int width, boolean right) {
        String spaces = " ".repeat(Math.max(0, width - value.length()));
        return right ? spaces + value : value + spaces;
    }
}
