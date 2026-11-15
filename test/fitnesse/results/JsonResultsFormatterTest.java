package fitnesse.results;

import fitnesse.testsystems.ExecutionLogListener;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.TestSummary;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.wiki.WikiPageDummy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonResultsFormatterTest {

  @Test
  public void pageTimeIsTheClockBetweenStartAndFinish() throws Exception {
    long[] now = {1_000L};
    LongSupplier clock = () -> now[0];
    JsonResultsFormatter formatter = new JsonResultsFormatter(new StringBuilder(), false, null, clock);
    TestPage page = page("Timed");

    formatter.testStarted(page);
    now[0] = 1_040L;
    formatter.testComplete(page, new TestSummary(1, 0, 0, 0));

    JSONObject done = formatter.events().get(1);
    assertEquals(40L, done.getLong("durationMillis"));
  }

  @Test
  public void aLaunchFailureIsRecordedOnThatSlimServer() throws Exception {
    StringBuilder out = new StringBuilder();
    JsonResultsFormatter formatter = new JsonResultsFormatter(out, false, null);
    formatter.commandStarted(context("slim", "java -cp fitnesse.jar fitnesse.slim.SlimService"));
    formatter.stdOut("started\n");
    formatter.stdErr("waiting\n");
    formatter.exitCode(1);
    formatter.exceptionOccurred(new IllegalStateException("Could not start slim"));
    formatter.finish();

    JSONObject server = formatter.document().getJSONObject("run").getJSONArray("servers").getJSONObject(0);
    assertEquals("slim", server.getString("name"));
    assertTrue(server.getString("command").contains("SlimService"));
    assertEquals(1, server.getInt("exitCode"));
    assertEquals("started\n", server.getString("stdout"));
    assertEquals("waiting\n", server.getString("stderr"));
    assertEquals("LAUNCH_FAILURE", server.getJSONObject("launchFailure").getString("kind"));
    assertTrue(server.getJSONArray("exceptions").getString(0).contains("Could not start"));
  }

  @Test
  public void streamedEventsReduceToTheSameDocument() throws Exception {
    StringBuilder out = new StringBuilder();
    JsonResultsFormatter formatter = new JsonResultsFormatter(out, true, null);
    formatter.setRunId("run-7");
    TestPage page = page("One");
    formatter.testStarted(page);
    formatter.testComplete(page, new TestSummary(2, 0, 0, 0));
    formatter.commandStarted(context("slim", "java"));
    formatter.exitCode(0);
    formatter.finish();

    JSONObject document = formatter.document();
    List<JSONObject> lines = new ArrayList<>();
    for (String line : out.toString().split("\n")) {
      if (!line.isBlank()) {
        lines.add(new JSONObject(new JSONTokener(line)));
      }
    }
    JSONObject run = document.getJSONObject("run");
    JSONObject reduced = RunDocument.reduce(lines, run.getString("id"), run.getString("name"),
      run.getString("start"), run.getLong("durationMillis"));
    assertJsonEquals(document.toString(), reduced.toString());
    assertEquals("?responder=stoptest&id=run-7", run.getString("stoptest"));
  }

  @Test
  public void eachSlimServerStartIsItsOwnRecord() throws Exception {
    JsonResultsFormatter formatter = new JsonResultsFormatter(new StringBuilder(), false, null);
    formatter.commandStarted(context("slim", "java one"));
    formatter.stdOut("first\n");
    formatter.exitCode(0);
    formatter.commandStarted(context("slim", "java two"));
    formatter.stdErr("java.net.BindException: Address already in use\n");
    formatter.exitCode(98);
    formatter.finish();

    JSONArray servers = formatter.document().getJSONObject("run").getJSONArray("servers");
    assertEquals(2, servers.length());
    assertEquals("java one", servers.getJSONObject(0).getString("command"));
    assertEquals(0, servers.getJSONObject(0).getInt("exitCode"));
    assertEquals("first\n", servers.getJSONObject(0).getString("stdout"));
    assertEquals("", servers.getJSONObject(0).getString("stderr"));
    assertEquals(98, servers.getJSONObject(1).getInt("exitCode"));
    assertEquals("java.net.BindException: Address already in use\n", servers.getJSONObject(1).getString("stderr"));
  }

  @Test
  public void aNestedPageDriverServerKeepsItsOwnStderrAndExitCode() throws Exception {
    JsonResultsFormatter formatter = new JsonResultsFormatter(new StringBuilder(), false, null);
    formatter.commandStarted(context("slim", "java outer"));
    formatter.stdOut(frame("start", new JSONObject().put("name", "slim:child").put("command", "java child")));
    formatter.stdOut(frame("stderr", new JSONObject().put("text", "java.net.BindException: Address already in use\n")));
    formatter.stdOut(frame("exit", new JSONObject().put("code", 98)));
    formatter.exitCode(0);
    formatter.finish();

    JSONArray servers = formatter.document().getJSONObject("run").getJSONArray("servers");
    assertEquals(2, servers.length());
    assertEquals(0, servers.getJSONObject(0).getInt("exitCode"));
    assertEquals("", servers.getJSONObject(0).getString("stderr"));
    assertEquals("slim:child", servers.getJSONObject(1).getString("name"));
    assertEquals("java child", servers.getJSONObject(1).getString("command"));
    assertEquals(98, servers.getJSONObject(1).getInt("exitCode"));
    assertEquals("java.net.BindException: Address already in use\n", servers.getJSONObject(1).getString("stderr"));
  }

  @Test
  public void aBrokenServerMarkerStaysOnTheParentStdout() throws Exception {
    JsonResultsFormatter formatter = new JsonResultsFormatter(new StringBuilder(), false, null);
    formatter.commandStarted(context("slim", "java"));
    formatter.stdOut("{\"agentnesseLog\":\n");
    formatter.finish();
    String stdout = formatter.document().getJSONObject("run").getJSONArray("servers").getJSONObject(0).getString("stdout");
    assertTrue(stdout, stdout.contains("agentnesseLog"));
  }

  @Test
  public void aRunDoneSummaryCountsOnePageOfEachStatus() throws Exception {
    StringBuilder out = new StringBuilder();
    long[] now = {10L};
    JsonResultsFormatter formatter = new JsonResultsFormatter(out, true, new java.io.File("last-failures.json"), () -> now[0]);
    complete(formatter, "PassPage", passTable());
    complete(formatter, "SecondPass", passTable());
    complete(formatter, "FailPage", failTable());
    TestPage errorPage = page("ErrorPage");
    formatter.testStarted(errorPage);
    formatter.tableFinished(errorPage, brokenTable());
    formatter.tableFinished(errorPage, errorTable());
    formatter.testComplete(errorPage, new TestSummary(0, 0, 0, 0));
    complete(formatter, "IgnorePage", ignoreTable());
    now[0] = 10L;
    formatter.testStarted(page("SameInstant"));
    formatter.testComplete(page("SameInstant"), new TestSummary(0, 0, 0, 0));
    formatter.testOutputChunk(page("PassPage"), null);
    formatter.testOutputChunk(page("PassPage"), "<table></table>kept");
    formatter.testOutputChunk(page("PassPage"), "<details class=\"agentnesse-include\"></details>");
    formatter.testOutputChunk(page("PassPage"), "   ");
    formatter.testOutputChunk(page("PassPage"), "<span class=\"error\">Bad table! missing</span>");
    formatter.testSystemStarted(null);
    formatter.finish();
    formatter.finish();

    String body = out.toString();
    assertTrue(body, body.contains("\"summary\":\"2 right, 1 wrong, 1 ignored, 1 exceptions\""));
    assertFalse(body, body.contains("kept"));
    assertEquals(0L, formatter.document().getJSONObject("run").getLong("durationMillis"));
    assertTrue(body, body.contains("\"assertions\":\"0 right, 0 wrong, 0 ignored, 2 exceptions\""));
    assertTrue(body, body.contains("\"summary\":\"2 right, 1 wrong, 1 ignored, 1 exceptions\",\"event\":\"run-done\",\"status\":\"error\""));
    assertEquals(1, count(body, "\"event\":\"output\""));
    assertTrue(body, body.contains("\"text\":\"Bad table! missing\""));
    assertTrue(body, body.contains("\"durationMillis\":0"));
    assertEquals(1, count(body, "\"event\":\"run-done\""));
  }

  @Test
  public void aPassingRunIsNotReportedAsAFailure() throws Exception {
    StringBuilder out = new StringBuilder();
    JsonResultsFormatter formatter = new JsonResultsFormatter(out, true, null);
    formatter.setSuiteName(null);
    formatter.setSuiteName("SuitePage");
    complete(formatter, "PassOnly", passTable());
    formatter.finish();
    assertEquals("SuitePage", formatter.document().getJSONObject("run").getString("name"));
    assertTrue(out.toString(), out.toString().contains("\"summary\":\"1 right, 0 wrong, 0 ignored, 0 exceptions\",\"event\":\"run-done\",\"status\":\"pass\""));
  }

  @Test
  public void oneFailingPageIsAFailingRun() throws Exception {
    StringBuilder out = new StringBuilder();
    JsonResultsFormatter formatter = new JsonResultsFormatter(out, true, null);
    complete(formatter, "FailPage", failTable());
    formatter.finish();
    assertTrue(out.toString(), out.toString().contains("\"summary\":\"0 right, 1 wrong, 0 ignored, 0 exceptions\",\"event\":\"run-done\",\"status\":\"fail\""));
  }

  @Test
  public void aTableOriginAndAStartedSystemAreOnTheEvent() throws Exception {
    JsonResultsFormatter formatter = new JsonResultsFormatter(new StringBuilder(), true, null);
    formatter.testSystemStarted(new fitnesse.testsystems.TestSystem() {
      public String getName() { return "slim:A"; }
      public void start() {}
      public void bye() {}
      public void kill() {}
      public void runTests(TestPage page) {}
      public void addTestSystemListener(fitnesse.testsystems.TestSystemListener listener) {}
      public boolean isSuccessfullyStarted() { return true; }
    });
    TestPage page = page("Origin");
    fitnesse.testsystems.slim.MarkdownTable table = passTable();
    formatter.tableFinished(page, table);
    assertTrue(formatter.events().get(0).toString(), formatter.events().get(0).toString().contains("\"origin\":\"\""));
    table.setOrigin("Included");
    formatter.testStarted(page);
    assertEquals("slim:A", formatter.events().get(formatter.events().size() - 1).getString("system"));
  }

  private static void complete(JsonResultsFormatter formatter, String name, fitnesse.testsystems.slim.MarkdownTable table) {
    TestPage page = page(name);
    formatter.testStarted(page);
    formatter.tableFinished(page, table);
    formatter.testComplete(page, new TestSummary(0, 0, 0, 0));
  }

  private static fitnesse.testsystems.slim.MarkdownTable passTable() {
    fitnesse.testsystems.slim.MarkdownTable table = fitnesse.testsystems.slim.MarkdownTable.parse("| script |\n");
    table.updateContent(0, 0, (fitnesse.testsystems.slim.results.SlimTestResult) fitnesse.testsystems.slim.results.SlimTestResult.pass("ok", "ok"));
    return table;
  }

  private static fitnesse.testsystems.slim.MarkdownTable failTable() {
    fitnesse.testsystems.slim.MarkdownTable table = fitnesse.testsystems.slim.MarkdownTable.parse("| script |\n");
    table.updateContent(0, 0, fitnesse.testsystems.slim.results.SlimTestResult.fail("no", "yes"));
    return table;
  }

  private static fitnesse.testsystems.slim.MarkdownTable brokenTable() {
    fitnesse.testsystems.slim.MarkdownTable table = fitnesse.testsystems.slim.MarkdownTable.parse("| script |\n");
    table.updateContent(0, 0, fitnesse.testsystems.slim.results.SlimTestResult.error("bad table"));
    return table;
  }

  private static fitnesse.testsystems.slim.MarkdownTable errorTable() {
    fitnesse.testsystems.slim.MarkdownTable table = fitnesse.testsystems.slim.MarkdownTable.parse("| script |\n");
    table.updateContent(0, 0, fitnesse.testsystems.slim.results.SlimTestResult.error("boom", "x"));
    return table;
  }

  private static fitnesse.testsystems.slim.MarkdownTable ignoreTable() {
    fitnesse.testsystems.slim.MarkdownTable table = fitnesse.testsystems.slim.MarkdownTable.parse("| script |\n");
    table.updateContent(0, 0, fitnesse.testsystems.slim.results.SlimTestResult.ignore("skip"));
    return table;
  }

  private static int count(String body, String needle) {
    int found = 0;
    int from = 0;
    while (true) {
      int at = body.indexOf(needle, from);
      if (at < 0) {
        return found;
      }
      found++;
      from = at + needle.length();
    }
  }

  private static String frame(String kind, JSONObject body) {
    JSONObject frame = new JSONObject().put("agentnesseLog", kind);
    for (String key : body.keySet()) {
      frame.put(key, body.get(key));
    }
    return frame.toString() + "\n";
  }

  private static TestPage page(String name) {
    fitnesse.wiki.WikiPage root = new WikiPageDummy("Root", "", null);
    return new WikiTestPage(new WikiPageDummy(name, "", root));
  }

  private static ExecutionLogListener.ExecutionContext context(String system, String command) {
    return new ExecutionLogListener.ExecutionContext() {
      @Override
      public String getCommand() {
        return command;
      }

      @Override
      public String getTestSystemName() {
        return system;
      }
    };
  }
}
