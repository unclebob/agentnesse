package agnesse.testsystems.slim.tables;

import agnesse.testsystems.slim.SlimClient;

/**
 * Asks the one Slim connection whether it understands the lanes instruction.
 * A server that does not gets a page error. There is no second connection.
 */
final class SlimLanes {
  static final String NEED =
      "a concurrent sequence needs the lanes instruction; this Slim server does not provide it";

  private SlimLanes() {
  }

  static SlimDesk open(SlimClient client, SequenceSheet sheet) throws SyntaxError {
    SlimDesk desk = new SlimDesk(client);
    try {
      if (!desk.probes()) {
        desk.close();
        throw new SyntaxError(NEED);
      }
      return desk;
    } catch (SyntaxError e) {
      throw e;
    } catch (RuntimeException e) {
      try {
        desk.close();
      } catch (RuntimeException ignored) {
        // A client that never connected has nothing to close.
      }
      throw new SyntaxError(NEED);
    }
  }
}
