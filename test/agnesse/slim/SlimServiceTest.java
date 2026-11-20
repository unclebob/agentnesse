// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.slim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import agnesse.slim.fixtureInteraction.InteractionDemo;
import agnesse.slim.instructions.CallInstruction;
import agnesse.slim.instructions.MakeInstruction;
import agnesse.socketservice.PlainClientSocketFactory;
import agnesse.testsystems.CompositeExecutionLogListener;
import agnesse.testsystems.MockCommandRunner;
import agnesse.testsystems.slim.SlimCommandRunningClient;
import org.junit.Test;

import static org.junit.Assert.*;

public class SlimServiceTest extends SlimServiceTestBase {

  @Override
  protected String getImport() {
    return "agnesse.slim.test";
  }

  @Override
  protected void startSlimService() throws IOException {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"0"});
    startWithFactoryAsync(JavaSlimFactory.createJavaSlimFactory(options), options);
  }

  @Override
  protected void closeSlimService() throws InterruptedException {
    waitForServiceToStopAsync();
    assertFalse(service.isAlive());
  }

  @Override
  protected String expectedExceptionMessage() {
    return "java.lang.Exception: This is my exception";
  }

  @Override
  protected String expectedStopTestExceptionMessage() {
    return "ABORT_SLIM_TEST:agnesse.slim.test.TestSlim$StopTestException: This is a stop test exception";
  }

  @Test
  public void nullInteractionService_returnsDefaultClass() {
    String defaultInteractionName = JavaSlimFactory.createInteraction(null).getClass().getName();
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"8099"});
    assertEquals(defaultInteractionName, options.interaction.getClass().getName());
  }

  @Test
  public void definedInteractionService_returnsCorrectClass() {
    String interactionClassName = InteractionDemo.class.getName();
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"-i", interactionClassName, "8099"});
    assertEquals(interactionClassName, options.interaction.getClass().getName());
  }

  @Test
  public void undefinedStatementTimeout() {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"8099"});
    assertNull(options.statementTimeout);
  }

  @Test
  public void definedStatementTimeout_returnsTimeout() {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"-s", "1000", "8099"});
    assertEquals(1000, (int) options.statementTimeout);
    assertEquals(Long.valueOf(1_000_000L), options.statementTimeoutMillis);
    assertEquals("1000", options.statementTimeoutLabel);
  }

  @Test
  public void millisecondStatementTimeoutIsAFractionOfASecond() {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"-s", "500ms", "8099"});
    assertNull(options.statementTimeout);
    assertEquals(Long.valueOf(500L), options.statementTimeoutMillis);
    assertEquals("0.5", options.statementTimeoutLabel);
  }

  @Test
  public void millisecondStatementTimeoutThatIsWholeSecondsDropsTheFraction() {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"-s", "2000ms", "8099"});
    assertNull(options.statementTimeout);
    assertEquals(Long.valueOf(2000L), options.statementTimeoutMillis);
    assertEquals("2", options.statementTimeoutLabel);
  }

  @Test
  public void wholeSecondConstructorStoresMillisAndTheSameLabel() {
    SlimService.Options options = new SlimService.Options(true, 1, new agnesse.slim.fixtureInteraction.DefaultInteraction(), false, 3);
    assertEquals(Integer.valueOf(3), options.statementTimeout);
    assertEquals(Long.valueOf(3_000L), options.statementTimeoutMillis);
    assertEquals("3", options.statementTimeoutLabel);
    assertTrue(options.verbose);
  }

  @Test
  public void absentTimeoutConstructorStoresNoMillis() {
    SlimService.Options options = new SlimService.Options(false, 1, new agnesse.slim.fixtureInteraction.DefaultInteraction(), false, null);
    assertNull(options.statementTimeout);
    assertNull(options.statementTimeoutMillis);
    assertNull(options.statementTimeoutLabel);
  }

  @Test
  public void verboseAndDaemonFlagsAreParsed() {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{
      "-v", "-d", "8099"});
    assertTrue(options.verbose);
    assertTrue(options.daemon);
    assertEquals(8099, options.port);
  }

  @Test
  public void invalidCommandLineReturnsNull() {
    assertNull(SlimService.parseCommandLine(new String[]{"-not-an-option", "8099"}));
    assertNull(SlimService.parseCommandLine(new String[]{}));
  }

  @Test
  public void parseFailureWritesUsage() {
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream previous = System.err;
    System.setErr(new PrintStream(err));
    try {
      SlimService.parseCommandLineFailed(new String[]{"nope"});
    } finally {
      System.setErr(previous);
    }
    String text = err.toString();
    assertTrue(text.contains("Invalid command line arguments"));
    assertTrue(text.contains(SlimService.class.getName()));
  }

  @Test
  public void reportsTheBoundPort() throws Exception {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"0"});
    try (ServerSocket socket = new ServerSocket(0)) {
      SlimService service = new SlimService(
        JavaSlimFactory.createJavaSlimFactory(options).getSlimServer(), socket, false);
      assertEquals(socket.getLocalPort(), service.getPort());
    }
  }

  @Test
  public void badArgumentsExitWith97AndABadPortWith98() throws Exception {
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream previous = System.err;
    System.setErr(new PrintStream(err));
    try {
      assertEquals(97, SlimService.run(new String[]{"-not-an-option"}));
      assertEquals(98, SlimService.run(new String[]{"99999"}));
    } finally {
      System.setErr(previous);
    }
    String text = err.toString();
    assertTrue(text.contains("Invalid command line arguments"));
    assertTrue(text.contains("Exiting as exception occured"));
  }

  @Test
  public void refusesAPortThatIsAlreadyTaken() throws Exception {
    try (ServerSocket held = new ServerSocket(0)) {
      SlimService.Options options = SlimService.parseCommandLine(new String[]{
        Integer.toString(held.getLocalPort())});
      try {
        SlimService.startWithFactory(JavaSlimFactory.createJavaSlimFactory(options), options);
        fail("expected the port to be taken");
      } catch (BindException expected) {
        agnesse.testsystems.slim.results.SlimFailure failure =
          agnesse.testsystems.slim.results.SlimFailure.parse(expected.toString());
        assertEquals(agnesse.testsystems.slim.results.SlimFailure.Kind.LAUNCH_FAILURE, failure.kind());
        System.out.println("LAUNCH " + failure.kind() + " " + expected);
      }
    }
  }

  @Test
  public void abandonClosesAClientThatNeverConnected() throws Exception {
    SlimCommandRunningClient client = newClient(1);
    client.abandon();
    assertFalse(client.isConnected());
  }

  @Test
  public void daemonConnectionsShareOneFixture() throws Exception {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"-d", "0"});
    SlimFactory shared = JavaSlimFactory.createJavaSlimFactory(options);
    StatementExecutorInterface first = shared.getStatementExecutor();
    StatementExecutorInterface second = shared.getStatementExecutor();
    first.create("seat", "agnesse.slim.test.Waitlist");
    assertEquals("ok", second.call("seat", "ping"));

    SlimFactory isolated = JavaSlimFactory.createJavaSlimFactory(SlimService.parseCommandLine(new String[]{"0"}));
    StatementExecutorInterface left = isolated.getStatementExecutor();
    StatementExecutorInterface right = isolated.getStatementExecutor();
    left.create("seat", "agnesse.slim.test.Waitlist");
    try {
      right.call("seat", "ping");
      fail("a plain server keeps a fixture on the connection that created it");
    } catch (SlimException e) {
      assertTrue(e.toString(), e.toString().contains("NO_INSTANCE"));
    }
  }

  @Test
  public void daemonSocketsCallTheSameFixture() throws Exception {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"-d", "0"});
    try (ServerSocket socket = new ServerSocket(0)) {
      int port = socket.getLocalPort();
      SlimFactory factory = JavaSlimFactory.createJavaSlimFactory(options);
      SlimService service = new SlimService(factory.getSlimServer(), socket, true);
      Thread server = new Thread(() -> {
        try {
          service.accept();
        } catch (IOException ignored) {
          // Closing the socket ends the daemon loop.
        }
      });
      server.setDaemon(true);
      server.start();
      SlimCommandRunningClient client = newClient(port);
      connectWithin(client);
      SlimCommandRunningClient second = newClient(port);
      connectWithin(second);
      client.invokeAndGetResponse(List.of(new MakeInstruction("m", "seat", "agnesse.slim.test.Waitlist")));
      Map<String, Object> result = second.invokeAndGetResponse(List.of(new CallInstruction("c", "seat", "ping")));
      assertEquals("ok", result.get("c"));
      second.abandon();
      client.bye();
      client.kill();
      socket.close();
      server.join(5000);
      assertFalse(server.isAlive());
    }
  }

  @Test
  public void daemonAcceptsAConnectionUntilTheSocketCloses() throws Exception {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"-d", "0"});
    try (ServerSocket socket = new ServerSocket(0)) {
      int port = socket.getLocalPort();
      SlimService service = new SlimService(
        JavaSlimFactory.createJavaSlimFactory(options).getSlimServer(), socket, true);
      Thread server = new Thread(() -> {
        try {
          service.accept();
        } catch (IOException ignored) {
          // Closing the socket ends the daemon loop.
        }
      });
      server.setDaemon(true);
      server.start();

      SlimCommandRunningClient client = newClient(port);
      connectWithin(client);
      assertTrue(client.isConnected());
      SlimCommandRunningClient second = newClient(port);
      connectWithin(second);
      assertTrue(second.isConnected());
      client.bye();
      client.kill();
      second.bye();
      second.kill();
      assertTrue(server.isAlive());
      socket.close();
      server.join(5000);
      assertFalse(server.isAlive());
    }
  }

  @Test
  public void pipeModeUsesTheConsoleAndDoesNotReplaceSystemOut() throws Exception {
    SlimService.Console previous = SlimService.console;
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PrintStream realOut = System.out;
    SlimService.console = new SlimService.Console() {
      private PrintStream errStream = new PrintStream(err, true);
      private PrintStream outStream = new PrintStream(new ByteArrayOutputStream(), true);
      public InputStream in() { return new ByteArrayInputStream(new byte[0]); }
      public PrintStream out() { return outStream; }
      public PrintStream err() { return errStream; }
      public void setOut(PrintStream out) { outStream = out; }
      public void setErr(PrintStream err) { errStream = err; }
      public void exit(int code) { }
    };
    try {
      SlimService.Options options = SlimService.parseCommandLine(new String[]{"-d", "1"});
      try (java.net.ServerSocket socket = SlimService.open(options)) {
        assertEquals(1, socket.getLocalPort());
      }
      assertTrue(err.toString(), err.toString().contains("Warning: in Slim Pipe mode"));
      assertSame(realOut, System.out);
    } finally {
      SlimService.console = previous;
    }
  }

  @Test
  public void pipeModeWithoutDaemonDoesNotWarn() throws Exception {
    SlimService.Console previous = SlimService.console;
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    SlimService.console = new SlimService.Console() {
      private PrintStream errStream = new PrintStream(err, true);
      private PrintStream outStream = new PrintStream(new ByteArrayOutputStream(), true);
      public InputStream in() { return new ByteArrayInputStream(new byte[0]); }
      public PrintStream out() { return outStream; }
      public PrintStream err() { return errStream; }
      public void setOut(PrintStream out) { outStream = out; }
      public void setErr(PrintStream err) { errStream = err; }
      public void exit(int code) { }
    };
    try {
      SlimService.Options options = SlimService.parseCommandLine(new String[]{"1"});
      try (java.net.ServerSocket socket = SlimService.open(options)) {
        assertEquals(1, socket.getLocalPort());
      }
      assertEquals("", err.toString());
    } finally {
      SlimService.console = previous;
    }
  }

  @Test
  public void outOfMemoryAbortsThroughTheConsole() {
    SlimService.Console previous = SlimService.console;
    int[] code = {-1};
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    SlimService.console = new SlimService.Console() {
      private final PrintStream errStream = new PrintStream(err, true);
      public InputStream in() { return new ByteArrayInputStream(new byte[0]); }
      public PrintStream out() { return errStream; }
      public PrintStream err() { return errStream; }
      public void setOut(PrintStream out) { }
      public void setErr(PrintStream err) { }
      public void exit(int status) { code[0] = status; }
    };
    try {
      SlimService.onOutOfMemory(new OutOfMemoryError("boom"));
      assertEquals(99, code[0]);
      assertTrue(err.toString(), err.toString().contains("Out of Memory"));
    } finally {
      SlimService.console = previous;
    }
  }

  @Test
  public void runReturnsZeroAfterOneClient() throws Exception {
    int port;
    try (ServerSocket probe = new ServerSocket(0)) {
      port = probe.getLocalPort();
    }
    int[] code = {-1};
    Thread server = new Thread(() -> {
      try {
        code[0] = SlimService.run(new String[]{Integer.toString(port)});
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    server.setDaemon(true);
    server.start();

    SlimCommandRunningClient client = new SlimCommandRunningClient(
      new MockCommandRunner(new CompositeExecutionLogListener()),
      "localhost", port, 20,
      SlimCommandRunningClient.MINIMUM_REQUIRED_SLIM_VERSION,
      new PlainClientSocketFactory());
    connectWithin(client);
    assertTrue("Slim server did not accept a connection", client.isConnected());
    client.bye();
    client.kill();
    server.join(5000);
    assertEquals(0, code[0]);
  }

  @Test
  public void servesOneConnectionThenStops() throws Exception {
    int port;
    try (ServerSocket probe = new ServerSocket(0)) {
      port = probe.getLocalPort();
    }
    SlimService.Options options = SlimService.parseCommandLine(new String[]{Integer.toString(port)});
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread server = new Thread(() -> {
      try {
        SlimService.startWithFactory(JavaSlimFactory.createJavaSlimFactory(options), options);
      } catch (Throwable thrown) {
        failure.set(thrown);
      }
    });
    server.setDaemon(true);
    server.start();

    SlimCommandRunningClient client = new SlimCommandRunningClient(
      new MockCommandRunner(new CompositeExecutionLogListener()),
      "localhost", port, 20,
      SlimCommandRunningClient.MINIMUM_REQUIRED_SLIM_VERSION,
      new PlainClientSocketFactory());
    connectWithin(client);
    assertTrue("Slim server did not accept a connection", client.isConnected());
    client.bye();
    client.kill();
    server.join(5000);
    assertFalse(server.isAlive());
    assertNull(failure.get());
  }

  private static SlimCommandRunningClient newClient(int port) {
    return new SlimCommandRunningClient(
      new MockCommandRunner(new CompositeExecutionLogListener()),
      "localhost", port, 20,
      SlimCommandRunningClient.MINIMUM_REQUIRED_SLIM_VERSION,
      new PlainClientSocketFactory());
  }

  private static void connectWithin(SlimCommandRunningClient client) {
    long deadline = System.nanoTime() + 5_000_000_000L;
    while (!client.isConnected() && System.nanoTime() < deadline) {
      try {
        client.connect();
      } catch (Exception ignored) {
        Thread.onSpinWait();
      }
    }
  }
}
