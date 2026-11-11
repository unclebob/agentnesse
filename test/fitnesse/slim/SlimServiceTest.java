// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.slim;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicReference;

import fitnesse.slim.fixtureInteraction.InteractionDemo;
import fitnesse.socketservice.PlainClientSocketFactory;
import fitnesse.testsystems.CompositeExecutionLogListener;
import fitnesse.testsystems.MockCommandRunner;
import fitnesse.testsystems.slim.SlimCommandRunningClient;
import org.junit.Test;

import static org.junit.Assert.*;

public class SlimServiceTest extends SlimServiceTestBase {

  @Override
  protected String getImport() {
    return "fitnesse.slim.test";
  }

  @Override
  protected void startSlimService() throws IOException {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"8099"});
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
    return "ABORT_SLIM_TEST:fitnesse.slim.test.TestSlim$StopTestException: This is a stop test exception";
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
  }

  @Test
  public void verboseDaemonAndSslFlagsAreParsed() {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{
      "-v", "-d", "-ssl", "com.example.SslParams", "8099"});
    assertTrue(options.verbose);
    assertTrue(options.daemon);
    assertTrue(options.useSSL);
    assertEquals("com.example.SslParams", options.sslParameterClassName);
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
  public void refusesAPortThatIsAlreadyTaken() throws Exception {
    try (ServerSocket held = new ServerSocket(0)) {
      SlimService.Options options = SlimService.parseCommandLine(new String[]{
        Integer.toString(held.getLocalPort())});
      try {
        SlimService.startWithFactory(JavaSlimFactory.createJavaSlimFactory(options), options);
        fail("expected the port to be taken");
      } catch (BindException expected) {
        assertNotNull(expected);
      }
    }
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
    for (int attempt = 0; attempt < 50 && !client.isConnected(); attempt++) {
      try {
        client.connect();
      } catch (Exception ignored) {
        Thread.sleep(20);
      }
    }
    assertTrue("Slim server did not accept a connection", client.isConnected());
    client.bye();
    client.kill();
    server.join(5000);
    assertFalse(server.isAlive());
    assertNull(failure.get());
  }
}
