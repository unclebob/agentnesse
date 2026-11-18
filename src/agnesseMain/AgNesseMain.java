package agnesseMain;

import agnesse.ConfigurationParameter;
import agnesse.ContextConfigurator;
import agnesse.AgNesse;
import agnesse.AgNesseContext;
import agnesse.mcp.McpServer;
import agnesse.mcp.WikiTools;
import agnesse.reporting.ExitCodeListener;
import agnesse.results.RunQuery;
import agnesse.socketservice.PlainServerSocketFactory;
import agnesse.wiki.PathParser;

import java.io.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static agnesse.ConfigurationParameter.*;

public class AgNesseMain {
  private static final Logger LOG = Logger.getLogger(AgNesseMain.class.getName());

  static Consumer<Integer> halt = System::exit;

  private final ExitCodeListener exitCodeListener = new ExitCodeListener();

  public static void main(String[] args) {
    finish(start(args));
  }

  static void finish(Integer exitCode) {
    if (exitCode != null) {
      exit(exitCode);
    }
  }

  public static Integer start(String[] args) {
    final Arguments arguments;
    try {
      arguments = new Arguments(args);
    } catch (IllegalArgumentException e) {
      Arguments.printUsage();
      return 1;
    }
    try {
      return new AgNesseMain().launchAgNesse(arguments);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Error while starting the AgNesse", e);
      return 1;
    }
  }

  protected static void exit(int exitCode) {
    halt.accept(exitCode);
  }

  public Integer launchAgNesse(Arguments arguments) throws Exception {
    return launchAgNesse(arguments, System.in, System.out);
  }

  public Integer launchAgNesse(Arguments arguments, InputStream in, OutputStream out) throws Exception {
    if (arguments.isMcp() && arguments.hasCommand()) {
      System.err.println("-mcp runs the server on standard input. It does not take -c.");
      return 1;
    }
    ContextConfigurator contextConfigurator = ContextConfigurator.systemDefaults();
    File configFile = new File(arguments.getConfigFile(contextConfigurator));
    try {
      agnesse.AgNesseConfig.apply(contextConfigurator, configFile, arguments.hasConfigFile());
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      return 1;
    }
    contextConfigurator = contextConfigurator.updatedWith(System.getProperties());
    contextConfigurator = arguments.update(contextConfigurator);
    if (arguments.isMcp()) {
      return serveMcp(contextConfigurator, in, out);
    }
    return launchAgNesse(contextConfigurator);
  }

  public Integer launchAgNesse(ContextConfigurator contextConfigurator) throws Exception {
    if (contextConfigurator.get(COMMAND) != null) {
      contextConfigurator.withTestSystemListener(exitCodeListener);
    }

    AgNesseContext context = contextOrNull(contextConfigurator);
    if (context == null) {
      return 1;
    }

    logStartupInfo(context);

    try {
      return launch(context, AgNesseMain.class.getClassLoader());
    } catch (BindException e) {
      LOG.severe("AgNesse cannot be started...");
      LOG.severe("Port " + context.port + " is already in use.");
      LOG.severe("Use the -p <port#> command line argument to use a different port.");
      return 1;
    }

  }

  private Integer serveMcp(ContextConfigurator configurator, InputStream in, OutputStream out) throws Exception {
    AgNesseContext context = contextOrNull(configurator);
    if (context == null) {
      return 1;
    }
    PrintStream previous = null;
    if (out == System.out) {
      previous = System.out;
      System.setOut(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }
    try {
      new McpServer(new WikiTools(context)).serve(in, out);
      return 0;
    } finally {
      if (previous != null) {
        System.setOut(previous);
      } else {
        out.close();
      }
    }
  }

  private AgNesseContext contextOrNull(ContextConfigurator configurator) throws Exception {
    configureLogging("verbose".equalsIgnoreCase(configurator.get(LOG_LEVEL)));
    configurator.withClassLoader(AgNesseMain.class.getClassLoader());
    AgNesseContext context = configurator.makeAgNesseContext();
    if (!establishRequiredDirectories(context.getRootPagePath())) {
      LOG.severe("AgNesse cannot be started...");
      LOG.severe("Unable to create AgNesse root directory in " + context.getRootPagePath());
      LOG.severe("Ensure you have sufficient permissions to create this folder.");
      return null;
    }
    return context;
  }

  private boolean establishRequiredDirectories(String rootPagePath) {
    return establishDirectory(new File(rootPagePath)) &&
            establishDirectory(new File(rootPagePath, PathParser.FILES));
  }

  private static boolean establishDirectory(File path) {
    return path.exists() || path.mkdir();
  }

  private Integer launch(AgNesseContext context, ClassLoader classLoader) throws Exception {
    String command = context.getProperty(COMMAND.getKey());
      if (command != null) {
        String output = context.getProperty(OUTPUT.getKey());
        executeSingleCommand(context.agNesse, jsonCommand(context, command), output);

        return exitCodeListener.getFailCount();
      } else {
        String listen = context.getProperty(LISTEN.getKey());
        LOG.info("Starting AgNesse on " + listen + ":" + context.port);
        ServerSocket serverSocket;
        try {
          serverSocket = openSocket(context);
        } catch (IllegalArgumentException e) {
          System.err.println(e.getMessage());
          return 1;
        }
        context.agNesse.start(serverSocket);
      }
    return null;
  }

  static ServerSocket openSocket(AgNesseContext context) throws IOException {
    String listen = context.getProperty(LISTEN.getKey());
    final InetAddress address;
    try {
      address = InetAddress.getByName(listen);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Cannot listen on " + listen + ": not an address");
    }
    PlainServerSocketFactory sockets = new PlainServerSocketFactory();
    if (address.isAnyLocalAddress()) {
      return sockets.createServerSocket(context.port);
    }
    return sockets.createServerSocket(context.port, address);
  }

  private String jsonCommand(AgNesseContext context, String command) {
    return RunQuery.json(command, context.getProperty(RERUN_FAILURES.getKey()));
  }

  private void executeSingleCommand(AgNesse agNesse, String command, String outputFile) throws Exception {

    LOG.info("Executing command: " + command);

    OutputStream os;

    boolean outputRedirectedToFile = outputFile != null;

    if (outputRedirectedToFile) {
      LOG.info("Command Output redirected to: " + outputFile);
      os = new FileOutputStream(outputFile);
    } else {
      os = System.out;
    }

    agNesse.executeSingleCommand(command, os);
    agNesse.stop();

    if (outputRedirectedToFile) {
      os.close();
    }
  }

  private void logStartupInfo(AgNesseContext context) {
    // This message is on standard output for backward compatibility with Jenkins Fitnesse plugin.
    // (ConsoleHandler of JUL uses standard error output for all messages).
    System.out.println("Bootstrapping AgNesse, the fully integrated standalone wiki and acceptance testing framework.");

    LOG.info("root page: " + context.getRootPage());
    LOG.info("logger: none");
    LOG.info("page factory: " + context.pageFactory);
  }

  public void configureLogging(boolean verbose) {
    if (loggingSystemPropertiesDefined()) {
      return;
    }

    readLogging(AgNesseMain.class.getResourceAsStream((verbose ? "verbose-" : "") + "logging.properties"));
    LOG.finest("Configured verbose logging");
  }

  static void readLogging(InputStream in) {
    try (InputStream stream = in) {
      LogManager.getLogManager().readConfiguration(stream);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Log configuration failed", e);
    }
  }

  private boolean loggingSystemPropertiesDefined() {
    return System.getProperty("java.util.logging.config.class") != null ||
            System.getProperty("java.util.logging.config.file") != null;
  }

}
