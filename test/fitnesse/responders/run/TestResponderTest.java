// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.run;

import fitnesse.FitNesseContext;
import fitnesse.FitNesseVersion;
import fitnesse.authentication.SecureOperation;
import fitnesse.authentication.SecureResponder;
import fitnesse.authentication.SecureTestOperation;
import fitnesse.html.HtmlUtil;
import fitnesse.http.MockRequest;
import fitnesse.http.MockResponseSender;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.util.Clock;
import fitnesse.util.DateAlteringClock;
import fitnesse.util.DateTimeUtil;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPagePath;
import fitnesse.wiki.WikiPageProperty;
import fitnesse.wiki.WikiPageUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.FileUtil;

import java.io.File;

import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static util.RegexTestCase.assertHasRegexp;
import static util.RegexTestCase.assertNotSubString;
import static util.RegexTestCase.assertSubString;
import static util.RegexTestCase.divWithIdAndContent;

public class TestResponderTest {
  private static final String TEST_TIME = "2008-12-05T13:19:00Z";
  private WikiPage root;
  private MockRequest request;
  private SuiteResponder responder;
  private FitNesseContext context;
  private Response response;
  private MockResponseSender sender;
  private WikiPage testPage;
  private String results;
  private boolean debug;

  @Before
  public void setUp() throws Exception {
    debug = true;
    File testDir = new File("TestDir");
    testDir.mkdir();
    Properties properties = new Properties();
    context = FitNesseUtil.makeTestContext();
    root = context.getRootPage();
    request = new MockRequest();
    responder = new TestResponder();
    properties.setProperty("FITNESSE_PORT", String.valueOf(context.port));
    new DateAlteringClock(DateTimeUtil.getDateFromString(TEST_TIME), TimeZone.getTimeZone("CET")).advanceMillisOnEachQuery();
  }

  @After
  public void tearDown() throws Exception {
    FitNesseUtil.destroyTestContext();
    Clock.restoreDefaultClock();
  }

  @Test
  public void xmlAndJunitFormatsAreRejected() throws Exception {
    WikiPageUtil.addPage(root, PathParser.parse("TestPage"), passFixtureTable());
    for (String format : new String[] {"xml", "junit"}) {
      request = new MockRequest();
      request.setResource("TestPage");
      request.addInput("format", format);
      Response rejected = new TestResponder().makeResponse(context, request);
      assertEquals(400, rejected.getStatus());
      assertEquals("text/plain", rejected.getContentType());
      assertEquals(SuiteResponderTest.unsupportedFormatMessage(format), ((SimpleResponse) rejected).getContent());
    }
  }

  @Test
  public void testIsValidHtml() throws Exception {
    doSimpleRun(passFixtureTable());

    assertSubString("<!DOCTYPE html>", results);
    assertSubString("</html>", results);

    //assertSubString("<base href=\"http://somehost.com:8080/\"", results);
    assertSubString("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>", results);
    //assertSubString("Command Line Test Results", html);
  }

  @Test
  public void testHead() throws Exception {
    doSimpleRun(passFixtureTable());
    assertSubString("<div id=\"test-summary\"><div id=\"progressBar\">Preparing Tests ...</div></div>", results);
  }

  @Test
  public void testSimpleRun() throws Exception {
    doSimpleRun(passFixtureTable());

    assertSubString(testPage.getName(), results);
    assertSubString("Test Results", results);
    assertSubString("class", results);
    assertNotSubString("ClassNotFoundException", results);
  }

  private void doSimpleRun(String fixtureTable) throws Exception {
    doSimpleRunWithTags(fixtureTable, null);
  }

  private void doSimpleRunWithTags(String fixtureTable, String tags) throws Exception {
    if (debug) request.addInput("debug", "true");
    String simpleRunPageName = "TestPage";
    testPage = WikiPageUtil.addPage(root, PathParser.parse(simpleRunPageName), classpathWidgets() + fixtureTable);
    if (tags != null) {
      PageData pageData = testPage.getData();
      pageData.setAttribute(PageData.PropertySUITES, tags);
      testPage.commit(pageData);
    }
    request.addInput("requestParam", "2");
    request.setResource(testPage.getName());

    response = responder.makeResponse(context, request);
    sender = new MockResponseSender();
    sender.doSending(response);

    results = sender.sentData();
  }

