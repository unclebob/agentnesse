// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesseMain;

import agnesse.AgNesseConfig;
import agnesse.ConfigurationParameter;
import agnesse.ContextConfigurator;
import agnesse.AgNesse;
import agnesse.AgNesseContext;
import agnesse.testutil.AgNesseUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class AgNesseMainTest {

  private static final String TEST_AGNESSE_ROOT = "testAgNesseRoot";

  private ContextConfigurator context;

  @Before
  public void setUp() throws Exception {
    context = ContextConfigurator.systemDefaults()
      .withRootPath(".")
      .withRootDirectoryName(TEST_AGNESSE_ROOT)
      .withPort(80);
  }

  @After
  public void tearDown() throws Exception {
    FileUtil.deleteFileSystemDirectory(TEST_AGNESSE_ROOT);
  }

  @Test
  public void badArgumentsReturnOneWithoutStarting() {
    assertEquals(Integer.valueOf(1), AgNesseMain.start(new String[] {"-x"}));
  }

  @Test
  public void commandArgCallsExecuteSingleCommand() throws Exception {
    context.withParameter(ConfigurationParameter.COMMAND, "command");

    AgNesse agNesse = mock(AgNesse.class);

    context = spy(context);
    doAnswer(agNesseContextWith(agNesse)).when(context).makeAgNesseContext();

    int exitCode = new AgNesseMain().launchAgNesse(context);
    assertThat(exitCode, is(0));
    verify(agNesse, never()).start(any());
    verify(agNesse).executeSingleCommand("command", System.out);
    verify(agNesse).stop();
  }

  @Test
  public void testDirCreations() throws Exception {
    runAgNesseMainWith("-c", "/root", "-r", TEST_AGNESSE_ROOT);

    assertTrue(new File(TEST_AGNESSE_ROOT).exists());
    assertTrue(new File(TEST_AGNESSE_ROOT, "files").exists());
  }

  @Test
  public void testIsRunning() throws Exception {
    AgNesseContext context = AgNesseUtil.makeTestContext();
    AgNesse agnesse = context.agNesse;

    assertFalse(agnesse.isRunning());

    agnesse.start(new ServerSocket(0));
    assertTrue(agnesse.isRunning());

    agnesse.stop();
    assertFalse(agnesse.isRunning());
  }

  @Test
  public void canRunSingleCommand() throws Exception {
    String response = runAgNesseMainWith("-c", "/root");
    assertThat(response, containsString("Executing command:"));
    assertThat(response, not(containsString("Starting AgNesse on port:")));
  }

  @Test(expected = Exception.class)
  public void runningCommandWithNonExistentAddressResultsInError() throws Exception {
    String[] args = {"-c", "/AgNesse.NonExistentTestCase?test"};
    Arguments arguments = new Arguments(args);
    try {
      new AgNesseMain().launchAgNesse(arguments);
    } catch (Exception e) {
      assertEquals("error loading page: 404", e.getMessage());
      throw e;
    }
  }

  @Test
  public void bindsTheListenAddress() throws Exception {
    try (ServerSocket loopback = AgNesseMain.openSocket(contextFor(new Arguments("-p", "0")))) {
      assertEquals("127.0.0.1", loopback.getInetAddress().getHostAddress());
    }
    try (ServerSocket all = AgNesseMain.openSocket(contextFor(new Arguments("-p", "0", "--listen", "0.0.0.0")))) {
      assertTrue(all.getInetAddress().isAnyLocalAddress());
    }
    try (ServerSocket specific = AgNesseMain.openSocket(contextFor(new Arguments("-p", "0", "--listen", "127.0.0.1")))) {
      assertEquals("127.0.0.1", specific.getInetAddress().getHostAddress());
    }
  }

  @Test
  public void unknownConfigKeyExitsNonZeroAndNamesTheFileAndKey() throws Exception {
    File config = new File("bad-agnesse.json");
    FileUtil.createFile(config.getPath(), "{\"bogus\": 1}\n");
    try {
      assertConfigError(new Arguments("-r", TEST_AGNESSE_ROOT, "-f", config.getPath(), "-c", "/root"),
        "bad-agnesse.json", "bogus");
    } finally {
      config.delete();
    }
  }

  @Test
  public void malformedConfigExitsNonZeroAndNamesTheFile() throws Exception {
    File config = new File("malformed-agnesse.json");
    FileUtil.createFile(config.getPath(), "{");
    try {
      assertConfigError(new Arguments("-r", TEST_AGNESSE_ROOT, "-f", config.getPath(), "-c", "/root"),
        "malformed-agnesse.json", "malformed");
    } finally {
      config.delete();
    }
  }

  @Test
  public void missingExplicitConfigFileExitsNonZero() throws Exception {
    assertConfigError(new Arguments("-r", TEST_AGNESSE_ROOT, "-f", "no-such-agnesse.json", "-c", "/root"),
      "no-such-agnesse.json", "not found");
  }

  private void assertConfigError(Arguments arguments, String... expectedText) throws Exception {
    PrintStream original = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setErr(new PrintStream(captured, true, "UTF-8"));
    int exitCode;
    try {
      exitCode = new AgNesseMain().launchAgNesse(arguments);
    } finally {
      System.setErr(original);
    }
    assertThat(exitCode, not(is(0)));
    String error = captured.toString("UTF-8");
    for (String expected : expectedText) {
      assertThat(error, containsString(expected));
    }
  }

  @Test
  public void aBadListenAddressExitsNonZeroAndNamesTheAddress() throws Exception {
    PrintStream original = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setErr(new PrintStream(captured, true, "UTF-8"));
    int exitCode;
    try {
      exitCode = new AgNesseMain().launchAgNesse(new Arguments(
        "-r", TEST_AGNESSE_ROOT, "-p", "0", "--listen", "999.999.999.999"));
    } finally {
      System.setErr(original);
    }
    assertThat(exitCode, not(is(0)));
    assertThat(captured.toString("UTF-8"), containsString("999.999.999.999"));
  }

  private AgNesseContext contextFor(Arguments arguments) throws Exception {
    return arguments.update(ContextConfigurator.systemDefaults()
      .withRootPath(".")
      .withRootDirectoryName(TEST_AGNESSE_ROOT)).makeAgNesseContext();
  }

  @Test
  public void xmlAndJunitCommandsFailWithUnsupportedFormatMessage() throws Exception {
    FileUtil.createFile(TEST_AGNESSE_ROOT + "/FormatPage.md",
      """
      { "type": "test" }
      
      | script | agnesse.fixtures.EchoFixture |
      | --- | --- | --- |
      | check | echo | hello | hello |
      """);

    assertUnsupportedFormatCommand("FormatPage?test&format=xml", "xml");
    assertUnsupportedFormatCommand("FormatPage?suite&format=junit", "junit");
  }

  private void assertUnsupportedFormatCommand(String command, String format) throws Exception {
    PrintStream original = System.out;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured, true, "UTF-8"));
    int exitCode;
    try {
      try {
        exitCode = new AgNesseMain().launchAgNesse(new Arguments(
          "-r", TEST_AGNESSE_ROOT, "-c", command));
      } catch (Exception e) {
        exitCode = 1;
      }
    } finally {
      System.setOut(original);
    }
    assertThat(exitCode, not(is(0)));
    assertThat(captured.toString("UTF-8"), org.hamcrest.Matchers.containsString(
      agnesse.responders.run.SuiteResponderTest.unsupportedFormatMessage(format).trim()));
  }

  @Test
  public void rerunFailuresIsAppendedToTheCommand() throws Exception {
    context.withParameter(ConfigurationParameter.COMMAND, "Suite?suite");
    context.withParameter(ConfigurationParameter.RERUN_FAILURES, "last failures.json");

    AgNesse agNesse = mock(AgNesse.class);
    context = spy(context);
    doAnswer(agNesseContextWith(agNesse)).when(context).makeAgNesseContext();

    new AgNesseMain().launchAgNesse(context);
    verify(agNesse).executeSingleCommand(
      "Suite?suite&format=json&rerunFailures=last+failures.json", System.out);
  }

  @Test
  public void commandExitCodeIsTheNumberOfFailingTests() throws Exception {
    FileUtil.createFile(TEST_AGNESSE_ROOT + "/TestFail.md",
      """
      { "type": "test" }
      
      | script | agnesse.fixtures.EchoFixture |
      | --- | --- | --- |
      | check | echo | hello | goodbye |
      """);

    int exitCode = launch(new String[]{"-r", TEST_AGNESSE_ROOT, "-c", "TestFail?test"});
    assertEquals(1, exitCode);
  }

  @Test
  public void passingCommandCanWriteResultsToAFile() throws Exception {
    FileUtil.createFile(TEST_AGNESSE_ROOT + "/TestPass.md",
      """
      { "type": "test" }
      
      | script | agnesse.fixtures.EchoFixture |
      | --- | --- | --- |
      | check | echo | hello | hello |
      """);
    File output = new File(TEST_AGNESSE_ROOT, "results.txt");

    int exitCode = launch(new String[]{
      "-r", TEST_AGNESSE_ROOT, "-c", "TestPass?test", "-b", output.getPath()});

    assertEquals(0, exitCode);
    assertThat(FileUtil.getFileContent(output), containsString("\"schemaVersion\": 1"));
    assertThat(FileUtil.getFileContent(output), containsString("\"status\": \"pass\""));
  }

  @Test
  public void occupiedPortReturnsExitCodeOne() throws Exception {
    try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
      int exitCode = launch(new String[]{
        "-r", TEST_AGNESSE_ROOT, "-p", Integer.toString(socket.getLocalPort())});
      assertEquals(1, exitCode);
    }
  }

  @Test
  public void systemPropertiesTakePrecedenceOverConfiguredProperties() throws Exception {
    final String configFileName = "systemPropertiesTakePrecedenceOverConfiguredProperties.json";
    String listenKey = ConfigurationParameter.LISTEN.getKey();
    FileUtil.createFile(configFileName, "{\"listen\": \"0.0.0.0\"}\n");

    System.setProperty(listenKey, "10.9.8.7");
    try {
      ContextConfigurator configurator = ContextConfigurator.systemDefaults();
      AgNesseConfig.apply(configurator, new File(configFileName), true);
      configurator = configurator.updatedWith(System.getProperties());
      assertThat(configurator.get(ConfigurationParameter.LISTEN), is("10.9.8.7"));
    } finally {
      System.getProperties().remove(listenKey);
      FileUtil.deleteFile(configFileName);
    }
  }

  @Test
  public void oneLoggingPropertySkipsReconfiguration() {
    String key = "java.util.logging.config.file";
    String previous = System.getProperty(key);
    Logger logger = Logger.getLogger("agnesse.logging.sentinel");
    Level previousLevel = logger.getLevel();
    System.setProperty(key, "sentinel.properties");
    logger.setLevel(Level.FINE);
    try {
      new AgNesseMain().configureLogging(false);
      assertEquals(Level.FINE, logger.getLevel());
    } finally {
      if (previous == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, previous);
      }
      logger.setLevel(previousLevel);
    }
  }

  private String runAgNesseMainWith(String... args) throws Exception {
    PrintStream err = System.err;
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
    System.setErr(new PrintStream(outputBytes));
    Integer exitCode = launch(args);
    assertThat(exitCode, is(0));
    System.setErr(err);
    return outputBytes.toString();
  }

  private int launch(String... args) throws Exception {
    Integer exitCode = new AgNesseMain().launchAgNesse(new Arguments(args));
    return exitCode == null ? 0 : exitCode;
  }

  private Answer<AgNesseContext> agNesseContextWith(final AgNesse agNesse) {
    return invocation -> {
      AgNesseContext agNesseContext = (AgNesseContext) invocation.callRealMethod();
      Field aField = agNesseContext.getClass().getDeclaredField("agNesse");
      aField.setAccessible(true);
      aField.set(agNesseContext, agNesse);
      return agNesseContext;
    };
  }

}
