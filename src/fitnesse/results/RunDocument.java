package fitnesse.results;

import fitnesse.testsystems.ExecutionResult;
import fitnesse.testsystems.TestSummary;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A run document is the reduction of the JSON events the runner emits.
 * Schema version 1 is not frozen; Robert decides that at checkpoint 7.
 */
public final class RunDocument {
  public static final int SCHEMA_VERSION = 1;

  private RunDocument() {
  }

  public static JSONObject reduce(List<JSONObject> events, String id, String start, long durationMillis) {
    Map<String, JSONObject> pages = new LinkedHashMap<>();
    JSONArray servers = new JSONArray();
    for (JSONObject event : events) {
      apply(event, pages, servers);
    }
    JSONArray pageList = new JSONArray();
    JSONArray failed = new JSONArray();
    TestSummary counts = new TestSummary();
    for (JSONObject page : pages.values()) {
      pageList.put(page);
      counts.add(countsOf(page.getJSONObject("counts")));
      if (failed(page.getString("status"))) {
        failed.put(page.getString("path"));
      }
    }
    JSONObject run = new JSONObject();
    run.put("id", id);
    run.put("start", start);
    run.put("durationMillis", durationMillis);
    run.put("counts", countsJson(counts));
    run.put("failedPages", failed);
    run.put("pages", pageList);
    run.put("servers", servers);
    run.put("stoptest", "?responder=stoptest&id=" + id);
    return new JSONObject().put("schemaVersion", SCHEMA_VERSION).put("run", run);
  }

  private static void apply(JSONObject event, Map<String, JSONObject> pages, JSONArray servers) {
    switch (event.getString("event")) {
      case "page-started" -> pages.computeIfAbsent(event.getString("path"), RunDocument::newPage);
      case "table-done" -> page(pages, event.getString("path")).getJSONArray("blocks").put(event.getJSONObject("block"));
      case "page-done" -> finish(page(pages, event.getString("path")), event);
      case "server" -> servers.put(event.getJSONObject("server"));
      default -> { }
    }
  }

  private static JSONObject page(Map<String, JSONObject> pages, String path) {
    return pages.computeIfAbsent(path, RunDocument::newPage);
  }

  private static JSONObject newPage(String path) {
    return new JSONObject()
      .put("path", path)
      .put("counts", countsJson(new TestSummary()))
      .put("durationMillis", 0)
      .put("status", "pass")
      .put("blocks", new JSONArray());
  }

  private static void finish(JSONObject page, JSONObject event) {
    page.put("counts", event.getJSONObject("counts"));
    page.put("durationMillis", event.getLong("durationMillis"));
    page.put("status", event.getString("status"));
  }

  public static JSONObject countsJson(TestSummary summary) {
    return new JSONObject()
      .put("right", summary.getRight())
      .put("wrong", summary.getWrong())
      .put("ignored", summary.getIgnores())
      .put("exceptions", summary.getExceptions());
  }

  public static TestSummary countsOf(JSONObject counts) {
    return new TestSummary(counts.getInt("right"), counts.getInt("wrong"),
      counts.getInt("ignored"), counts.getInt("exceptions"));
  }

  public static String status(String pageName, TestSummary summary) {
    return ExecutionResult.getExecutionResult(pageName, summary).toString();
  }

  public static boolean failed(String status) {
    return "fail".equals(status) || "error".equals(status) || "empty".equals(status);
  }

  public static List<String> failedPages(JSONObject document) {
    JSONArray pages = document.getJSONObject("run").getJSONArray("failedPages");
    List<String> paths = new ArrayList<>();
    for (int i = 0; i < pages.length(); i++) {
      paths.add(pages.getString(i));
    }
    return paths;
  }
}
