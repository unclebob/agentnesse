package agnesse.results;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Checks a results file against the checked-in schema's required fields. */
public final class SchemaCheck {
  private SchemaCheck() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("usage: SchemaCheck <results.json>");
    }
    String text = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
    int start = text.indexOf('{');
    JSONObject document = new JSONObject(new JSONTokener(text.substring(start)));
    JSONObject schema;
    try (var in = RunDocument.class.getResourceAsStream("results.schema.json")) {
      schema = new JSONObject(new JSONTokener(in));
    }
    if (document.getInt("schemaVersion") != schema.getJSONObject("properties").getJSONObject("schemaVersion").getInt("const")) {
      throw new IllegalStateException("schemaVersion");
    }
    require(schema.getJSONObject("$defs").getJSONObject("run"), document.getJSONObject("run"));
    pagesThatAssertHaveBlocks(document.getJSONObject("run"));
    countsMatchCells(document.getJSONObject("run"));
    System.out.println("results match schema version " + document.getInt("schemaVersion"));
  }

  /** Assertion cells sum to the page counts, and table failures sum to tableFailures. */
  static void countsMatchCells(JSONObject run) {
    JSONArray pages = run.getJSONArray("pages");
    int[] total = new int[5];
    for (int i = 0; i < pages.length(); i++) {
      JSONObject page = pages.getJSONObject(i);
      int[] found = tally(page.getJSONArray("blocks"));
      JSONObject counts = page.getJSONObject("counts");
      same(page.getString("path"), counts, found);
      if (found[4] > 0 && "pass".equals(page.getString("status"))) {
        throw new IllegalStateException(page.getString("path") + " has a table failure but status pass");
      }
      add(total, found);
    }
    same("run", run.getJSONObject("counts"), total);
  }

  private static int[] tally(JSONArray blocks) {
    int[] found = new int[5];
    for (int i = 0; i < blocks.length(); i++) {
      JSONObject block = blocks.getJSONObject(i);
      if ("table-failure".equals(block.optString("status", ""))) {
        found[4]++;
      }
      JSONArray rows = block.getJSONArray("rows");
      for (int row = 0; row < rows.length(); row++) {
        JSONArray cells = rows.getJSONObject(row).getJSONArray("cells");
        for (int column = 0; column < cells.length(); column++) {
          tallyCell(found, cells.getJSONObject(column).optString("status", ""));
        }
      }
    }
    return found;
  }

  private static void tallyCell(int[] found, String status) {
    if ("pass".equals(status)) found[0]++;
    else if ("fail".equals(status)) found[1]++;
    else if ("ignore".equals(status)) found[2]++;
    else if ("error".equals(status)) found[3]++;
  }

  private static void same(String where, JSONObject counts, int[] found) {
    String[] names = {"right", "wrong", "ignored", "exceptions", "tableFailures"};
    for (int i = 0; i < names.length; i++) {
      if (counts.getInt(names[i]) != found[i]) {
        throw new IllegalStateException(where + " " + names[i] + " is " + counts.getInt(names[i]) + " but cells sum to " + found[i]);
      }
    }
  }

  private static void add(int[] total, int[] found) {
    for (int i = 0; i < total.length; i++) {
      total[i] += found[i];
    }
  }

  static void pagesThatAssertHaveBlocks(JSONObject run) {
    JSONArray pages = run.getJSONArray("pages");
    for (int i = 0; i < pages.length(); i++) {
      JSONObject page = pages.getJSONObject(i);
      if (asserted(page.getJSONObject("counts")) && page.getJSONArray("blocks").isEmpty()) {
        throw new IllegalStateException(page.getString("path") + " recorded assertions but no blocks");
      }
    }
  }

  private static boolean asserted(JSONObject counts) {
    return counts.getInt("right") + counts.getInt("wrong") + counts.getInt("ignored") + counts.getInt("exceptions") > 0;
  }

  static void require(JSONObject definition, JSONObject value) {
    JSONArray required = definition.getJSONArray("required");
    for (int i = 0; i < required.length(); i++) {
      String name = required.getString(i);
      if (!value.has(name)) {
        throw new IllegalStateException("missing " + name);
      }
    }
  }
}
