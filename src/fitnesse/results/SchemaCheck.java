package fitnesse.results;

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
    System.out.println("results match schema version " + document.getInt("schemaVersion"));
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
