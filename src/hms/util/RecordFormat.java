package hms.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a record to one line of a text file and back.
 * Fields are separated by "|". A "|", backslash or new line inside a field is escaped
 * so free text like diagnoses and notes cannot break the file.
 */
public final class RecordFormat {

    public static final String SEPARATOR = "|";

    private RecordFormat() {
    }

    public static String join(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    public static String[] split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (char c : line.toCharArray()) {
            if (escaping) {
                current.append(c == 'n' ? '\n' : c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '|') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n").replace("\r", "");
    }
}
