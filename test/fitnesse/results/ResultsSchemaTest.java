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
      new JSONObject().put("event", "table-done").put("path", "Page").put("block",
        new JSONObject().put("name", "script").put("status", "pass")
          .put("counts", new JSONObject().put("right", 1).put("wrong", 0).put("ignored", 0).put("exceptions", 0))
          .put("rows", new JSONArray().put(new JSONObject().put("cells", new JSONArray().put(
            new JSONObject().put("row", 1).put("column", 1).put("status", "pass")))))),
      new JSONObject().put("event", "page-done").put("path", "Page")
        .put("counts", RunDocument.countsJson(new fitnesse.testsystems.TestSummary(1, 0, 0, 0)))
        .put("durationMillis", 3).put("status", "pass")
    ), "id", "Suite", "2026-11-14T00:00:00Z", 3);

    assertEquals(schema.getJSONObject("properties").getJSONObject("schemaVersion").getInt("const"),
      document.getInt("schemaVersion"));
    require(schema.getJSONObject("$defs").getJSONObject("run"), document.getJSONObject("run"));
    SchemaCheck.pagesThatAssertHaveBlocks(document.getJSONObject("run"));
  }

  @Test(expected = IllegalStateException.class)
  public void aPageWithAssertionsAndNoBlocksIsRejected() throws Exception {
    SchemaCheck.main(new String[]{"test/fitnesse/results/assertions-without-blocks.json"});
  }

  @Test
  public void matchingCellsOfEveryAssertionStatusAreAccepted() {
    JSONObject cell = new JSONObject().put("row", 1).put("column", 1);
    JSONObject pass = new JSONObject(cell.toString()).put("status", "pass");
    JSONObject fail = new JSONObject(cell.toString()).put("status", "fail");
    JSONObject ignore = new JSONObject(cell.toString()).put("status", "ignore");
    JSONObject error = new JSONObject(cell.toString()).put("status", "error");
    JSONObject compared = new JSONObject().put("name", "script").put("status", "fail")
      .put("counts", new JSONObject().put("right", 1).put("wrong", 1).put("ignored", 1).put("exceptions", 1))
      .put("rows", new JSONArray().put(new JSONObject().put("cells", new JSONArray().put(pass).put(fail).put(ignore).put(error))));
    JSONObject broken = new JSONObject().put("name", "Missing").put("status", "table-failure")
      .put("counts", new JSONObject().put("right", 0).put("wrong", 0).put("ignored", 0).put("exceptions", 0))
      .put("rows", new JSONArray());
    JSONObject page = new JSONObject().put("path", "Demo").put("status", "fail").put("durationMillis", 1)
      .put("counts", PageCounts.counts(1, 1, 1, 1, 1))
      .put("blocks", new JSONArray().put(compared).put(broken));
    JSONObject run = new JSONObject().put("pages", new JSONArray().put(page)).put("counts", PageCounts.counts(1, 1, 1, 1, 1));
    SchemaCheck.countsMatchCells(run);
  }

  @Test(expected = IllegalStateException.class)
  public void cellTotalsThatDoNotMatchPageCountsAreRejected() throws Exception {
    SchemaCheck.main(new String[]{"test/fitnesse/results/counts-do-not-match-cells.json"});
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
