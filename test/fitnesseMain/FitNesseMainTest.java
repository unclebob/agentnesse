// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesseMain;

import fitnesse.ConfigurationParameter;
import fitnesse.ContextConfigurator;
import fitnesse.FitNesse;
import fitnesse.FitNesseContext;
import fitnesse.testutil.FitNesseUtil;
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

public class FitNesseMainTest {

  private static final String TEST_FITNESSE_ROOT = "testFitnesseRoot";

  private ContextConfigurator context;

  @Before
  public void setUp() throws Exception {
    context = ContextConfigurator.systemDefaults()
      .withRootPath(".")
      .withRootDirectoryName(TEST_FITNESSE_ROOT)
      .withPort(80);
  }

  @After
  public void tearDown() throws Exception {
    FileUtil.deleteFileSystemDirectory(TEST_FITNESSE_ROOT);
  }

  @Test
  public void badArgumentsReturnOneWithoutStarting() {
    assertEquals(Integer.valueOf(1), FitNesseMain.start(new String[] {"-x"}));
  }

  @Test
  public void commandArgCallsExecuteSingleCommand() throws Exception {
    context.withParameter(ConfigurationParameter.COMMAND, "command");

    FitNesse fitNesse = mock(FitNesse.class);

    context = spy(context);
    doAnswer(fitNesseContextWith(fitNesse)).when(context).makeFitNesseContext();

    int exitCode = new FitNesseMain().launchFitNesse(context);
    assertThat(exitCode, is(0));
    verify(fitNesse, never()).start(any());
    verify(fitNesse).executeSingleCommand("command", System.out);
    verify(fitNesse).stop();
  }

  @Test
  public void testDirCreations() throws Exception {
    runFitnesseMainWith("-c", "/root", "-r", TEST_FITNESSE_ROOT);

    assertTrue(new File(TEST_FITNESSE_ROOT).exists());
    assertTrue(new File(TEST_FITNESSE_ROOT, "files").exists());
  }

  @Test
  public void testIsRunning() throws Exception {
    FitNesseContext context = FitNesseUtil.makeTestContext();
    FitNesse fitnesse = context.fitNesse;

    assertFalse(fitnesse.isRunning());

    fitnesse.start(new ServerSocket(0));
    assertTrue(fitnesse.isRunning());

    fitnesse.stop();
    assertFalse(fitnesse.isRunning());
  }

  @Test
  public void canRunSingleCommand() throws Exception {
    String response = runFitnesseMainWith("-c", "/root");
    assertThat(response, containsString("Executing command:"));
    assertThat(response, not(containsString("Starting FitNesse on port:")));
  }

  @Test(expected = Exception.class)
  public void runningCommandWithNonExistentAddressResultsInError() throws Exception {
    String[] args = {"-c", "/FitNesse.NonExistentTestCase?test"};
    Arguments arguments = new Arguments(args);
    try {
      new FitNesseMain().launchFitNesse(arguments);
    } catch (Exception e) {
      assertEquals("error loading page: 404", e.getMessage());
      throw e;
    }
  }

  @Test
  public void bindsTheListenAddress() throws Exception {
    try (ServerSocket loopback = FitNesseMain.openSocket(contextFor(new Arguments("-p", "0")))) {
      assertEquals("127.0.0.1", loopback.getInetAddress().getHostAddress());
    }
    try (ServerSocket all = FitNesseMain.openSocket(contextFor(new Arguments("-p", "0", "--listen", "0.0.0.0")))) {
      assertTrue(all.getInetAddress().isAnyLocalAddress());
    }
    try (ServerSocket specific = FitNesseMain.openSocket(contextFor(new Arguments("-p", "0", "--listen", "127.0.0.1")))) {
      assertEquals("127.0.0.1", specific.getInetAddress().getHostAddress());
    }
  }

  @Test
  public void unknownConfigKeyExitsNonZeroAndNamesTheFileAndKey() throws Exception {
    File config = new File("bad-agentnesse.json");
    FileUtil.createFile(config.getPath(), "{\"bogus\": 1}\n");
    try {
      assertConfigError(new Arguments("-r", TEST_FITNESSE_ROOT, "-f", config.getPath(), "-c", "/root"),
        "bad-agentnesse.json", "bogus");
    } finally {
      config.delete();
    }
  }

  @Test
  public void malformedConfigExitsNonZeroAndNamesTheFile() throws Exception {
    File config = new File("malformed-agentnesse.json");
    FileUtil.createFile(config.getPath(), "{");
    try {
      assertConfigError(new Arguments("-r", TEST_FITNESSE_ROOT, "-f", config.getPath(), "-c", "/root"),
        "malformed-agentnesse.json", "malformed");
    } finally {
      config.delete();
    }
  }

  @Test
  public void missingExplicitConfigFileExitsNonZero() throws Exception {
    assertConfigError(new Arguments("-r", TEST_FITNESSE_ROOT, "-f", "no-such-agentnesse.json", "-c", "/root"),
      "no-such-agentnesse.json", "not found");
  }

