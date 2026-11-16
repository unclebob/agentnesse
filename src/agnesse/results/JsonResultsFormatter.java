package agnesse.results;

import agnesse.reporting.BaseFormatter;
import agnesse.testsystems.ExecutionLogListener;
import agnesse.testsystems.TestPage;
import agnesse.testsystems.TestSummary;
import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.TableFinished;
import agnesse.testsystems.slim.results.SlimFailure;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Collects Slim run events. HTTP streams them as NDJSON. The command line
 * prints the reduced document instead.
 */
public class JsonResultsFormatter extends BaseFormatter implements TableFinished, ExecutionLogListener, agnesse.testrunner.TestsRunnerListener {
  private final Appendable out;
  private final boolean streamEvents;
  private final File failuresFile;
  private final List<JSONObject> events = new ArrayList<>();
  private final Map<String, Long> pageStarted = new LinkedHashMap<>();
  private final ServerTranscript servers = new ServerTranscript();
  private final LongSupplier clock;
  private final long startedAt;
  private String runId = "";
  private String suiteName = "";
  private int failures;
  private String system = "";
  private boolean finished;
  private int pagesTotal;
  private int pagesFinished;

  public JsonResultsFormatter(Appendable out, boolean streamEvents, File failuresFile) {
    this(out, streamEvents, failuresFile, System::currentTimeMillis);
  }

  JsonResultsFormatter(Appendable out, boolean streamEvents, File failuresFile, LongSupplier clock) {
    this.out = out;
    this.streamEvents = streamEvents;
    this.failuresFile = failuresFile;
    this.clock = clock;
    this.startedAt = clock.getAsLong();
  }

  public void setRunId(String runId) {
    this.runId = runId == null ? "" : runId;
  }

  public void setSuiteName(String suiteName) {
    this.suiteName = suiteName == null ? "" : suiteName;
  }

  public List<JSONObject> events() {
    return List.copyOf(events);
  }

  @Override
  public void tableFinished(TestPage page, MarkdownTable table) {
    record(new JSONObject()
      .put("event", "table-done")
      .put("path", path(page))
      .put("origin", table.origin())
      .put("block", BlockJson.from(table)));
  }

  @Override
  public void testStarted(TestPage testPage) {
    pageStarted.put(path(testPage), clock.getAsLong());
    record(new JSONObject().put("event", "page-started").put("path", path(testPage)).put("system", system));
  }

  @Override
  public void testComplete(TestPage testPage, TestSummary testSummary) {
    String page = path(testPage);
    JSONObject counts = PageCounts.counts(blocks(page));
    String status = PageCounts.status(testPage.getName(), counts);
    if (RunDocument.failed(status)) {
      failures++;
    }
    pagesFinished++;
    record(new JSONObject()
      .put("event", "page-done")
      .put("path", page)
      .put("counts", counts)
      .put("assertions", assertionSummary(counts))
      .put("durationMillis", elapsed(page))
      .put("status", status)
      .put("index", pagesFinished)
      .put("pages", pagesTotal));
  }

  private JSONArray blocks(String path) {
    JSONArray blocks = new JSONArray();
    for (JSONObject event : events) {
      if ("table-done".equals(event.optString("event")) && path.equals(event.optString("path"))) {
        blocks.put(event.getJSONObject("block"));
      }
    }
    return blocks;
  }

  @Override
  public synchronized void commandStarted(ExecutionContext context) {
    servers.commandStarted(context.getTestSystemName(), context.getCommand());
  }

  @Override
  public synchronized void stdOut(String output) {
    servers.stdOut(output);
  }

  @Override
  public synchronized void stdErr(String output) {
    servers.stdErr(output);
  }

  @Override
  public synchronized void exitCode(int exitCode) {
    servers.exitCode(exitCode);
  }

  @Override
  public void announceNumberTestsToRun(int testsToRun) {
    pagesTotal = testsToRun;
    record(new JSONObject().put("event", "run-started").put("pages", pagesTotal).put("root", suiteName));
  }

  @Override
  public void unableToStartTestSystem(String testSystemName, Throwable cause) {
    String message = "Unable to start test system '" + testSystemName + "': " + cause;
    record(new JSONObject().put("event", "output").put("text", message));
    exceptionOccurred(cause);
  }

  @Override
  public void testSystemStarted(agnesse.testsystems.TestSystem testSystem) {
    system = testSystem == null || testSystem.getName() == null ? "" : testSystem.getName();
  }

  @Override
  public void testOutputChunk(TestPage testPage, String output) {
    if (output == null || output.contains("<table") || output.contains("<details")) {
      return;
    }
    String text = output.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    if (!text.isEmpty()) {
      record(new JSONObject().put("event", "output").put("path", path(testPage)).put("text", text));
    }
  }

  @Override
  public synchronized void exceptionOccurred(Throwable error) {
    SlimFailure parsed = SlimFailure.parse(error.toString());
    SlimFailure launch = parsed.kind() == SlimFailure.Kind.LAUNCH_FAILURE ? parsed : null;
    servers.exception(error, launch);
  }

  public JSONObject document() {
    long duration = Math.max(0, clock.getAsLong() - startedAt);
    return RunDocument.reduce(events, runId, suiteName, Instant.ofEpochMilli(startedAt).toString(), duration);
  }

  public void finish() throws IOException {
    if (finished) {
      return;
    }
    finished = true;
    synchronized (this) {
      servers.flush();
      for (JSONObject server : servers.toJson()) {
        record(new JSONObject().put("event", "server").put("server", server));
      }
    }
    JSONObject document = document();
    if (!streamEvents) {
      out.append(document.toString(2));
      out.append("\n");
    } else {
      int[] pages = pageTally();
      String summary = pages[0] + " right, " + pages[1] + " wrong, " + pages[2] + " ignored, " + pages[3] + " exceptions";
      String status = pages[3] > 0 ? "error" : pages[1] > 0 ? "fail" : "pass";
      record(new JSONObject().put("event", "run-done").put("summary", summary).put("status", status));
    }
    writeFailures(document);
  }

  @Override
  public int getErrorCount() {
    return failures;
  }

  private void record(JSONObject event) {
    events.add(event);
    if (streamEvents) {
      try {
        out.append(event.toString());
        out.append("\n");
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void writeFailures(JSONObject document) throws IOException {
    if (failuresFile == null) {
      return;
    }
    JSONArray pages = document.getJSONObject("run").getJSONArray("failedPages");
    File parent = failuresFile.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }
    Files.writeString(failuresFile.toPath(), new JSONObject().put("failedPages", pages).toString(2), StandardCharsets.UTF_8);
  }

  private static String assertionSummary(JSONObject counts) {
    int exceptions = counts.getInt("exceptions") + counts.getInt("tableFailures");
    return counts.getInt("right") + " right, " + counts.getInt("wrong") + " wrong, "
      + counts.getInt("ignored") + " ignored, " + exceptions + " exceptions";
  }

  private int[] pageTally() {
    int[] pages = new int[4];
    for (JSONObject event : events) {
      if (!"page-done".equals(event.optString("event"))) {
        continue;
      }
      switch (event.optString("status")) {
        case "pass" -> pages[0]++;
        case "fail" -> pages[1]++;
        case "ignore" -> pages[2]++;
        case "error" -> pages[3]++;
        default -> { }
      }
    }
    return pages;
  }

  private long elapsed(String path) {
    long began = pageStarted.getOrDefault(path, clock.getAsLong());
    return Math.max(0, clock.getAsLong() - began);
  }

  private static String path(TestPage page) {
    return page.getFullPath();
  }
}
