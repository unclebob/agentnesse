package agnesse.testsystems.slim.tables;

import agnesse.testsystems.slim.SlimClient;
import agnesse.testsystems.slim.SlimClientBuilder;
import agnesse.testsystems.slim.SlimCommandRunningClient;
import agnesse.testsystems.slim.sequence.CallBroke;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens one extra Slim connection per thread and checks that those connections
 * call the fixture the first connection created. A pipe, or a server that
 * keeps a fixture private to one connection, is a page error.
 */
final class SlimLanes {
  static final String PIPE =
      "a concurrent sequence needs a socket Slim server; a pipe runs one call at a time";
  static final String SERIAL = "this Slim server accepts one connection at a time";
  static final String UNSHARED = "this Slim server does not share one fixture across connections";
  private static final String PROBE = "__agnesse_lane_probe__";

  private SlimLanes() {
  }

  static SlimDesk open(SlimClient client, SequenceSheet sheet) throws SyntaxError {
    SlimCommandRunningClient running = socket(client);
    List<SlimClient> extras = connect(running, sheet.threadsHigh);
    SlimDesk desk = new SlimDesk(running, extras);
    try {
      refuse(probe(desk, sheet));
      return desk;
    } catch (SyntaxError e) {
      desk.close();
      throw e;
    }
  }

  /** Null when the connections share the fixture. Otherwise the page error. */
  static String probeVerdict(String message) {
    if (named(message, "NO_METHOD_IN_CLASS")) {
      return null;
    }
    if (named(message, "NO_INSTANCE")) {
      return UNSHARED;
    }
    return text(message);
  }

  private static void refuse(String verdict) throws SyntaxError {
    if (verdict != null) {
      throw new SyntaxError(verdict);
    }
  }

  private static String probe(SlimDesk desk, SequenceSheet sheet) {
    try {
      desk.make("laneProbe", sheet.fixture, sheet.arguments);
      int lane = desk.lanes() - 1;
      desk.call(lane, "laneProbe", PROBE, List.of());
    } catch (CallBroke e) {
      return probeVerdict(e.getMessage());
    }
    return probeVerdict(null);
  }

  private static SlimCommandRunningClient socket(SlimClient client) throws SyntaxError {
    if (client instanceof SlimCommandRunningClient running && running.port() != SlimClientBuilder.SLIM_USE_PIPE_PORT) {
      return running;
    }
    throw new SyntaxError(PIPE);
  }

  private static List<SlimClient> connect(SlimCommandRunningClient running, int threads) throws SyntaxError {
    List<SlimClient> extras = new ArrayList<>();
    try {
      for (int lane = 1; lane < threads; lane++) {
        extras.add(open(running));
      }
      return extras;
    } catch (SyntaxError e) {
      SlimDesk.close(extras);
      throw e;
    }
  }

  private static SlimClient open(SlimCommandRunningClient running) throws SyntaxError {
    SlimClient extra = running.another();
    if (!(extra instanceof SlimCommandRunningClient lane)) {
      throw new SyntaxError(PIPE);
    }
    try {
      lane.connect();
      return lane;
    } catch (Exception e) {
      throw new SyntaxError(SERIAL);
    }
  }

  private static boolean named(String message, String token) {
    return message != null && message.contains(token);
  }

  private static String text(String message) {
    if (message == null || message.isBlank()) {
      return UNSHARED;
    }
    return message;
  }
}