  private void assertConfigError(Arguments arguments, String... expectedText) throws Exception {
    PrintStream original = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setErr(new PrintStream(captured, true, "UTF-8"));
    int exitCode;
    try {
      exitCode = new FitNesseMain().launchFitNesse(arguments);
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
      exitCode = new FitNesseMain().launchFitNesse(new Arguments(
        "-r", TEST_FITNESSE_ROOT, "-p", "0", "--listen", "999.999.999.999"));
    } finally {
      System.setErr(original);
    }
    assertThat(exitCode, not(is(0)));
    assertThat(captured.toString("UTF-8"), containsString("999.999.999.999"));
  }

  private FitNesseContext contextFor(Arguments arguments) throws Exception {
    return arguments.update(ContextConfigurator.systemDefaults()
      .withRootPath(".")
      .withRootDirectoryName(TEST_FITNESSE_ROOT)).makeFitNesseContext();
  }

  @Test
  public void xmlAndJunitCommandsFailWithUnsupportedFormatMessage() throws Exception {
    FileUtil.createFile(TEST_FITNESSE_ROOT + "/FormatPage.wiki",
      "!define TEST_SYSTEM {slim}\n" +
        "|script|fitnesse.fixtures.EchoFixture|\n" +
        "|check|echo|hello|hello|\n");

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
        exitCode = new FitNesseMain().launchFitNesse(new Arguments(
          "-r", TEST_FITNESSE_ROOT, "-c", command));
      } catch (Exception e) {
        exitCode = 1;
      }
    } finally {
      System.setOut(original);
    }
    assertThat(exitCode, not(is(0)));
    assertThat(captured.toString("UTF-8"), org.hamcrest.Matchers.containsString(
      fitnesse.responders.run.SuiteResponderTest.unsupportedFormatMessage(format).trim()));
  }

  @Test
  public void commandExitCodeIsTheNumberOfFailingTests() throws Exception {
    FileUtil.createFile(TEST_FITNESSE_ROOT + "/TestFail.wiki",
      "!define TEST_SYSTEM {slim}\n" +
        "|script|fitnesse.fixtures.EchoFixture|\n" +
        "|check|echo|hello|goodbye|\n");

    int exitCode = launch(new String[]{"-r", TEST_FITNESSE_ROOT, "-c", "TestFail?test&format=text"});
    assertEquals(1, exitCode);
  }

  @Test
  public void passingCommandCanWriteResultsToAFile() throws Exception {
    FileUtil.createFile(TEST_FITNESSE_ROOT + "/TestPass.wiki",
      "!define TEST_SYSTEM {slim}\n" +
        "|script|fitnesse.fixtures.EchoFixture|\n" +
        "|check|echo|hello|hello|\n");
    File output = new File(TEST_FITNESSE_ROOT, "results.txt");

    int exitCode = launch(new String[]{
      "-r", TEST_FITNESSE_ROOT, "-c", "TestPass?test&format=text", "-b", output.getPath()});

    assertEquals(0, exitCode);
    assertThat(FileUtil.getFileContent(output), containsString("0 Failures"));
  }

  @Test
  public void occupiedPortReturnsExitCodeOne() throws Exception {
    try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
      int exitCode = launch(new String[]{
        "-r", TEST_FITNESSE_ROOT, "-p", Integer.toString(socket.getLocalPort())});
      assertEquals(1, exitCode);
    }
  }

  @Test
  public void systemPropertiesTakePrecedenceOverConfiguredProperties() throws Exception {
    final String configFileName = "systemPropertiesTakePrecedenceOverConfiguredProperties.json";
    String themeKey = ConfigurationParameter.THEME.getKey();
    FileUtil.createFile(configFileName, "{\"theme\": \"example\"}\n");

    System.setProperty(themeKey, "othertheme");
    try {
      // Checked via logging:
      String output = runFitnesseMainWith("-c", "/root", "-f", configFileName);
      assertThat(output, containsString("othertheme"));
    } finally {
      System.getProperties().remove(themeKey);
      FileUtil.deleteFile(configFileName);
    }
  }

  private String runFitnesseMainWith(String... args) throws Exception {
    PrintStream err = System.err;
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
    System.setErr(new PrintStream(outputBytes));
    Integer exitCode = launch(args);
    assertThat(exitCode, is(0));
    System.setErr(err);
    return outputBytes.toString();
  }

  private int launch(String... args) throws Exception {
    Integer exitCode = new FitNesseMain().launchFitNesse(new Arguments(args));
    return exitCode == null ? 0 : exitCode;
  }

  private Answer<FitNesseContext> fitNesseContextWith(final FitNesse fitNesse) {
    return invocation -> {
      FitNesseContext fitNesseContext = (FitNesseContext) invocation.callRealMethod();
      Field aField = fitNesseContext.getClass().getDeclaredField("fitNesse");
      aField.setAccessible(true);
      aField.set(fitNesseContext, fitNesse);
      return fitNesseContext;
    };
  }

}
