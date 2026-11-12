package fitnesse.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * A pipe table whose first row is shorter than a later row still needs a
 * separator as wide as the widest row, or the extra cells are discarded.
 */
public final class TableSeparators {
  private TableSeparators() {
  }

  public static String supply(String markdown) {
    StringBuilder out = new StringBuilder();
    List<String> table = new ArrayList<>();
    boolean fence = false;
    for (String line : markdown.split("\n", -1)) {
      if (line.trim().startsWith("```")) {
        flush(table, out);
        fence = !fence;
        out.append(line).append('\n');
        continue;
      }
      if (!fence && isRow(line)) {
        table.add(row(line));
        continue;
      }
      flush(table, out);
      out.append(line).append('\n');
    }
    flush(table, out);
    return out.toString();
  }

  private static boolean isRow(String line) {
    String trimmed = line.trim();
    return trimmed.startsWith("|") || trimmed.startsWith("!|");
  }

  private static String row(String line) {
    String trimmed = line.trim();
    if (trimmed.startsWith("!|")) {
      int bang = line.indexOf('!');
      return line.substring(0, bang) + line.substring(bang + 1);
    }
    return line;
  }

  private static void flush(List<String> table, StringBuilder out) {
    if (table.isEmpty()) {
      return;
    }
    separateFromParagraph(out);
    int columns = 1;
    for (String line : table) {
      if (!separator(line)) {
        columns = Math.max(columns, columns(line));
      }
    }
    boolean wroteHead = false;
    for (String line : table) {
      if (separator(line)) {
        if (columns(line) >= columns) {
          out.append(line).append('\n');
        } else {
          out.append(separator(columns)).append('\n');
        }
        continue;
      }
      out.append(pad(line, columns)).append('\n');
      if (!wroteHead) {
        wroteHead = true;
        if (table.stream().noneMatch(TableSeparators::separator)) {
          out.append(separator(columns)).append('\n');
        }
      }
    }
    table.clear();
  }

  private static String separator(int columns) {
    StringBuilder line = new StringBuilder("|");
    for (int i = 0; i < columns; i++) {
      line.append(" --- |");
    }
    return line.toString();
  }

  private static void separateFromParagraph(StringBuilder out) {
    if (out.length() == 0 || out.toString().endsWith("\n\n")) {
      return;
    }
    if (!out.toString().endsWith("\n")) {
      out.append('\n');
    }
    out.append('\n');
  }

  private static boolean separator(String line) {
    return line.trim().matches("\\|(\\s*-+\\s*\\|)+\\s*");
  }

  private static String pad(String line, int columns) {
    int have = columns(line);
    if (have >= columns) {
      return line;
    }
    StringBuilder padded = new StringBuilder(line.stripTrailing());
    if (padded.length() > 0 && padded.charAt(padded.length() - 1) != '|') {
      padded.append('|');
    }
    for (int i = have; i < columns; i++) {
      padded.append(" |");
    }
    return padded.toString();
  }

  private static int columns(String line) {
    return Math.max(1, line.trim().split("\\|", -1).length - 2);
  }
}
