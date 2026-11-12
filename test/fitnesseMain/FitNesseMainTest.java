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
  public void canUseSingleCommandToCreateSymbolicLink() throws Exception {
    // create page to link to in test root
    FileUtil.createFile(TEST_FITNESSE_ROOT + "/LinkTarget.wiki", "Target for symbolic link unit test");

    String response = runFitnesseMainWith("-r", TEST_FITNESSE_ROOT, "-c", "?responder=symlink&linkName=MyLink&linkPath=LinkTarget");
    assertThat(response, containsString("Executing command:"));
    assertThat(response, not(containsString("Starting FitNesse on port:")));
  }

  @Test
  public void bindsLoopbackByDefaultAndEveryAddressOnlyWithTheFlag() throws Exception {
    try (java.net.ServerSocket loopback = FitNesseMain.openSocket(contextFor(new Arguments("-p", "0")))) {
      assertTrue(loopback.getInetAddress().isLoopbackAddress());
    }
    try (java.net.ServerSocket all = FitNesseMain.openSocket(contextFor(new Arguments("-p", "0", "--all-interfaces")))) {
      assertTrue(all.getInetAddress().isAnyLocalAddress());
    }
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
    try (ServerSocket socket = new ServerSocket(0)) {
      int exitCode = launch(new String[]{
        "-r", TEST_FITNESSE_ROOT, "-p", Integer.toString(socket.getLocalPort())});
      assertEquals(1, exitCode);
    }
  }

  @Test
  public void systemPropertiesTakePrecedenceOverConfiguredProperties() throws Exception {
    final String configFileName = "systemPropertiesTakePrecedenceOverConfiguredProperties.properties";
    String themeKey = ConfigurationParameter.THEME.getKey();
    FileUtil.createFile(configFileName, themeKey + "=example");

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
