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
  private final Map<String, ServerLog> servers = new LinkedHashMap<>();
  private final Map<String, Long> pageStarted = new LinkedHashMap<>();
  private final LongSupplier clock;
  private final long startedAt;
  private String currentServer = "slim";
  private String runId = "";
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
  public void commandStarted(ExecutionContext context) {
    currentServer = context.getTestSystemName();
    current().command.append(context.getCommand());
  }

  @Override
  public void stdOut(String output) {
    current().stdout.append(output);
  }

  @Override
  public void stdErr(String output) {
    ServerLog server = current();
    server.stderr.append(output);
  }

  @Override
  public void exitCode(int exitCode) {
    current().exitCode = exitCode;
  }

  @Override
  public void exceptionOccurred(Throwable e) {
    ServerLog server = current();
    server.exceptions.add(e.toString());
    SlimFailure parsed = SlimFailure.parse(e.toString());
    if (parsed.kind() == SlimFailure.Kind.LAUNCH_FAILURE) {
      server.launchFailure = parsed;
    }
  }

  public JSONObject document() {
    long duration = Math.max(0, clock.getAsLong() - startedAt);
    return RunDocument.reduce(events, runId, Instant.ofEpochMilli(startedAt).toString(), duration);
  }

  public void finish() throws IOException {
    for (ServerLog server : servers.values()) {
      record(new JSONObject().put("event", "server").put("server", server.json()));
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

  private ServerLog current() {
    return servers.computeIfAbsent(currentServer, ServerLog::new);
  }

  private static String path(TestPage page) {
    return page.getFullPath();
  }

  private static final class ServerLog {
    private final String name;
    private final StringBuilder command = new StringBuilder();
    private final StringBuilder stdout = new StringBuilder();
    private final StringBuilder stderr = new StringBuilder();
    private final List<String> exceptions = new ArrayList<>();
    private Integer exitCode;
    private SlimFailure launchFailure;

    private ServerLog(String name) {
      this.name = name;
    }

    private JSONObject json() {
      JSONObject server = new JSONObject();
      server.put("name", name);
      server.put("command", command.toString());
      server.put("exitCode", exitCode == null ? JSONObject.NULL : exitCode);
      server.put("stdout", stdout.toString());
      server.put("stderr", stderr.toString());
      server.put("exceptions", new JSONArray(exceptions));
      server.put("launchFailure", launchFailure == null ? JSONObject.NULL : BlockJson.errorJson(launchFailure));
      return server;
    }
  }
}
