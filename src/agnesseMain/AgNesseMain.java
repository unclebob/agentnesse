package agnesseMain;

import agnesse.ConfigurationParameter;
import agnesse.ContextConfigurator;
import agnesse.AgNesse;
import agnesse.AgNesseContext;
import agnesse.reporting.ExitCodeListener;
import agnesse.socketservice.PlainServerSocketFactory;
import agnesse.wiki.PathParser;

import java.io.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static agnesse.ConfigurationParameter.*;

public class AgNesseMain {
  private static final Logger LOG = Logger.getLogger(AgNesseMain.class.getName());

  private final ExitCodeListener exitCodeListener = new ExitCodeListener();

  public static void main(String[] args) {
    Integer exitCode = start(args);
    if (exitCode != null) {
      exit(exitCode);
    }
  }

  static Integer start(String[] args) {
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
    System.exit(exitCode);
  }

  public Integer launchAgNesse(Arguments arguments) throws Exception {
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

    return launchAgNesse(contextConfigurator);
  }

  public Integer launchAgNesse(ContextConfigurator contextConfigurator) throws Exception {
    configureLogging("verbose".equalsIgnoreCase(contextConfigurator.get(LOG_LEVEL)));

    ClassLoader classLoader = AgNesseMain.class.getClassLoader();
    contextConfigurator.withClassLoader(classLoader);

    if (contextConfigurator.get(COMMAND) != null) {
      contextConfigurator.withTestSystemListener(exitCodeListener);
    }

    AgNesseContext context = contextConfigurator.makeAgNesseContext();

    if (!establishRequiredDirectories(context.getRootPagePath())) {
      LOG.severe("AgNesse cannot be started...");
      LOG.severe("Unable to create AgNesse root directory in " + context.getRootPagePath());
      LOG.severe("Ensure you have sufficient permissions to create this folder.");
      return 1;
    }

    logStartupInfo(context);

    try {
      return launch(context, classLoader);
    } catch (BindException e) {
      LOG.severe("AgNesse cannot be started...");
      LOG.severe("Port " + context.port + " is already in use.");
      LOG.severe("Use the -p <port#> command line argument to use a different port.");
      return 1;
    }

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
    String withFormat = command;
    if (command.contains("?") && !command.contains("format=")) {
      withFormat = command + "&format=json";
    }
    String rerun = context.getProperty(RERUN_FAILURES.getKey());
    if (rerun == null) {
      return withFormat;
    }
    return withFormat + "&rerunFailures=" + java.net.URLEncoder.encode(rerun, java.nio.charset.StandardCharsets.UTF_8);
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
    LOG.info("logger: " + (context.logger == null ? "none" : context.logger.toString()));
    LOG.info("page factory: " + context.pageFactory);
  }

  public void configureLogging(boolean verbose) {
    if (loggingSystemPropertiesDefined()) {
      return;
    }

    try (InputStream in = AgNesseMain.class.getResourceAsStream((verbose ? "verbose-" : "") + "logging.properties")) {
      LogManager.getLogManager().readConfiguration(in);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Log configuration failed", e);
    }
    LOG.finest("Configured verbose logging");
  }

  private boolean loggingSystemPropertiesDefined() {
    return System.getProperty("java.util.logging.config.class") != null ||
            System.getProperty("java.util.logging.config.file") != null;
  }

}