  @Test
  public void testEmptyTestPage() throws Exception {
    PageData data = root.getData();
    data.setContent(classpathWidgets());
    root.commit(data);
    testPage = WikiPageUtil.addPage(root, PathParser.parse("EmptyTestPage"), "");
    request.setResource(testPage.getName());

    response = responder.makeResponse(context, request);
    sender = new MockResponseSender();
    sender.doSending(response);
    sender.sentData();

    String results = sender.sentData();
    assertSubString("0 right", results);
  }

  @Test
  public void testStandardOutput() throws Exception {
    String content = classpathWidgets()
      + outputWritingTable("output1")
      + outputWritingTable("output2")
      + outputWritingTable("output3");

    String errorLogContent = doRunAndGetErrorLog(content);

    assertHasRegexp("output1", errorLogContent);
    assertHasRegexp("output2", errorLogContent);
    assertHasRegexp("output3", errorLogContent);
  }

  @Test
  public void testErrorOutput() throws Exception {
    String content = classpathWidgets()
      + errorWritingTable("error1")
      + errorWritingTable("error2")
      + errorWritingTable("error3");

    String errorLogContent = doRunAndGetErrorLog(content);

    assertHasRegexp("error1", errorLogContent);
    assertHasRegexp("error2", errorLogContent);
    assertHasRegexp("error3", errorLogContent);
  }

  private String doRunAndGetErrorLog(String content) throws Exception {
    WikiPage testPage = WikiPageUtil.addPage(root, PathParser.parse("TestPage"), content);
    request.setResource(testPage.getName());

    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    String results = sender.sentData();

    assertHasRegexp("\\?executionLog", results);

    return results;
  }

  @Test
  public void testHasExitValueHeader() throws Exception {
    WikiPage testPage = WikiPageUtil.addPage(root, PathParser.parse("TestPage"), classpathWidgets() + passFixtureTable());
    request.setResource(testPage.getName());

    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    String results = sender.sentData();

    assertSubString("Exit-Code: 0", results);
  }

  @Test
  public void exitCodeIsCountOfErrors() throws Exception {
    doSimpleRun(failFixtureTable());
    assertSubString("Exit-Code: 1", results);
  }

  @Test
  public void testTestIdIsSentAsHeader() throws Exception {
    WikiPage testPage = WikiPageUtil.addPage(root, PathParser.parse("TestPage"), "");
    request.setResource(testPage.getName());

    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);

