package fitnesseMain;

import fitnesse.ConfigurationParameter;
import fitnesse.ContextConfigurator;
import fitnesse.FitNesse;
import fitnesse.FitNesseContext;
import fitnesse.reporting.ExitCodeListener;
import fitnesse.socketservice.PlainServerSocketFactory;
import fitnesse.wiki.PathParser;

import java.io.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static fitnesse.ConfigurationParameter.*;

public class FitNesseMain {
  private static final Logger LOG = Logger.getLogger(FitNesseMain.class.getName());

  private final ExitCodeListener exitCodeListener = new ExitCodeListener();

  public static void main(String[] args) {
    Arguments arguments = null;
    try {
      arguments = new Arguments(args);
    } catch (IllegalArgumentException e) {
      Arguments.printUsage();
      exit(1);
    }
    Integer exitCode;
    try {
        exitCode = new FitNesseMain().launchFitNesse(arguments);
    } catch (Exception e){
      LOG.log(Level.SEVERE, "Error while starting the FitNesse", e);
      exitCode = 1;
    }
    if (exitCode != null) {
      exit(exitCode);
    }
  }

  protected static void exit(int exitCode) {
    System.exit(exitCode);
  }

  public Integer launchFitNesse(Arguments arguments) throws Exception {
    ContextConfigurator contextConfigurator = ContextConfigurator.systemDefaults();
    contextConfigurator = contextConfigurator.updatedWith(System.getProperties());
    contextConfigurator = contextConfigurator.updatedWith(ConfigurationParameter.loadProperties(new File(arguments.getConfigFile(contextConfigurator))));
    contextConfigurator = arguments.update(contextConfigurator);

    return launchFitNesse(contextConfigurator);
  }

  public Integer launchFitNesse(ContextConfigurator contextConfigurator) throws Exception {
    configureLogging("verbose".equalsIgnoreCase(contextConfigurator.get(LOG_LEVEL)));

    ClassLoader classLoader = FitNesseMain.class.getClassLoader();
    contextConfigurator.withClassLoader(classLoader);

    if (contextConfigurator.get(COMMAND) != null) {
      contextConfigurator.withTestSystemListener(exitCodeListener);
    }

    FitNesseContext context = contextConfigurator.makeFitNesseContext();

    if (!establishRequiredDirectories(context.getRootPagePath())) {
      LOG.severe("FitNesse cannot be started...");
      LOG.severe("Unable to create FitNesse root directory in " + context.getRootPagePath());
      LOG.severe("Ensure you have sufficient permissions to create this folder.");
      return 1;
    }

    logStartupInfo(context);

    try {
      return launch(context, classLoader);
    } catch (BindException e) {
      LOG.severe("FitNesse cannot be started...");
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

  private Integer launch(FitNesseContext context, ClassLoader classLoader) throws Exception {
    String command = context.getProperty(COMMAND.getKey());
      if (command != null) {
        String output = context.getProperty(OUTPUT.getKey());
        executeSingleCommand(context.fitNesse, command, output);

        return exitCodeListener.getFailCount();
      } else {
        String listen = context.getProperty(LISTEN.getKey());
        LOG.info("Starting FitNesse on " + listen + ":" + context.port);
        ServerSocket serverSocket;
        try {
          serverSocket = openSocket(context);
        } catch (IllegalArgumentException e) {
          System.err.println(e.getMessage());
          return 1;
        }
        context.fitNesse.start(serverSocket);
      }
    return null;
  }

  static ServerSocket openSocket(FitNesseContext context) throws IOException {
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

  private void executeSingleCommand(FitNesse fitNesse, String command, String outputFile) throws Exception {

    LOG.info("Executing command: " + command);

    OutputStream os;

    boolean outputRedirectedToFile = outputFile != null;

    if (outputRedirectedToFile) {
      LOG.info("Command Output redirected to: " + outputFile);
      os = new FileOutputStream(outputFile);
    } else {
      os = System.out;
    }

    fitNesse.executeSingleCommand(command, os);
    fitNesse.stop();

    if (outputRedirectedToFile) {
      os.close();
    }
  }

  private void logStartupInfo(FitNesseContext context) {
    // This message is on standard output for backward compatibility with Jenkins Fitnesse plugin.
    // (ConsoleHandler of JUL uses standard error output for all messages).
    System.out.println("Bootstrapping FitNesse, the fully integrated standalone wiki and acceptance testing framework.");

    LOG.info("root page: " + context.getRootPage());
    LOG.info("logger: " + (context.logger == null ? "none" : context.logger.toString()));
    LOG.info("page factory: " + context.pageFactory);
    LOG.info("page theme: " + context.pageFactory.getTheme());
  }

  public void configureLogging(boolean verbose) {
    if (loggingSystemPropertiesDefined()) {
      return;
    }

    InputStream in = FitNesseMain.class.getResourceAsStream((verbose ? "verbose-" : "") + "logging.properties");
    try {
      LogManager.getLogManager().readConfiguration(in);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Log configuration failed", e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          LOG.log(Level.SEVERE, "Unable to close Log configuration file", e);
        }
      }
    }
    LOG.finest("Configured verbose logging");
  }

  private boolean loggingSystemPropertiesDefined() {
    return System.getProperty("java.util.logging.config.class") != null ||
            System.getProperty("java.util.logging.config.file") != null;
  }

}
