package fitnesse.results;

import fitnesse.reporting.BaseFormatter;
import fitnesse.testsystems.ExecutionLogListener;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.TestSummary;
import fitnesse.testsystems.slim.MarkdownTable;
import fitnesse.testsystems.slim.TableFinished;
import fitnesse.testsystems.slim.results.SlimFailure;
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
public class JsonResultsFormatter extends BaseFormatter implements TableFinished, ExecutionLogListener {
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
      .put("block", BlockJson.from(table)));
  }

  @Override
  public void testStarted(TestPage testPage) {
    pageStarted.put(path(testPage), clock.getAsLong());
    record(new JSONObject().put("event", "page-started").put("path", path(testPage)));
  }

  @Override
  public void testComplete(TestPage testPage, TestSummary testSummary) {
    String status = RunDocument.status(testPage.getName(), testSummary);
    if (RunDocument.failed(status)) {
      failures++;
    }
    record(new JSONObject()
      .put("event", "page-done")
      .put("path", path(testPage))
      .put("counts", RunDocument.countsJson(testSummary))
      .put("durationMillis", elapsed(path(testPage)))
      .put("status", status));
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

  private long elapsed(String path) {
    long began = pageStarted.getOrDefault(path, clock.getAsLong());
    return Math.max(0, clock.getAsLong() - began);
  }

  private static String path(TestPage page) {
    return page.getFullPath();
  }
}
