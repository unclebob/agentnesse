package agnesse.results;

import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.results.CellRecord;
import agnesse.testsystems.slim.results.SlimFailure;
import org.json.JSONArray;
import org.json.JSONObject;

/** One Slim table, as the checkpoint-6 cell model, with 1-based coordinates. */
public final class BlockJson {
  private BlockJson() {
  }

  public static JSONObject from(MarkdownTable table) {
    JSONArray rows = new JSONArray();
    Outcome outcome = new Outcome();
    for (int row = 0; row < table.getRowCount(); row++) {
      rows.put(rowJson(table, row, outcome));
    }
    return new JSONObject()
      .put("name", nameOf(table))
      .put("status", outcome.status())
      .put("counts", outcome.counts())
      .put("rows", rows);
  }

  private static String nameOf(MarkdownTable table) {
    if (table.getRowCount() == 0 || table.getColumnCountInRow(0) == 0) {
      return "";
    }
    return table.originalCell(0, 0);
  }

  private static JSONObject rowJson(MarkdownTable table, int row, Outcome outcome) {
    JSONArray cells = new JSONArray();
    for (int column = 0; column < table.getColumnCountInRow(row); column++) {
      CellRecord record = table.cellRecord(row, column);
      outcome.note(record);
      cells.put(cellJson(record, row + 1, column + 1));
    }
    return new JSONObject().put("cells", cells);
  }

  private static Object jsonStatus(CellRecord record) {
    if (record.status() == null) {
      return JSONObject.NULL;
    }
    if ("symbol".equals(record.status())) {
      return "symbol";
    }
    if (!record.assertion() && "pass".equals(record.status())) {
      return JSONObject.NULL;
    }
    return record.status();
  }

  static JSONObject cellJson(CellRecord record, int row, int column) {
    JSONObject cell = new JSONObject();
    cell.put("row", row);
    cell.put("column", column);
    cell.put("status", jsonStatus(record));
    put(cell, "actual", record.actual());
    put(cell, "expected", record.expected());
    put(cell, "symbol", record.symbol());
    String value = record.value();
    if (record.symbol() != null && record.symbol().startsWith("SECRET_")) {
      value = "*****";
    }
    put(cell, "value", value);
    cell.put("error", record.failure() == null ? JSONObject.NULL : errorJson(record.failure()));
    return cell;
  }

  public static JSONObject errorJson(SlimFailure failure) {
    JSONObject error = new JSONObject();
    error.put("kind", failure.kind().name());
    error.put("type", failure.type() == null ? "" : failure.type());
    error.put("message", failure.sentence());
    error.put("stackTrace", failure.stack());
    error.put("page", json(failure.page()));
    error.put("block", json(failure.block()));
    error.put("row", failure.row() == null ? JSONObject.NULL : failure.row());
    error.put("cell", failure.cell() == null ? JSONObject.NULL : failure.cell());
    return error;
  }

  private static void put(JSONObject cell, String name, String value) {
    cell.put(name, value == null ? JSONObject.NULL : value);
  }

  private static Object json(String value) {
    return value == null ? JSONObject.NULL : value;
  }

  /** Assertion totals for one table, plus whether the table itself failed. */
  private static final class Outcome {
    private int right;
    private int wrong;
    private int ignored;
    private int exceptions;
    private boolean trouble;

    private void note(CellRecord record) {
      if (record.assertion()) {
        count(record.status());
      } else if (record.trouble()) {
        trouble = true;
      }
    }

    private void count(String status) {
      if ("pass".equals(status)) right++;
      else if ("fail".equals(status)) wrong++;
      else if ("ignore".equals(status)) ignored++;
      else if ("error".equals(status)) exceptions++;
    }

    private JSONObject counts() {
      return new JSONObject().put("right", right).put("wrong", wrong).put("ignored", ignored).put("exceptions", exceptions);
    }

    private Object status() {
      if (emptyTrouble()) {
        return "table-failure";
      }
      return countedStatus();
    }

    private boolean emptyTrouble() {
      return trouble && right == 0 && wrong == 0 && ignored == 0 && exceptions == 0;
    }

    private Object countedStatus() {
      if (exceptions > 0) return "error";
      if (wrong > 0) return "fail";
      if (right > 0) return "pass";
      if (ignored > 0) return "ignore";
      return JSONObject.NULL;
    }
  }
}
