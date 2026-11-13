// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.slim;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import fitnesse.slim.fixtureInteraction.FixtureInteraction;
import fitnesse.socketservice.PlainServerSocketFactory;
import fitnesse.socketservice.ServerSocketFactory;
import util.CommandLine;

import static fitnesse.slim.JavaSlimFactory.*;

public class SlimService {
  private static final String OPTION_DESCRIPTOR = "[-v] [-i interactionClass] [-s statementTimeout] [-d] port";

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
    /**
     * daemon mode: keep accepting new connections indefinitely.
     */
    public final boolean daemon;
    public final Integer statementTimeout;

    public Options(boolean verbose, int port, FixtureInteraction interaction, boolean daemon, Integer statementTimeout) {
      this.verbose = verbose;
      this.port = port;
      this.interaction = interaction;
      this.daemon = daemon;
      this.statementTimeout = statementTimeout;
    }
  }

  private final ServerSocket serverSocket;
  private final SlimServer slimServer;
  private final boolean daemon;
  private final Executor executor = Executors.newFixedThreadPool(5);

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
      if (options.daemon) {
        console.err().println("Warning: in Slim Pipe mode the daemon flag is not supported.");
      }
      return new SlimPipeSocket(console);
    }
    return new PlainServerSocketFactory().createServerSocket(options.port);
  }

  public static void startWithFactory(SlimFactory slimFactory, Options options) throws IOException {
    ServerSocket socket = open(options);
    try {
      SlimService slimservice = new SlimService(slimFactory.getSlimServer(),
          socket, options.daemon);
      slimservice.accept();
    } catch (java.lang.OutOfMemoryError e) {
      onOutOfMemory(e);
      throw e;
    } catch (BindException e) {
      System.err.println("Can not bind to port " + options.port + ". Aborting.");
      e.printStackTrace();
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
      Integer statementTimeout = (statementTimeoutString == null) ? null : Integer.parseInt(statementTimeoutString);
      boolean daemon = commandLine.hasOption("d");
      FixtureInteraction interaction = createInteraction(interactionClassName);
      return new Options(verbose, port, interaction, daemon, statementTimeout);
    }
    return null;
  }

  public SlimService(SlimServer slimServer, ServerSocket serverSocket, boolean daemon) throws IOException {
    this.daemon = daemon;
    this.slimServer = slimServer;
    this.serverSocket = serverSocket;
//
  }

  public int getPort() {
    return serverSocket.getLocalPort();
  }

  public void accept() throws IOException {
    try {
      if (daemon) {
        acceptMany();
      } else {
        acceptOne();
      }
    } catch (java.lang.OutOfMemoryError e) {
      onOutOfMemory(e);
    } finally {
      serverSocket.close();
    }
  }

  private void acceptMany() throws IOException {
    while (true) {
      final Socket socket = serverSocket.accept();
      executor.execute(new Runnable() {

        @Override
        public void run() {
          try {
            handle(socket);
          } catch (IOException e) {
            throw new SlimError(e);
          }
        }
      });
    }
  }

  private void handle(Socket socket) throws IOException {
    try {
      slimServer.serve(socket);
    } finally {
      socket.close();
    }
  }

  private void acceptOne() throws IOException {
    Socket socket = serverSocket.accept();
    handle(socket);
  }
}
