package fitnesse.results;

import org.json.JSONArray;
import org.json.JSONObject;

/** The expected-versus-actual text a failing page puts in JUnit XML. */
final class FailureText {
  private FailureText() {
  }

  static String of(JSONObject page) {
    StringBuilder text = new StringBuilder();
    JSONArray blocks = page.optJSONArray("blocks");
    if (blocks != null) {
      for (int i = 0; i < blocks.length(); i++) {
        appendBlock(text, blocks.getJSONObject(i));
      }
    }
    if (text.length() == 0 && "empty".equals(page.optString("status"))) {
      return "Page executed no assertions";
    }
    if (text.length() == 0) {
      return page.getJSONObject("counts").toString();
    }
    return text.toString().trim();
  }

  private static void appendBlock(StringBuilder text, JSONObject block) {
    JSONArray rows = block.getJSONArray("rows");
    for (int row = 0; row < rows.length(); row++) {
      appendRow(text, block.optString("name"), rows.getJSONObject(row));
    }
  }

  private static void appendRow(StringBuilder text, String name, JSONObject row) {
    JSONArray cells = row.getJSONArray("cells");
    for (int column = 0; column < cells.length(); column++) {
      appendCell(text, name, cells.getJSONObject(column));
    }
  }

  private static void appendCell(StringBuilder text, String name, JSONObject cell) {
    if (!failed(cell)) {
      return;
    }
    text.append(name).append(place(cell)).append(comparison(cell)).append('\n');
  }

  private static boolean failed(JSONObject cell) {
    String status = cell.optString("status", "");
    return "fail".equals(status) || "error".equals(status);
  }

  private static String place(JSONObject cell) {
    return " row " + cell.optInt("row") + " column " + cell.optInt("column");
  }

  private static String comparison(JSONObject cell) {
    return quoted(cell, "expected") + quoted(cell, "actual") + error(cell);
  }

  private static String quoted(JSONObject cell, String key) {
    if (!cell.has(key) || cell.isNull(key)) {
      return "";
    }
    return " " + key + " [" + cell.getString(key) + "]";
  }

  private static String error(JSONObject cell) {
    JSONObject error = cell.optJSONObject("error");
    if (error == null || error.optString("message").isEmpty()) {
      return "";
    }
    return " " + error.getString("message");
  }
}
