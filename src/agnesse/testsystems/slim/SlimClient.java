package agnesse.testsystems.slim;

import agnesse.slim.instructions.Instruction;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SlimClient {
  void start() throws IOException, SlimVersionMismatch;

  Map<String, Object> invokeAndGetResponse(List<Instruction> statements) throws SlimCommunicationException;

  void connect() throws IOException, SlimVersionMismatch;

  void bye() throws IOException;

  /** Stop without waiting for a daemon process to exit on its own. */
  default void abandon() throws IOException {
    bye();
  }

  /** Another connection to the server this client already started, or null when that is impossible. */
  default SlimClient another() {
    return null;
  }

  void kill();
}
