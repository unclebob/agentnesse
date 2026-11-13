package fitnesse.testsystems.slim;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads rendered table cells without an HTML parser. */
public final class HtmlCells {
  private HtmlCells() {
  }

  public static int tables(String html) {
    return split(html, "(?i)<table").length - 1;
  }

  public static int columns(String html, int table, int row) {
    String tableHtml = split(html, "(?i)<table")[table + 1];
    String rowHtml = split(tableHtml, "(?i)<tr")[row + 1];
    return split(rowHtml, "(?i)<td").length - 1;
  }

  public static String raw(String html, int table, int row, int column) {
    String tableHtml = split(html, "(?i)<table")[table + 1];
    String rowHtml = split(tableHtml, "(?i)<tr")[row + 1];
    String cellHtml = split(rowHtml, "(?i)<td")[column + 1];
    int end = cellHtml.toLowerCase().indexOf("</td");
    if (end >= 0) {
      cellHtml = cellHtml.substring(0, end);
    }
    if (cellHtml.startsWith(">")) {
      cellHtml = cellHtml.substring(1);
    }
    return cellHtml;
  }

  public static String cell(String html, int table, int row, int column) {
    String cellHtml = raw(html, table, row, column);
    Matcher slim = Pattern.compile("data-slim=\"([^\"]*)\"").matcher(cellHtml);
    if (slim.find()) {
      return unescape(slim.group(1));
    }
    return unescape(cellHtml.replaceAll("<[^>]+>", "")).trim();
  }

  public static boolean contains(String html, String text) {
    String[] tables = split(html, "(?i)<table");
    for (int table = 1; table < tables.length; table++) {
      String[] rows = split(tables[table], "(?i)<tr");
      for (int row = 1; row < rows.length; row++) {
        String[] cells = split(rows[row], "(?i)<td");
        for (int column = 1; column < cells.length; column++) {
          if (text.equals(cell(html, table - 1, row - 1, column - 1).trim())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static String[] split(String html, String tag) {
    return html.split(tag, -1);
  }

  private static String unescape(String value) {
    return value.replace("&#10;", "\n").replace("&quot;", "\"").replace("&amp;", "&");
  }
}
