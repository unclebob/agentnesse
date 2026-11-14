package fitnesse.results;

import fitnesse.testsystems.slim.results.SlimFailure;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * One Slim server record per start. Nested page-driver runs announce
 * themselves as JSON lines on the outer server's stdout; those lines become
 * their own records instead of being pasted into the parent.
 */
final class ServerTranscript {
  private final List<ServerLog> servers = new ArrayList<>();
  private final Deque<ServerLog> nested = new ArrayDeque<>();
  private final StringBuilder stdout = new StringBuilder();
  private final StringBuilder stderr = new StringBuilder();
  private ServerLog active;

  void commandStarted(String name, String command) {
    active = open(name, command);
    nested.clear();
  }

  void stdOut(String output) {
    append(stdout, output, true);
  }

  void stdErr(String output) {
    append(stderr, output, false);
  }

  private void append(StringBuilder buffer, String output, boolean fromOut) {
    buffer.append(output);
    drain(buffer, fromOut);
  }

  void exitCode(int exitCode) {
    active().exitCode = exitCode;
  }

  void exception(Throwable error, SlimFailure launchFailure) {
    ServerLog server = target();
    server.exceptions.add(error.toString());
    if (launchFailure != null) {
      server.launchFailure = launchFailure;
    }
  }

  void flush() {
    drain(stdout, true);
    drain(stderr, false);
    keepRemainder(stdout, true);
    keepRemainder(stderr, false);
  }

  List<JSONObject> toJson() {
    List<JSONObject> json = new ArrayList<>();
    for (ServerLog server : servers) {
      json.add(server.json());
    }
    return json;
  }

  private void drain(StringBuilder buffer, boolean fromOut) {
    int end = buffer.indexOf("\n");
    while (end >= 0) {
      String line = buffer.substring(0, end);
      buffer.delete(0, end + 1);
      accept(line, fromOut);
      end = buffer.indexOf("\n");
    }
  }

  private void keepRemainder(StringBuilder buffer, boolean fromOut) {
    if (buffer.length() == 0) {
      return;
    }
    accept(buffer.toString(), fromOut);
    buffer.setLength(0);
  }

  private void accept(String line, boolean fromOut) {
    if (fromOut && frame(line)) {
      return;
    }
    StringBuilder sink = fromOut ? active().stdout : active().stderr;
    sink.append(line);
    sink.append('\n');
  }

  private boolean frame(String line) {
    if (!line.contains("\"agentnesseLog\"")) {
      return false;
    }
    try {
      apply(new JSONObject(line));
      return true;
    } catch (JSONException ignored) {
      return false;
    }
  }

  private void apply(JSONObject frame) {
    switch (frame.getString("agentnesseLog")) {
      case "start" -> nested.push(open(frame.optString("name"), frame.optString("command")));
      case "stdout" -> target().stdout.append(frame.optString("text"));
      case "stderr" -> target().stderr.append(frame.optString("text"));
      case "exit" -> exitNested(frame.optInt("code"));
      case "exception" -> target().exceptions.add(frame.optString("text"));
      default -> { }
    }
  }

  private void exitNested(int code) {
    ServerLog server = nested.isEmpty() ? active() : nested.pop();
    server.exitCode = code;
  }

  private ServerLog target() {
    return nested.isEmpty() ? active() : nested.peek();
  }

  private ServerLog active() {
    if (active == null) {
      active = open("slim", "");
    }
    return active;
  }

  private ServerLog open(String name, String command) {
    ServerLog server = new ServerLog(name == null ? "" : name);
    server.command.append(command == null ? "" : command);
    servers.add(server);
    return server;
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
