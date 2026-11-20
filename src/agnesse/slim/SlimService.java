// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.slim;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import agnesse.slim.fixtureInteraction.FixtureInteraction;
import agnesse.socketservice.PlainServerSocketFactory;
import agnesse.socketservice.ServerSocketFactory;
import util.CommandLine;

import static agnesse.slim.JavaSlimFactory.*;

public class SlimService {
  private static final String OPTION_DESCRIPTOR = "[-v] [-i interactionClass] [-s statementTimeout] port";

  /**
   * The process streams and the exit call. Tests replace this so pipe mode
   * and the out-of-memory abort can run without touching the real JVM.
   */
  public interface Console {
    InputStream in();
    PrintStream out();
    PrintStream err();
    void setOut(PrintStream out);
    void setErr(PrintStream err);
    void exit(int code);
  }

  static Console console = new Console() {
    public InputStream in() { return System.in; }
    public PrintStream out() { return System.out; }
    public PrintStream err() { return System.err; }
    public void setOut(PrintStream out) { System.setOut(out); }
    public void setErr(PrintStream err) { System.setErr(err); }
    public void exit(int code) { System.exit(code); }
  };

  static void onOutOfMemory(OutOfMemoryError error) {
    console.err().println("Out of Memory. Aborting.");
    error.printStackTrace(console.err());
    console.exit(99);
  }

  public static class Options {
    public final boolean verbose;
    public final int port;
    public final FixtureInteraction interaction;
    /** Whole seconds from <code>-s 1</code>. Null when the flag is absent or uses the <code>ms</code> form. */
    public final Integer statementTimeout;
    /** How long a statement may run, in milliseconds. Null when no timeout was requested. */
    public final Long statementTimeoutMillis;
    /** The number of seconds reported when the timeout fires, such as <code>1</code> or <code>0.5</code>. */
    public final String statementTimeoutLabel;

    public Options(boolean verbose, int port, FixtureInteraction interaction, Integer statementTimeout) {
      this(verbose, port, interaction, statementTimeout, millisFor(statementTimeout), labelFor(statementTimeout));
    }

    public Options(boolean verbose, int port, FixtureInteraction interaction,
                   Integer statementTimeout, Long statementTimeoutMillis, String statementTimeoutLabel) {
      this.verbose = verbose;
      this.port = port;
      this.interaction = interaction;
      this.statementTimeout = statementTimeout;
      this.statementTimeoutMillis = statementTimeoutMillis;
      this.statementTimeoutLabel = statementTimeoutLabel;
    }
  }

  private final ServerSocket serverSocket;
  private final SlimServer slimServer;

  public static void main(String[] args) throws IOException {
    System.exit(run(args));
  }

  static int run(String[] args) throws IOException {
    Options options = parseCommandLine(args);
    if (options == null) {
      parseCommandLineFailed(args);
      return 97;
    }
    try {
      startWithFactory(createJavaSlimFactory(options), options);
      return 0;
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Exiting as exception occured: " + e.getMessage());
      return 98;
    }
  }

  protected static void parseCommandLineFailed(String[] args) {
    System.err.println("Invalid command line arguments: " + Arrays.asList(args));
    System.err.println("Usage:");
    System.err.println("    " + SlimService.class.getName() + " " + OPTION_DESCRIPTOR);
  }

  static ServerSocket open(Options options) throws IOException {
    if (options.port == 1) {
      return new SlimPipeSocket(console);
    }
    return new PlainServerSocketFactory().createServerSocket(options.port);
  }

  public static void startWithFactory(SlimFactory slimFactory, Options options) throws IOException {
    ServerSocket socket = open(options);
    try {
      SlimService slimservice = new SlimService(slimFactory.getSlimServer(), socket);
      slimservice.accept();
    } catch (java.lang.OutOfMemoryError e) {
      onOutOfMemory(e);
      throw e;
    }
  }

  public static Options parseCommandLine(String[] args) {
    CommandLine commandLine = new CommandLine(OPTION_DESCRIPTOR);
    if (commandLine.parse(args)) {
      boolean verbose = commandLine.hasOption("v");
      String interactionClassName = commandLine.getOptionArgument("i", "interactionClass");
      String portString = commandLine.getArgument("port");
      int port = (portString == null) ? 1 : Integer.parseInt(portString);
      String statementTimeoutString = commandLine.getOptionArgument("s", "statementTimeout");
      Integer statementTimeout = statementTimeoutString == null ? null : secondsOf(statementTimeoutString);
      Long statementTimeoutMillis = statementTimeoutString == null ? null : millisOf(statementTimeoutString);
      String statementTimeoutLabel = statementTimeoutString == null ? null : labelOf(statementTimeoutString);
      FixtureInteraction interaction = createInteraction(interactionClassName);
      return new Options(verbose, port, interaction, statementTimeout, statementTimeoutMillis, statementTimeoutLabel);
    }
    return null;
  }

  static Long millisFor(Integer seconds) {
    return seconds == null ? null : seconds * 1000L;
  }

  static String labelFor(Integer seconds) {
    return seconds == null ? null : seconds.toString();
  }

  private static Integer secondsOf(String text) {
    if (text.endsWith("ms"))
      return null;
    return Integer.valueOf(text);
  }

  private static Long millisOf(String text) {
    if (text.endsWith("ms"))
      return Long.valueOf(text.substring(0, text.length() - 2));
    return millisFor(Integer.valueOf(text));
  }

  private static String labelOf(String text) {
    if (text.endsWith("ms"))
      return secondsText(Long.parseLong(text.substring(0, text.length() - 2)));
    return text;
  }

  static String secondsText(long millis) {
    return BigDecimal.valueOf(millis).movePointLeft(3).stripTrailingZeros().toPlainString();
  }

  public SlimService(SlimServer slimServer, ServerSocket serverSocket) throws IOException {
    this.slimServer = slimServer;
    this.serverSocket = serverSocket;
  }

  public int getPort() {
    return serverSocket.getLocalPort();
  }

  public void accept() throws IOException {
    try {
      acceptConnections();
    } catch (java.lang.OutOfMemoryError e) {
      onOutOfMemory(e);
    } finally {
      serverSocket.close();
    }
  }

  private void acceptConnections() throws IOException {
    handle(serverSocket.accept());
  }

  private void handle(Socket socket) throws IOException {
    try {
      slimServer.serve(socket);
    } finally {
      socket.close();
    }
  }
}
