package agnesse.results;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * The command line the runner already understands. {@code -c} and MCP both
 * build it here, then hand it to {@code AgNesse.executeSingleCommand}.
 */
public final class RunQuery {
  private RunQuery() {
  }

  public static String json(String command, String rerunFailures) {
    String withFormat = command;
    if (command.contains("?") && !command.contains("format=")) {
      withFormat = command + "&format=json";
    }
    if (rerunFailures == null) {
      return withFormat;
    }
    return withFormat + "&rerunFailures=" + URLEncoder.encode(rerunFailures, StandardCharsets.UTF_8);
  }
}
