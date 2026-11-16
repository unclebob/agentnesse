package agnesse.testsystems.slim;

import agnesse.AgNesseContext;
import agnesse.slim.SlimPipeSocket;
import agnesse.socketservice.*;
import agnesse.testsystems.*;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

import static agnesse.slim.SlimPipeSocket.STDERR_PREFIX;
import static agnesse.slim.SlimPipeSocket.STDOUT_PREFIX;

public class SlimClientBuilder extends ClientBuilder<SlimCommandRunningClient> {
  public static final String SLIM_PORT = "SLIM_PORT";
  public static final String SLIM_HOST = "SLIM_HOST";
  public static final String SLIM_FLAGS = "SLIM_FLAGS";
  private static final String SLIM_VERSION = "SLIM_VERSION";
  public static final String MANUALLY_START_TEST_RUNNER_ON_DEBUG = "MANUALLY_START_TEST_RUNNER_ON_DEBUG";
  public static final String MANUALLY_START_TEST_RUNNER = "MANUALLY_START_TEST_RUNNER";
  public static final int SLIM_USE_PIPE_PORT = 1;

  private static final AtomicInteger slimPortOffset = new AtomicInteger(0);

  private int slimPort;

  public SlimClientBuilder(Descriptor descriptor) {
    super(descriptor);
    slimPort = getNextSlimPort();
  }

  @Override
  public SlimCommandRunningClient build() {
    CommandRunner commandRunner = determineCommandRunner();

    return new SlimCommandRunningClient(commandRunner, determineSlimHost(),
        getSlimPort(), determineTimeout(), getSlimVersion(),
        determineSocketFactory(commandRunner));

  }

  protected CommandRunner determineCommandRunner() {
    if (getSlimPort() == SLIM_USE_PIPE_PORT) {
      // Wrap executionLogListener
      return new CommandRunner(buildCommand(),
        createClasspathEnvironment(getClassPath()),
          getExecutionLogListener(), determineTimeout()) {

        @Override
        protected void redirectOutputs(Process process, final ExecutionLogListener executionLogListener) throws IOException {
          InputStream stderr = process.getErrorStream();
          new Thread(new OutputReadingRunnable(stderr, new OutputWriter() {
            @Override
            public void write(String output) {
              deliverPipeOutput(output, executionLogListener, message -> setCommandErrorMessage(message));
            }
          }), "CommandRunner stdOutErr").start();

        }
      };

    } else if (useManualStartForTestSystem()) {
      return new MockCommandRunner(
          "Connection to running SlimService: " + determineSlimHost() + ":"
              + getSlimPort(), getExecutionLogListener(), determineTimeout());
    } else {
      return new CommandRunner(buildCommand(), createClasspathEnvironment(getClassPath()), getExecutionLogListener(), determineTimeout());
    }
  }

  protected ClientSocketFactory determineSocketFactory(CommandRunner commandRunner) {
    if (getSlimPort() == SLIM_USE_PIPE_PORT) {
      return new PipeBasedSocketFactory(commandRunner);
    }
    return new PlainClientSocketFactory();
  }

  public double getSlimVersion() {
    double version = SlimCommandRunningClient.MINIMUM_REQUIRED_SLIM_VERSION;
    try {
      String slimVersion = getVariable(SLIM_VERSION);
      if (slimVersion != null) {
        version = Double.valueOf(slimVersion);
      }
    } catch (NumberFormatException e) {
      // stick with default
    }
    return version;
  }

  @Override
  protected String defaultTestRunner() {
    return "agnesse.slim.SlimService";
  }

  protected String[] buildCommand() {
    String[] slimArguments = buildArguments();
    String[] slimCommandPrefix = super.buildCommand(getCommandPattern(), getTestRunner(), getClassPath());
    return ArrayUtils.addAll(slimCommandPrefix, slimArguments);
  }

  protected String[] buildArguments() {
    String[] arguments = new String[] {};
    for (String flag : getSlimFlags()) {
      arguments = ArrayUtils.add(arguments, flag);
    }
    arguments = ArrayUtils.add(arguments, Integer.toString(getSlimPort()));
    return arguments;
  }

