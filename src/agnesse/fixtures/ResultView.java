package agnesse.fixtures;

import agnesse.html.HtmlUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

/**
 * The HTML a person sees for the last test response: the tables, the drawn
 * values, the failing cells, and the notes. {@code show} leaves this in the
 * outer page.
 */
public class ResultView {
  private static final Set<String> STATUSES = Set.of("pass", "fail", "error", "ignore", "symbol");

  public String renderedResult() throws Exception {
    String sent = AgNesseFixtureContext.sender == null ? "" : AgNesseFixtureContext.sender.sentData();
    return html(sent);
  }

  static String html(String sent) {
    StringBuilder body = new StringBuilder();
    String text = JsonBody.parse(sent).text();
    if (text != null) {
      write(body, text.trim());
    }
    return "<div class=\"rendered-test\">" + body + "</div>";
  }

  private static void write(StringBuilder html, String text) {
    if (!text.contains("\n") && text.startsWith("{")) {
      writeOne(html, new JSONObject(text));
      return;
    }
    for (String line : text.split("\n")) {
      if (!line.isBlank()) {
        writeOne(html, new JSONObject(line));
      }
    }
  }

  private static void writeOne(StringBuilder html, JSONObject object) {
    if (object.has("run")) {
      writeRun(html, object.getJSONObject("run"));
      return;
    }
    if (!"table-done".equals(object.optString("event")) && !"output".equals(object.optString("event"))) {
      return;
    }
    if ("output".equals(object.optString("event"))) {
      html.append("<p class=\"error\">").append(escape(object.optString("text"))).append("</p>");
      return;
    }
    html.append(table(object.getJSONObject("block")));
  }

  private static void writeRun(StringBuilder html, JSONObject run) {
    JSONArray pages = run.optJSONArray("pages");
    if (pages == null) {
      return;
    }
    for (int page = 0; page < pages.length(); page++) {
      JSONArray blocks = pages.getJSONObject(page).optJSONArray("blocks");
      if (blocks == null) {
        continue;
      }
      for (int block = 0; block < blocks.length(); block++) {
        html.append(table(blocks.getJSONObject(block)));
      }
    }
  }

  private static String table(JSONObject block) {
    StringBuilder html = new StringBuilder("<table class=\"agnesse-table\">");
    JSONArray rows = block.optJSONArray("rows");
    if (rows != null) {
      for (int row = 0; row < rows.length(); row++) {
        html.append("<tr>");
        JSONArray cells = rows.getJSONObject(row).optJSONArray("cells");
        if (cells != null) {
          for (int column = 0; column < cells.length(); column++) {
            html.append(cell(cells.getJSONObject(column)));
          }
        }
        html.append("</tr>");
      }
    }
    return html.append("</table>").toString();
  }

  private static String cell(JSONObject cell) {
    return "<td" + status(cell) + ">" + escape(cellText(cell)) + "</td>";
  }

  private static String status(JSONObject cell) {
    String status = text(cell, "status");
    if (status.isEmpty() && hasError(cell)) {
      status = "error";
    }
    if (!STATUSES.contains(status)) {
      return "";
    }
    return " class=\"" + status + "\"";
  }

  private static String cellText(JSONObject cell) {
    String body = body(cell);
    String message = errorMessage(cell);
    if (message.isEmpty()) {
      return body;
    }
    if (body.isEmpty()) {
      return message;
    }
    return body + " " + message;
  }

  private static String body(JSONObject cell) {
    String symbol = text(cell, "symbol");
    if (!symbol.isEmpty()) {
      return symbolText(symbol, text(cell, "value"));
    }
    String value = text(cell, "value");
    if (!value.isEmpty()) {
      return value;
    }
    String actual = text(cell, "actual");
    if (!actual.isEmpty()) {
      return actual;
    }
    return text(cell, "expected");
  }

  private static boolean hasError(JSONObject cell) {
    return !cell.isNull("error") && cell.get("error") instanceof JSONObject;
  }

  private static String errorMessage(JSONObject cell) {
    if (!hasError(cell)) {
      return "";
    }
    return text(cell.getJSONObject("error"), "message");
  }

  private static String symbolText(String symbol, String value) {
    boolean secret = symbol.startsWith("SECRET_");
    String shown = secret ? "*****" : value;
    String arrow = secret ? "->" : "<-";
    return "$" + symbol + arrow + "[" + shown + "]";
  }

  private static String text(JSONObject cell, String key) {
    if (cell.isNull(key)) {
      return "";
    }
    return String.valueOf(cell.get(key));
  }

  private static String escape(String text) {
    return HtmlUtil.escapeHTML(text);
  }
}
