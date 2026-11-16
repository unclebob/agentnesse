package agnesse.testsystems.slim;

import agnesse.html.HtmlUtil;
import org.json.JSONArray;
import org.json.JSONObject;

/** HTML for one result block, built only from its JSON. */
public final class ResultHtml {
  private ResultHtml() {
  }

  public static String block(JSONObject block) {
    StringBuilder html = new StringBuilder();
    html.append("<table class=\"agentnesse-table");
    String name = block.optString("name", "");
    if (!name.isEmpty()) {
      html.append(' ').append(HtmlUtil.escapeHTML(name));
    }
    html.append("\">\n");
    JSONArray rows = block.optJSONArray("rows");
    if (rows != null) {
      for (int i = 0; i < rows.length(); i++) {
        html.append("<tr>");
        JSONArray cells = rows.getJSONObject(i).optJSONArray("cells");
        if (cells != null) {
          for (int column = 0; column < cells.length(); column++) {
            html.append(cell(cells.getJSONObject(column)));
          }
        }
        html.append("</tr>\n");
      }
    }
    html.append("</table>\n");
    return html.toString();
  }

  private static String cell(JSONObject cell) {
    String status = cell.optString("status", "");
    if (status.isEmpty() && hasError(cell)) {
      status = "error";
    }
    String cls = status.isEmpty() ? "" : " class=\"" + status + "\"";
    return "<td" + cls + ">" + HtmlUtil.escapeHTML(text(cell)) + message(cell) + "</td>";
  }

  private static String text(JSONObject cell) {
    if (!cell.isNull("symbol")) {
      String name = cell.optString("symbol", "");
      String shown = name.startsWith("SECRET_") ? "*****" : plain(cell, "value");
      String arrow = name.startsWith("SECRET_") ? "->" : "<-";
      return "$" + name + arrow + "[" + shown + "]";
    }
    if (!cell.isNull("value")) {
      return cell.optString("value", "");
    }
    String expected = plain(cell, "expected");
    String actual = plain(cell, "actual");
    if (!actual.isEmpty() && !actual.equals(expected)) {
      return actual;
    }
    return expected.isEmpty() ? actual : expected;
  }

  private static String message(JSONObject cell) {
    if (!hasError(cell)) {
      return "";
    }
    String message = cell.getJSONObject("error").optString("message", "");
    if (message.isEmpty()) {
      return "";
    }
    return " <span class=\"error\">" + HtmlUtil.escapeHTML(message) + "</span>";
  }

  private static boolean hasError(JSONObject cell) {
    return cell.has("error") && !cell.isNull("error");
  }

  private static String plain(JSONObject cell, String name) {
    return cell.isNull(name) ? "" : cell.optString(name, "");
  }
}
