package agnesse.testsystems.slim;

import agnesse.html.HtmlUtil;
import agnesse.wiki.TestAdjunct;
import org.json.JSONArray;
import org.json.JSONObject;

/** Collapsed sections in a test result. Open when a table in the section failed. */
public final class ResultSections {
  private ResultSections() {
  }

  public static String details(String title, String body, boolean open) {
    return "<details class=\"agnesse-include\"" + (open ? " open" : "") + "><summary>"
      + HtmlUtil.escapeHTML(title) + "</summary>" + body + "</details>\n";
  }

  /** True when a block or cell failed, errored, or recorded an exception. */
  public static boolean shouldOpen(JSONArray blocks) {
    if (blocks == null) {
      return false;
    }
    for (int i = 0; i < blocks.length(); i++) {
      if (trouble(blocks.getJSONObject(i))) {
        return true;
      }
    }
    return false;
  }

  public static String section(String origin, String testPage, String firstCell) {
    if (structural(origin)) {
      return origin;
    }
    if (firstCell != null && firstCell.trim().equalsIgnoreCase("import")) {
      return "import";
    }
    if (origin != null && !origin.isEmpty() && !origin.equals(testPage)) {
      return origin;
    }
    return "";
  }

  private static boolean trouble(JSONObject block) {
    String status = block.optString("status", "");
    if ("fail".equals(status) || "error".equals(status) || "table-failure".equals(status)) {
      return true;
    }
    JSONArray rows = block.optJSONArray("rows");
    if (rows == null) {
      return false;
    }
    for (int row = 0; row < rows.length(); row++) {
      if (rowTrouble(rows.getJSONObject(row))) {
        return true;
      }
    }
    return false;
  }

  private static boolean rowTrouble(JSONObject row) {
    JSONArray cells = row.optJSONArray("cells");
    if (cells == null) {
      return false;
    }
    for (int column = 0; column < cells.length(); column++) {
      JSONObject cell = cells.getJSONObject(column);
      String status = cell.optString("status", "");
      if ("fail".equals(status) || "error".equals(status)) {
        return true;
      }
      if (cell.has("error") && !cell.isNull("error")) {
        return true;
      }
    }
    return false;
  }

  private static boolean structural(String name) {
    return TestAdjunct.named(name);
  }
}
