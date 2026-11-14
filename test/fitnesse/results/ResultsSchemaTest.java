package fitnesse.results;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks a results document against the checked-in schema's required shape. */
public class ResultsSchemaTest {

  @Test
  public void aReducedDocumentMatchesTheCheckedInSchema() throws Exception {
    JSONObject schema;
    try (InputStream in = RunDocument.class.getResourceAsStream("results.schema.json")) {
      assertNotNull(in);
      schema = new JSONObject(new JSONTokener(in));
    }
    JSONObject document = RunDocument.reduce(java.util.List.of(
      new JSONObject().put("event", "page-done").put("path", "Page")
        .put("counts", RunDocument.countsJson(new fitnesse.testsystems.TestSummary(1, 0, 0, 0)))
        .put("durationMillis", 3).put("status", "pass")
    ), "id", "2026-11-14T00:00:00Z", 3);

    assertEquals(schema.getJSONObject("properties").getJSONObject("schemaVersion").getInt("const"),
      document.getInt("schemaVersion"));
    require(schema.getJSONObject("$defs").getJSONObject("run"), document.getJSONObject("run"));
    assertTrue(document.getJSONObject("run").getJSONArray("pages").getJSONObject(0).getJSONArray("blocks").isEmpty());
  }

  @Test
  public void schemaCheckAcceptsAResultsFile() throws Exception {
    SchemaCheck.main(new String[]{"test/fitnesse/results/golden-run.json"});
  }

  @Test(expected = IllegalArgumentException.class)
  public void schemaCheckRequiresOneFile() throws Exception {
    SchemaCheck.main(new String[0]);
  }

  @Test(expected = IllegalStateException.class)
  public void schemaCheckRejectsTheWrongVersion() throws Exception {
    Path file = Files.createTempFile("results", ".json");
    Files.writeString(file, "noise\n{\"schemaVersion\":2,\"run\":{}}");
    SchemaCheck.main(new String[]{file.toString()});
  }

  @Test(expected = IllegalStateException.class)
  public void schemaCheckRejectsARunThatOmitsRequiredFields() throws Exception {
    Path file = Files.createTempFile("results", ".json");
    Files.writeString(file, "{\"schemaVersion\":1,\"run\":{}}");
    SchemaCheck.main(new String[]{file.toString()});
  }

  private static void require(JSONObject definition, JSONObject value) {
    JSONArray required = definition.getJSONArray("required");
    for (int i = 0; i < required.length(); i++) {
      String name = required.getString(i);
      assertTrue(name, value.has(name));
    }
  }
}
