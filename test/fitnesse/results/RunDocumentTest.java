package fitnesse.results;

import fitnesse.testsystems.TestSummary;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunDocumentTest {

  @Test
  public void theDocumentIsTheReductionOfTheEvents() throws Exception {
    List<JSONObject> events = List.of(
      new JSONObject().put("event", "page-started").put("path", "Suite.One"),
      new JSONObject().put("event", "table-done").put("path", "Suite.One").put("block",
        new JSONObject().put("name", "script").put("rows", new JSONArray())),
      new JSONObject().put("event", "page-done").put("path", "Suite.One")
        .put("counts", RunDocument.countsJson(new TestSummary(1, 0, 0, 0)))
        .put("durationMillis", 5).put("status", "pass"),
      new JSONObject().put("event", "page-started").put("path", "Suite.Empty"),
      new JSONObject().put("event", "page-done").put("path", "Suite.Empty")
        .put("counts", RunDocument.countsJson(new TestSummary()))
        .put("durationMillis", 1).put("status", "empty"),
      new JSONObject().put("event", "server").put("server", new JSONObject()
        .put("name", "slim").put("command", "java").put("exitCode", 0)
        .put("stdout", "").put("stderr", "").put("exceptions", new JSONArray())
        .put("launchFailure", JSONObject.NULL))
    );

    JSONObject document = RunDocument.reduce(events, "run-1", "2026-11-14T00:00:00Z", 12);
    String golden = Files.readString(Path.of("test/fitnesse/results/golden-run.json"));
    assertJsonEquals(golden, document.toString());
    assertEquals(1, document.getInt("schemaVersion"));
    assertEquals("?responder=stoptest&id=run-1", document.getJSONObject("run").getString("stoptest"));
    assertEquals(List.of("Suite.Empty"), RunDocument.failedPages(document));
    assertEquals("pass", document.getJSONObject("run").getJSONArray("pages").getJSONObject(0).getString("status"));
    assertTrue(document.getJSONObject("run").getJSONArray("servers").getJSONObject(0).getString("command").contains("java"));
  }

  @Test
  public void junitXmlMarksAnEmptyPageAsAFailure() {
    JSONObject document = RunDocument.reduce(List.of(
      new JSONObject().put("event", "page-done").put("path", "Demo")
        .put("counts", RunDocument.countsJson(new TestSummary()))
        .put("durationMillis", 0).put("status", "empty")
    ), "id", "start", 0);
    String xml = JunitXmlWriter.xml(document);
    assertTrue(xml.contains("failures=\"1\""));
    assertTrue(xml.contains("<failure message=\"empty\">"));
  }
}
