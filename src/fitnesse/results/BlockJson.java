package fitnesse.results;

import fitnesse.testsystems.slim.MarkdownTable;
import fitnesse.testsystems.slim.results.CellRecord;
import fitnesse.testsystems.slim.results.SlimFailure;
import org.json.JSONArray;
import org.json.JSONObject;

/** One Slim table, as the checkpoint-6 cell model, with 1-based coordinates. */
public final class BlockJson {
  private BlockJson() {
  }

  public static JSONObject from(MarkdownTable table) {
    JSONArray rows = new JSONArray();
    for (int row = 0; row < table.getRowCount(); row++) {
      rows.put(rowJson(table, row));
    }
    String name = table.getRowCount() == 0 || table.getColumnCountInRow(0) == 0
      ? "" : table.getCellContents(0, 0);
    return new JSONObject().put("name", name).put("rows", rows);
  }

  private static JSONObject rowJson(MarkdownTable table, int row) {
    JSONArray cells = new JSONArray();
    for (int column = 0; column < table.getColumnCountInRow(row); column++) {
      cells.put(cellJson(table.cellRecord(row, column), row + 1, column + 1));
    }
    return new JSONObject().put("cells", cells);
  }

  static JSONObject cellJson(CellRecord record, int row, int column) {
    JSONObject cell = new JSONObject();
    cell.put("row", row);
    cell.put("column", column);
    cell.put("status", record.status() == null ? JSONObject.NULL : record.status());
    put(cell, "actual", record.actual());
    put(cell, "expected", record.expected());
    put(cell, "symbol", record.symbol());
    put(cell, "value", record.value());
    cell.put("error", record.failure() == null ? JSONObject.NULL : errorJson(record.failure()));
    return cell;
  }

  public static JSONObject errorJson(SlimFailure failure) {
    JSONObject error = new JSONObject();
    error.put("kind", failure.kind().name());
    error.put("type", failure.type() == null ? "" : failure.type());
    error.put("message", failure.message() == null ? "" : failure.message());
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
}
