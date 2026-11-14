package fitnesse.testsystems;

import org.json.JSONObject;

/**
 * Prints one JSON line per Slim server event. A page driver runs inside the
 * Slim process, so these lines travel on that process's stdout and the outer
 * run can file them on the server that produced them.
 */
public class ConsoleExecutionLogListener implements ExecutionLogListener {
  @Override
  public void commandStarted(ExecutionContext context) {
    line("start", new JSONObject()
      .put("name", context.getTestSystemName())
      .put("command", context.getCommand()));
  }

  @Override
  public void stdOut(String output) { text("stdout", output); }

  @Override
  public void stdErr(String output) { text("stderr", output); }

  @Override
  public void exitCode(int exitCode) {
    line("exit", new JSONObject().put("code", exitCode));
  }

  @Override
  public void exceptionOccurred(Throwable e) {
    line("exception", new JSONObject().put("text", String.valueOf(e)));
  }

  private static void text(String kind, String output) {
    line(kind, new JSONObject().put("text", output));
  }

  private static void line(String kind, JSONObject body) {
    JSONObject frame = new JSONObject().put("agentnesseLog", kind);
    for (String key : body.keySet()) {
      frame.put(key, body.get(key));
    }
    System.out.println(frame.toString());
  }
}
