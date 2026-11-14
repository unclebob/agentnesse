package fitnesse.results;

import fitnesse.testsystems.ExecutionLogListener;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.TestSummary;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.wiki.WikiPageDummy;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.Assert.assertEquals;
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
    JSONObject reduced = RunDocument.reduce(lines, run.getString("id"), run.getString("start"), run.getLong("durationMillis"));
    assertJsonEquals(document.toString(), reduced.toString());
    assertEquals("?responder=stoptest&id=run-7", run.getString("stoptest"));
  }

  private static TestPage page(String name) {
    return new WikiTestPage(new WikiPageDummy(name, "", null));
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