  static void deliverPipeOutput(String output, ExecutionLogListener listener, java.util.function.Consumer<String> onError) {
    String stdout = originalPipeMessage(output, STDOUT_PREFIX);
    if (stdout != null) {
      listener.stdOut(stdout);
      return;
    }
    String stderr = originalPipeMessage(output, STDERR_PREFIX);
    if (stderr != null) {
      listener.stdErr(stderr);
      onError.accept(stderr);
      return;
    }
    listener.stdOut(output);
  }

  static String originalPipeMessage(String prefixedMessage, String level) {
    if (prefixedMessage.startsWith(level)) {
      return prefixedMessage.substring(level.length() + SlimPipeSocket.FOLLOWING_LINE_PREFIX.length());
    }
    return null;
  }

  public int getSlimPort() {
    return slimPort;
  }

  protected void setSlimPort(int slimPort) {
    this.slimPort = slimPort;
  }

  private int findFreePort() {
    int port;
    try {
      ServerSocket socket = new PlainServerSocketFactory().createServerSocket(0);
      port = socket.getLocalPort();
      socket.close();
    } catch (Exception e) {
      port = -1;
    }
    return port;
  }

  protected int getNextSlimPort() {
    final int base = getSlimPortBase();
    final int poolSize = getSlimPortPoolSize();

    if (base == 0) {
      return findFreePort();
    }
    if (base == SLIM_USE_PIPE_PORT)
      return SLIM_USE_PIPE_PORT;

    synchronized (SlimClientBuilder.class) {
      int offset = Math.floorMod(slimPortOffset.get(), poolSize);
      slimPortOffset.set((offset + 1) % poolSize);
      return base + offset;
    }
  }

  public static void clearSlimPortOffset() {
    slimPortOffset.set(0);
  }

  private int getSlimPortBase() {
    return integerVariable("slim.port", SLIM_PORT, SLIM_USE_PIPE_PORT);
  }

  private int getSlimPortPoolSize() {
    return integerVariable("slim.pool.size", null, 10);
  }

  private int integerVariable(String primary, String alternate, int fallback) {
    try {
      String value = getVariable(primary);
      if (value == null && alternate != null) {
        value = getVariable(alternate);
      }
      if (value != null) {
        return Integer.parseInt(value);
      }
    } catch (NumberFormatException e) {
      // stick with default
    }
    return fallback;
  }

  protected String determineSlimHost() {
    String slimHost = getVariable("slim.host");
    if (slimHost == null) {
      slimHost = getVariable(SLIM_HOST);
    }
    return slimHost == null ? "localhost" : slimHost;
  }

  protected String[] getSlimFlags() {
    String slimFlags = getVariable("slim.flags");
    if (slimFlags == null) {
      slimFlags = getVariable(SLIM_FLAGS);
    }
    return slimFlags == null ? new String[] {} : parseCommandLine(slimFlags);
  }

  protected int determineTimeout() {
    if (isDebug()) {
      Integer debugTimeout = optionalInteger("slim.debug.timeout");
      if (debugTimeout != null) {
        return debugTimeout;
      }
    }
    Integer timeout = optionalInteger("slim.timeout");
    return timeout == null ? 10 : timeout;
  }

  private Integer optionalInteger(String name) {
    try {
      String value = getVariable(name);
      return value == null ? null : Integer.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private boolean useManualStartForTestSystem() {
    if (isDebug()) {
      String useManualStart = getVariable("manually.start.test.runner.on.debug");
      if (useManualStart == null) {
        useManualStart = getVariable(MANUALLY_START_TEST_RUNNER_ON_DEBUG);
      }
      if (useManualStart != null) {
        return "true".equalsIgnoreCase(useManualStart);
      }
    }
    String useManualStart = getVariable("manually.start.test.runner");
    if (useManualStart == null) {
      useManualStart = getVariable(MANUALLY_START_TEST_RUNNER);
    }
    return "true".equalsIgnoreCase(useManualStart);

  }
}
