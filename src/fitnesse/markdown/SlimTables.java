package fitnesse.markdown;

/**
 * Which GFM tables are Slim. A table is Slim only on a test page, and a
 * comment table on a test page is not Slim.
 */
public final class SlimTables {
  private SlimTables() {
  }

  public static boolean slim(boolean testPage, String firstCell) {
    return testPage && !SlimTableHeading.comment(firstCell);
  }

  /** Drops Slim tables so the words inside them are not page links. */
  public static String withoutSlimTables(String content) {
    StringBuilder out = new StringBuilder();
    boolean fence = false;
    boolean inTable = false;
    boolean slim = false;
    for (String line : (content == null ? "" : content).split("\n", -1)) {
      if (line.trim().startsWith("```")) {
        fence = !fence;
        inTable = false;
        out.append(line).append('\n');
        continue;
      }
      if (fence) {
        out.append(line).append('\n');
        continue;
      }
      if (TableSeparators.isRow(line)) {
        if (!inTable) {
          inTable = true;
          slim = slim(true, firstCell(line));
        }
        if (!slim) {
          out.append(line).append('\n');
        }
        continue;
      }
      inTable = false;
      out.append(line).append('\n');
    }
    return out.toString();
  }

  public static String firstCell(String row) {
    String trimmed = row.trim();
    if (trimmed.startsWith("!|")) {
      trimmed = trimmed.substring(1);
    }
    String[] cells = trimmed.split("\\|", -1);
    for (String cell : cells) {
      if (!cell.isBlank()) {
        return cell.trim();
      }
    }
    return "";
  }
}
