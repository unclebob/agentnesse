package agnesse.testsystems.slim;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import agnesse.slim.SlimServer;
import agnesse.slim.SlimStreamReader;
import agnesse.slim.SlimVersion;
import agnesse.slim.instructions.Instruction;
import agnesse.slim.protocol.SlimDeserializer;
import agnesse.slim.protocol.SlimListBuilder;
import agnesse.slim.protocol.SlimSerializer;
import agnesse.testsystems.ExecutionLogListener;
import agnesse.util.MockSocket;

import static agnesse.testsystems.slim.SlimCommandRunningClient.resultToMap;

public class InProcessSlimClient implements SlimClient {
  private final String testSystemName;
  private final SlimServer slimServer;
  private final ExecutionLogListener executionLogListener;
  private ClassLoader classLoader;
  private MockSocket socket;
  private PipedOutputStream clientOutput;
  private Thread slimServerThread;
  private SlimStreamReader reader;
  private double slimServerVersion;

  void readFrom(SlimStreamReader reader) {
    this.reader = reader;
  }

  public InProcessSlimClient(String testSystemName, SlimServer slimServer, ExecutionLogListener executionLogListener, ClassLoader classLoader) {
    this.testSystemName = testSystemName;
    this.slimServer = slimServer;
    this.executionLogListener = executionLogListener;
    this.classLoader = classLoader;
  }

  @Override
  public void start() throws IOException, SlimVersionMismatch {
    commandStarted();

    PipedInputStream socketInput = new PipedInputStream();
    clientOutput = new PipedOutputStream(socketInput);
    PipedInputStream clientInput = new PipedInputStream();
    PipedOutputStream socketOutput = new PipedOutputStream(clientInput);
    reader = new SlimStreamReader(clientInput);
    socket = new MockSocket(socketInput, socketOutput);
    // Start SlimServer in a separate thread

    slimServerThread = new Thread(new Runnable() {
      @Override public void run() {
        try {
          slimServer.serve(socket);
          executionLogListener.exitCode(0);
        } catch (Throwable t) { // NOSONAR
          // This point is not reached since no errors bubble up this far
          executionLogListener.exceptionOccurred(t);
        }
      }
    });
    slimServerThread.setContextClassLoader(classLoader);
    slimServerThread.start();
    connect();
  }

  private void commandStarted() {
    executionLogListener.commandStarted(ExecutionLogListener.ExecutionContext.of("", testSystemName));
  }

  @Override
  public Map<String, Object> invokeAndGetResponse(List<Instruction> statements) throws SlimCommunicationException {
    return SlimCommandRunningClient.exchange(clientOutput, reader, slimServerVersion, statements);
  }

  @Override
  public void connect() throws IOException, SlimVersionMismatch {
    String slimServerVersionMessage = reader.readLine();
    if (!isConnected(slimServerVersionMessage)) {
      throw new SlimVersionMismatch("Got invalid slim header from client. Read the following: " + slimServerVersionMessage);
    }

    slimServerVersion = Double.parseDouble(slimServerVersionMessage.replace(SlimVersion.SLIM_HEADER, ""));
    if (slimServerVersion == SlimCommandRunningClient.NO_SLIM_SERVER_CONNECTION_FLAG) {
      throw new SlimVersionMismatch("Slim Protocol Version Error: Server did not respond with a valid version number.");
    } else if (slimServerVersion < SlimCommandRunningClient.MINIMUM_REQUIRED_SLIM_VERSION) {
      throw new SlimVersionMismatch(String.format("Slim Protocol Version Error: Expected V%s but was V%s", SlimCommandRunningClient.MINIMUM_REQUIRED_SLIM_VERSION, slimServerVersion));
    }
  }

  public boolean isConnected(String slimServerVersionMessage) {
    return slimServerVersionMessage.startsWith(SlimVersion.SLIM_HEADER);
  }

  @Override
  public void bye() throws IOException {
    // Close slimServer thread
    if (slimServerThread.isAlive()) {
      SlimStreamReader.sendSlimMessage(clientOutput, SlimVersion.BYEMESSAGE);
    }
  }

  @Override
  public void kill() {
    slimServerThread.interrupt();
  }
}