    String results = sender.sentData();
    assertSubString("X-FitNesse-Test-Id: ", results);
  }

  @Test
  public void testFixtureThatCrashes() throws Exception {
    WikiPage testPage = WikiPageUtil.addPage(root, PathParser.parse("TestPage"), classpathWidgets() + crashFixtureTable());
    request.setResource(testPage.getName());

    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);

    String results = sender.sentData();
    assertSubString("?executionLog", results);
  }

  @Test
  public void testResultsIncludeActions() throws Exception {
    doSimpleRun(passFixtureTable());
    assertSubString("<nav", results);
  }

  @Test
  public void testResultsDoHaveHeaderAndFooter() throws Exception {
    WikiPageUtil.addPage(root, PathParser.parse("PageHeader"), "HEADER");
    WikiPageUtil.addPage(root, PathParser.parse("PageFooter"), "FOOTER");
    doSimpleRun(passFixtureTable());
    assertSubString("HEADER", results);
    assertSubString("FOOTER", results);
  }

  @Test
  public void testExecutionLogLinkAppears() throws Exception {
    doSimpleRun(passFixtureTable());
    assertHasRegexp("class=\\\\\"ok\\\\\">Execution Log", results);
  }

  @Test
  public void simpleTextFormatForPassingTest() throws Exception {
    request.addInput("format", "text");
    doSimpleRun(passFixtureTable());
    assertEquals("text/text", response.getContentType());
    assertTrue(results.contains("\n. "));
    assertTrue(results.contains("R:1    W:0    I:0    E:0    TestPage\t(TestPage)"));
    assertTrue(results.contains("1 Tests,\t0 Failures"));
  }

  @Test
  public void simpleTextFormatForFailingTest() throws Exception {
    request.addInput("format", "text");
    doSimpleRun(failFixtureTable());
    assertEquals("text/text", response.getContentType());
    assertTrue(results.contains("\nF "));
    assertTrue(results.contains("R:0    W:1    I:0    E:0    TestPage\t(TestPage)"));
    assertTrue(results.contains("1 Tests,\t1 Failures"));
  }

  @Test
  public void simpleTextFormatForErrorTest() throws Exception {
    request.addInput("format", "text");
    doSimpleRun(errorFixtureTable());
    assertEquals("text/text", response.getContentType());
    assertTrue(results.contains("\nX "));
    assertTrue(results.contains("R:0    W:0    I:0    E:1    TestPage\t(TestPage)"));
    assertTrue(results.contains("1 Tests,\t1 Failures"));
  }

  @Test
  public void testExecutionStatusOk() throws Exception {
    doSimpleRun(passFixtureTable());
    assertTrue(results.contains(">Execution Log<"));
    assertTrue(results.contains("\\\"ok\\\""));
  }

  @Test
  public void debugTest() throws Exception {
    request.addInput("debug", "");
    doSimpleRun(passFixtureTable());
    assertTrue(results.contains(">Execution Log<"));
    assertTrue(results.contains("\\\"ok\\\""));
    assertTrue("should be fast test", responder.isDebug());
  }

  @Test
  public void testExecutionStatusError() throws Exception {
    debug = false;
    doSimpleRun(crashFixtureTable());
    assertTrue(results.contains(">Execution Log<"));
    assertTrue(results.contains("\\\"error\\\""));
  }

  @Test
  public void testExecutionStatusErrorHasPriority() throws Exception {
    debug = false;
    doSimpleRun(errorWritingTable("blah") + crashFixtureTable());
    assertTrue(results.contains("class=\\\"error\\\">Execution Log<"));
  }

  @Test
  public void testTestSummaryAppears() throws Exception {
    doSimpleRun(passFixtureTable());
    assertHasRegexp(divWithIdAndContent("test-summary", ".*?"), results);
  }

  @Test
  public void testTestSummaryInformationAppears() throws Exception {
    doSimpleRun(passFixtureTable());
    assertHasRegexp("<script>.*?document\\.getElementById\\(\"test-summary\"\\)\\.innerHTML = \".*?Assertions:.*?\";.*?</script>", results);
    assertHasRegexp("<script>.*?document\\.getElementById\\(\"test-summary\"\\)\\.className = \".*?\";.*?</script>", results);
  }


  @Test
  public void testTestSummaryHasRightClass() throws Exception {
    doSimpleRun(passFixtureTable());
    assertHasRegexp("<script>.*?document\\.getElementById\\(\"test-summary\"\\)\\.className = \"pass\";.*?</script>", results);
  }

  @Test
  public void testTestHasStopped() throws Exception {

    String semaphoreName = "testTestHasStopped.semaphore";
    File semaphore = new File(semaphoreName);
    if (semaphore.exists())
      semaphore.delete();

    new Thread(new WaitForSemaphoreThenStopProcesses(semaphore)).start();

    doSimpleRun(createAndWaitFixture(semaphoreName));
    assertHasRegexp("Testing was interrupted", results);
    semaphore.delete();
  }

  private String createAndWaitFixture(String semaphoreName) {
    return "!define TEST_SYSTEM {slim}\n" +
      // Set a timeout, so the command can be processed async
      "!define slim.flags {-s 10 }\n" +
      "!|fitnesse.testutil.CreateFileAndWaitFixture|" + semaphoreName + "|\n";
  }

  private class WaitForSemaphoreThenStopProcesses implements Runnable {
    private File semaphore;

    public WaitForSemaphoreThenStopProcesses(File semaphore) {
      this.semaphore = semaphore;
    }

    @Override
    public void run() {
      waitForSemaphore();
      SuiteResponder.runningTestingTracker.stopAllProcesses();
    }

    private void waitForSemaphore() {
      try {
        int i = 1000;
        while (!semaphore.exists()) {
          if (--i <= 0)
            break;
          Thread.sleep(5);
        }
      } catch (InterruptedException e) {
      }
    }
  }


  @Test
  public void testAuthentication_RequiresTestPermission() throws Exception {
    assertTrue(responder instanceof SecureResponder);
    SecureOperation operation = responder.getSecureOperation();
    assertEquals(SecureTestOperation.class, operation.getClass());
  }

  @Test
  public void testSuiteSetUpAndTearDownIsCalledIfSingleTestIsRun() throws Exception {
    WikiPage suitePage = WikiPageUtil.addPage(root, PathParser.parse("TestSuite"), classpathWidgets());
    WikiPage testPage = WikiPageUtil.addPage(suitePage, PathParser.parse("TestPage"), outputWritingTable("Output of TestPage"));
    WikiPageUtil.addPage(suitePage, PathParser.parse(PageData.SUITE_SETUP_NAME), outputWritingTable("Output of SuiteSetUp"));
    WikiPageUtil.addPage(suitePage, PathParser.parse(PageData.SUITE_TEARDOWN_NAME), outputWritingTable("Output of SuiteTearDown"));

    PageData data = testPage.getData();
    WikiPageProperty properties = data.getProperties();
    properties.set(PageData.PropertySUITES, "Test Page tags");
    testPage.commit(data);

    WikiPagePath testPagePath = testPage.getFullPath();
    String resource = PathParser.render(testPagePath);
    request.setResource(resource);

    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    results = sender.sentData();

    assertTrue(results.contains(">Execution Log<"));
    assertHasRegexp("\\?executionLog", results);
    assertSubString("Test Page tags", results);

    assertHasRegexp("Output of SuiteSetUp", results);
    assertHasRegexp("Output of TestPage", results);
    assertHasRegexp("Output of SuiteTearDown", results);
  }

  @Test
  public void testSuiteSetUpDoesNotIncludeSetUp() throws Exception {
    WikiPage suitePage = WikiPageUtil.addPage(root, PathParser.parse("TestSuite"), classpathWidgets());
    WikiPage testPage = WikiPageUtil.addPage(suitePage, PathParser.parse("TestPage"), outputWritingTable("Output of TestPage"));
    WikiPageUtil.addPage(suitePage, PathParser.parse(PageData.SUITE_SETUP_NAME), outputWritingTable("Output of SuiteSetUp"));
    WikiPageUtil.addPage(suitePage, PathParser.parse("SetUp"), outputWritingTable("Output of SetUp"));

    WikiPagePath testPagePath = testPage.getFullPath();
    String resource = PathParser.render(testPagePath);
    request.setResource(resource);

    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    results = sender.sentData();

    assertMessagesOccurInOrder(results, "Output of SuiteSetUp", "Output of SetUp", "Output of TestPage");
    assertMessageHasJustOneOccurrenceOf(results, "Output of SetUp");
  }

  @Test
  public void testSuiteTearDownDoesNotIncludeTearDown() throws Exception {
    WikiPage suitePage = WikiPageUtil.addPage(root, PathParser.parse("TestSuite"), classpathWidgets());
    WikiPage testPage = WikiPageUtil.addPage(suitePage, PathParser.parse("TestPage"), outputWritingTable("Output of TestPage"));
    WikiPageUtil.addPage(suitePage, PathParser.parse(PageData.SUITE_TEARDOWN_NAME), outputWritingTable("Output of SuiteTearDown"));
    WikiPageUtil.addPage(suitePage, PathParser.parse("TearDown"), outputWritingTable("Output of TearDown"));

    WikiPagePath testPagePath = testPage.getFullPath();
    String resource = PathParser.render(testPagePath);
    request.setResource(resource);

    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    results = sender.sentData();

    assertMessagesOccurInOrder(results, "Output of TestPage", "Output of TearDown", "Output of SuiteTearDown");
    assertMessageHasJustOneOccurrenceOf(results, "Output of TearDown");
  }

  @Test
  public void testSuiteSetUpAndSuiteTearDownWithSetUpAndTearDown() throws Exception {
    WikiPage suitePage = WikiPageUtil.addPage(root, PathParser.parse("TestSuite"), classpathWidgets());
    WikiPage testPage = WikiPageUtil.addPage(suitePage, PathParser.parse("TestPage"), outputWritingTable("Output of TestPage"));
    WikiPageUtil.addPage(suitePage, PathParser.parse(PageData.SUITE_SETUP_NAME), outputWritingTable("Output of SuiteSetUp"));
    WikiPageUtil.addPage(suitePage, PathParser.parse("SetUp"), outputWritingTable("Output of SetUp"));
    WikiPageUtil.addPage(suitePage, PathParser.parse(PageData.SUITE_TEARDOWN_NAME), outputWritingTable("Output of SuiteTearDown"));
    WikiPageUtil.addPage(suitePage, PathParser.parse("TearDown"), outputWritingTable("Output of TearDown"));

    WikiPagePath testPagePath = testPage.getFullPath();
    String resource = PathParser.render(testPagePath);
    request.setResource(resource);

    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    results = sender.sentData();

    assertMessagesOccurInOrder(results, "Output of SuiteSetUp", "Output of SetUp", "Output of TestPage", "Output of TearDown", "Output of SuiteTearDown");
    assertMessageHasJustOneOccurrenceOf(results, "Output of SetUp");
  }

  private void assertMessageHasJustOneOccurrenceOf(String output, String regexp) {
    Matcher match = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL).matcher(output);
    match.find();
    boolean found = match.find();
    if (found)
      fail("The regexp <" + regexp + "> was more than once in: " + output + ".");
  }

  private void assertMessagesOccurInOrder(String errorLogContent, String... messages) {
    int previousIndex = 0, currentIndex = 0;
    String previousMsg = "";
    for (String msg: messages) {
      currentIndex = errorLogContent.indexOf(msg);
      assertTrue(String.format("\"%s\" should occur not before \"%s\", but did in \"%s\"", msg, previousMsg, errorLogContent), currentIndex > previousIndex);
      previousIndex = currentIndex;
      previousMsg = msg;
    }
  }

  private String errorWritingTable(String message) {
    return "\n!define TEST_SYSTEM {slim}\n|script|fitnesse.testutil.ErrorWritingFixture|\n|write|!-" + message + "-!|\n\n";
  }

  private String outputWritingTable(String message) {
    return "\n!define TEST_SYSTEM {slim}\n|script|fitnesse.testutil.OutputWritingFixture|\n|write|!-" + message + "-!|\n\n";
  }

  private String classpathWidgets() {
    return "!path classes\n";
  }

  private String crashFixtureTable() {
    return "!define TEST_SYSTEM {slim}\n|fitnesse.testutil.CrashFixture|\n";
  }

  private String passFixtureTable() {
    return "!define TEST_SYSTEM {slim}\n|script|fitnesse.fixtures.EchoFixture|\n|check|echo|ok|ok|\n";
  }

  private String failFixtureTable() {
    return "!define TEST_SYSTEM {slim}\n|script|fitnesse.fixtures.EchoFixture|\n|check|echo|ok|bad|\n";
  }

  private String errorFixtureTable() {
    return "!define TEST_SYSTEM {slim}\n|script|fitnesse.testutil.ErrorFixture|\n|error|\n";
  }

}
