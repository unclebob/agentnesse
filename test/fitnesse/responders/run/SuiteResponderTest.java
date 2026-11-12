// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.run;

import fitnesse.FitNesseContext;
import fitnesse.http.ChunkedResponse;
import fitnesse.http.MockRequest;
import fitnesse.http.MockResponseSender;
import fitnesse.http.Response;
import fitnesse.http.ResponseSender;
import fitnesse.http.SimpleResponse;
import fitnesse.reporting.BaseFormatter;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.util.Clock;
import fitnesse.util.DateAlteringClock;
import fitnesse.util.DateTimeUtil;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageProperty;
import fitnesse.wiki.WikiPageUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import static fitnesse.wiki.WikiPageProperty.SUITES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static util.RegexTestCase.assertDoesntHaveRegexp;
import static util.RegexTestCase.assertHasRegexp;
import static util.RegexTestCase.assertNotSubString;
import static util.RegexTestCase.assertSubString;

public class SuiteResponderTest {
  private static final String TEST_TIME = "2008-12-05T14:19:00+14:00";
  private MockRequest request;
  private SuiteResponder responder;
  private WikiPage root;
  private WikiPage suite;
  private FitNesseContext context;
  private final String fitPassFixture = "!define TEST_SYSTEM {slim}\n|script|fitnesse.fixtures.EchoFixture|\n|check|echo|ok|ok|\n";
  private final String fitFailFixture = "!define TEST_SYSTEM {slim}\n|script|fitnesse.fixtures.EchoFixture|\n|check|echo|ok|bad|\n";
  private final String simpleSlimDecisionTable = "!define TEST_SYSTEM {slim}\n" +
    "|!-DT:fitnesse.slim.test.TestSlim-!|\n" +
    "|string|get string arg?|\n" +
    "|wow|wow|\n";

  @Before
  public void setUp() throws Exception {
    String suitePageName = "SuitePage";
    context = FitNesseUtil.makeTestContext();
    root = context.getRootPage();
    PageData data = root.getData();
    data.setContent(classpathWidgets());
    root.commit(data);
    suite = WikiPageUtil.addPage(root, PathParser.parse(suitePageName), "This is the test suite\n");
    addTestToSuite("TestOne", fitPassFixture);

    request = new MockRequest();
    request.setResource(suitePageName);
    request.addInput("debug", "");
    responder = new SuiteResponder();
    responder.page = suite;

    new DateAlteringClock(DateTimeUtil.getDateFromString(TEST_TIME)).freeze();
  }

  @After
  public void restoreDefaultClock() {
    Clock.restoreDefaultClock();
  }

  private WikiPage addTestToSuite(String name, String content) {
    return addTestPage(suite, name, content);
  }

  private WikiPage addTestPage(WikiPage page, String name, String content) {
    WikiPage testPage = WikiPageUtil.addPage(page, PathParser.parse(name), content);
    PageData data = testPage.getData();
    data.setAttribute("Test");
    testPage.commit(data);
    return testPage;
  }

  @After
  public void tearDown() throws Exception {
    FitNesseUtil.destroyTestContext();
  }

  @Test
  public void xmlAndJunitFormatsAreRejected() throws Exception {
    for (String format : new String[] {"xml", "junit"}) {
      request.addInput("format", format);
      Response response = responder.makeResponse(context, request);
      assertEquals(400, response.getStatus());
      assertEquals("text/plain", response.getContentType());
      assertEquals(unsupportedFormatMessage(format), ((SimpleResponse) response).getContent());
    }
  }

  public static String unsupportedFormatMessage(String format) {
    return "Result format " + format + " is not supported until JSON results land. format=text and HTML still work.\n";
  }

  @Test
  public void nonExistingReturns404() throws Exception {
    request.setResource("DoesNotExist");
    Response response = responder.makeResponse(context, request);

    int status = response.getStatus();
    assertEquals(404, status);

    String results = getResponseContent(response);
    assertSubString("Not Found:DoesNotExist", results);
  }

  @Test
  public void testWithOneTest() throws Exception {
    String results = runSuite();
    assertSubString("href=\\\"#TestOne1\\\"", results);
    assertSubString("1 right", results);
    assertSubString("name=\"TestOne1\"", results);
    assertSubString(" href=\"SuitePage.TestOne\"", results);
    assertSubString("EchoFixture", results);
  }

  @Test
  public void testPageWithXref() throws Exception {
    PageData data = suite.getData();
    data.setContent("!see XrefOne\r\n!see XrefTwo\n!see XrefThree\n");
    suite.commit(data);
    addTestPage(root, "XrefOne", fitPassFixture);
    addTestPage(root, "XrefTwo", fitPassFixture);

    String results = runSuite();
    assertSubString("href=\\\"#XrefOne2\\\"", results);
    assertSubString("href=\\\"#XrefTwo3\\\"", results);
  }

  @Test
  public void testWithTwoTests() throws Exception {
    addTestToSuite("TestTwo", fitFailFixture + "\n" + fitFailFixture);
    String results = runSuite();

    assertSubString("href=\\\"#TestOne1\\\"", results);
    assertSubString("href=\\\"#TestTwo2\\\"", results);
    assertSubString("1 right", results);
    assertSubString("2 wrong", results);
    assertSubString("name=\"TestOne1\"", results);
    assertSubString("name=\"TestTwo2\"", results);
    assertSubString("EchoFixture", results);
    assertSubString("bad", results);

    assertSubString("RerunLastFailures", results);
    assertSubString("Rerun Failed", results);
    File rerunPage = responder.getRerunPageFile();
    assertTrue(rerunPage.exists());
    String rerunPageContent = FileUtil.getFileContent(rerunPage);
    assertSubString("SuitePage.TestTwo", rerunPageContent);
    assertNotSubString("TestOne", rerunPageContent);

    // execute rerun suite
    String rerunPageName = responder.getRerunPageName();
    request = new MockRequest();
    request.setResource(rerunPageName);
    responder = new SuiteResponder();
    suite = WikiPageUtil.addPage(root, PathParser.parse(rerunPageName), rerunPageContent);
    responder.page = suite;

    String rerunresults = runSuite();
    assertSubString("href=\\\"#SuitePage.TestTwo1\\\"", rerunresults);
    assertNotSubString("TestOne", rerunresults);
  }

  @Test
  public void testWithPrunedPage() throws Exception {
    WikiPage pageTwo = addTestToSuite("TestTwo", fitFailFixture + "\n" + fitFailFixture);
    PageData data = pageTwo.getData();
    data.setAttribute("Prune");
    pageTwo.commit(data);
    String results = runSuite();

    assertSubString("href=\\\"#TestOne1\\\"", results);
    assertNotSubString("href=\\\"#TestTwo2\\\"", results);
    assertSubString("1 right", results);
    assertSubString("0 wrong", results);
    assertSubString("name=\"TestOne1\"", results);
    assertNotSubString("id=\"TestTwo2\"", results);
    assertSubString("EchoFixture", results);
    assertNotSubString("bad", results);

    File rerunPage = responder.getRerunPageFile();
    assertFalse(rerunPage.exists());
  }

  @Test
  public void testSuiteWithOneTestWithoutTable() throws Exception {
    addTestToSuite("TestWithoutTable", "This test has not table");
    addTestToSuite("TestTwo", fitPassFixture);
    addTestToSuite("TestThree", fitPassFixture);
    String results = runSuite();

    assertSubString("TestOne", results);
    assertSubString("TestTwo", results);
    assertSubString("TestThree", results);
    assertSubString("TestWithoutTable", results);
  }

  @Test
  public void testExitCodeHeader() throws Exception {
    String results = runSuite();
    assertSubString("Exit-Code: 0", results);
  }

  @Test
  public void exitCodeHeaderIsErrorCount() throws Exception {
    addTestToSuite("TestFailingTest", fitFailFixture);
    String results = runSuite();
    assertSubString("Exit-Code: 1", results);
  }

  @Test
  public void testNoExitCodeHeaderIfNotChunked() throws Exception {
    responder.turnOffChunking();
    String results = runSuite();
    assertFalse(results.contains("Exit-Code: 0"));
  }

  @Test
  public void testExecutionLogLinkAppears() throws Exception {
    String results = runSuite();
    // Lots of escaping: the content is escaped, since it's written from Javascript.
    // Everything needs to be double escaped because it's handled as a regexp.
    assertHasRegexp("class=\\\\\"ok\\\\\">Execution Log", results);
  }

  @Test
  public void testTestSummaryInformationIncludesPageSummary() throws Exception {
    String results = runSuite();
    assertHasRegexp(".*?Test Pages:.*?&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.*?Assertions:.*?", results);
  }

  @Test
  public void testFormatTestSummaryInformation() throws Exception {
    String results = runSuite();
    assertHasRegexp(".*?<strong>Test Pages:</strong>.*?<strong>Assertions:</strong>.*?", results);
  }

  private String classpathWidgets() {
    return "!path classes\n" +
      "!path lib/dummy.jar\n";
  }

  @Test
  public void testNonMatchingSuiteFilter() throws Exception {
    addTestPagesWithSuiteProperty();
    request.setQueryString("suiteFilter=xxx");
    String results = runSuite();
    assertDoesntHaveRegexp(".*href=\\\"#TestOne\\\".*", results);
    assertDoesntHaveRegexp(".*href=\\\"#TestTwo\\\".*", results);
    assertDoesntHaveRegexp(".*href=\\\"#TestThree\\\".*", results);
  }

  @Test
  public void testSimpleMatchingSuiteQuery() throws Exception {
    addTestPagesWithSuiteProperty();
    request.setQueryString("suiteFilter=foo");
    String results = runSuite();
    assertDoesntHaveRegexp(".*href=\\\"#TestOne.*", results);
    assertSubString("href=\\\"#TestTwo1\\\"", results);
    assertDoesntHaveRegexp(".*href=\\\"#TestThree.*", results);
  }

  @Test
  public void testEmptySuiteFilter() throws Exception {
    addTestPagesWithSuiteProperty();
    request.setQueryString("suiteFilter=");
    String results = runSuite();
    assertSubString("href=\\\"#TestTwo3\\\"", results);
    assertSubString("href=\\\"#TestThree2\\\"", results);
  }

  @Test
  public void testSecondMatchingSuiteQuery() throws Exception {
    addTestPagesWithSuiteProperty();
    request.setQueryString("suiteFilter=smoke");
    String results = runSuite();
    assertDoesntHaveRegexp(".*href=\\\"#TestOne.*", results);
    assertDoesntHaveRegexp(".*href=\\\"#TestTwo.*", results);
    assertSubString("href=\\\"#TestThree1\\\"", results);
  }

  @Test
  public void multipleSuiteQuery() throws Exception {
    addTestPagesWithSuiteProperty();
    request.setQueryString("suiteFilter=smoke,foo");
    String results = runSuite();
    assertDoesntHaveRegexp("#TestOne", results);
    assertHasRegexp("#TestTwo", results);
    assertHasRegexp("#TestThree", results);
  }

  @Test
  public void excludeSuiteQuery() throws Exception {
    addTestPagesWithSuiteProperty();
    request.setQueryString("excludeSuiteFilter=foo");
    String results = runSuite();
    assertHasRegexp("#TestOne", results);
    assertDoesntHaveRegexp("#TestTwo", results);
    assertHasRegexp("#TestThree", results);
  }

  @Test
  public void excludeSuiteWithSuiteFilterQuery() throws Exception {
    addTestPagesWithSuiteProperty();
    request.setQueryString("excludeSuiteFilter=bar&suiteFilter=smoke,foo");
    String results = runSuite();
    assertDoesntHaveRegexp("#TestOne", results);
    assertHasRegexp("#TestTwo", results);
    assertDoesntHaveRegexp("#TestThree", results);
  }

  @Test
  public void testFirstTest() throws Exception {
    addTestPagesWithSuiteProperty();
    request.setQueryString("firstTest=TestThree");
    String results = runSuite();
    assertDoesntHaveRegexp("#TestOne", results);
    assertHasRegexp("#TestTwo", results);
    assertHasRegexp("#TestThree", results);
  }

  @Test
  public void testFirstTestWholePath() throws Exception {
    addTestPagesWithSuiteProperty();
    request.setQueryString("firstTest=SuitePage.TestThree");
    String results = runSuite();
    assertDoesntHaveRegexp("#TestOne", results);
    assertHasRegexp("#TestTwo", results);
    assertHasRegexp("#TestThree", results);
  }

  @Test
  public void testTagsShouldBeInheritedFromSuite() throws Exception {
    PageData suiteData = suite.getData();
    suiteData.setAttribute(SUITES, "tag");
    suite.commit(suiteData);
    addTestToSuite("TestInheritsTag", fitPassFixture);

    request.setQueryString("suiteFilter=tag");
    String results = runSuite();
    assertHasRegexp("#TestInheritsTag", results);
  }

  private void addTestPagesWithSuiteProperty() {
    WikiPage test2 = addTestToSuite("TestTwo", fitPassFixture);
    WikiPage test3 = addTestToSuite("TestThree", fitPassFixture);
    PageData data2 = test2.getData();
    PageData data3 = test3.getData();
    data2.setAttribute(SUITES, "foo");
    data3.setAttribute(SUITES, "bar, smoke");
    test2.commit(data2);
    test3.commit(data3);
  }

  @Test
  public void testCanMixSlimAndFitTests() throws Exception {
    addTestToSuite("SlimTest", simpleSlimDecisionTable);
    String results = runSuite();
    assertHasRegexp("EchoFixture", results);
    assertHasRegexp("<td><span class=\"pass\">wow</span></td>", results);
    assertHasRegexp("<h3>slim:in-process", results);
    assertNotSubString("fit:fit.FitServer", results);
  }

  @Test
  public void testGetRerunPageName_withRerunPrefix() throws Exception {
    String rerunPageName = "RerunLastFailures_SuitePage";
    suite = WikiPageUtil.addPage(root, PathParser.parse(rerunPageName), "This is a rerun page\n");
    request.setResource(rerunPageName);
    responder.makeResponse(context, request);

    String result = responder.getRerunPageName();
    assertEquals(rerunPageName, result);
  }

  @Test
  public void testGetRerunPageName_ReplacesPeriod() throws Exception {
    addTestToSuite("TestTwo", fitFailFixture + "\n" + fitFailFixture);
    request.setResource("SuitePage.TestTwo");
    responder.makeResponse(context, request);

    String result = responder.getRerunPageName();
    assertEquals("RerunLastFailures_SuitePage-TestTwo", result);
  }

  @Test
  public void testGetRerunPageName_withoutRerunPrefix() throws Exception {
    request.setResource("SuitePage");
    responder.makeResponse(context, request);

    String result = responder.getRerunPageName();
    assertEquals("RerunLastFailures_SuitePage", result);
  }

  @Test
  public void testGetRerunPageName_withSuiteFilter() throws Exception {
    request.setResource("SuitePage");
    request.setQueryString("suiteFilter=smoke");
    responder.makeResponse(context, request);

    String result = responder.getRerunPageName();
    assertEquals("RerunLastFailures_SuitePage_suiteFilter_smoke", result);
  }

  @Test
  public void testGetRerunPageName_withMultipleSuiteFilters() throws Exception {
    request.setResource("SuitePage");
    request.setQueryString("suiteFilter=smoke,foo");
    responder.makeResponse(context, request);

    String result = responder.getRerunPageName();
    assertEquals("RerunLastFailures_SuitePage_suiteFilter_smoke-foo", result);
  }

  @Test
  public void testGetRerunPageName_withAnyTagFilterParam() throws Exception {
    request.setResource("SuitePage");
    request.setQueryString("runTestsMatchingAnyTag=smoke,foo");
    responder.makeResponse(context, request);

    String result = responder.getRerunPageName();
    assertEquals("RerunLastFailures_SuitePage_suiteFilter_smoke-foo", result);
  }

  @Test
  public void outputIsSentUntilTheResponderCloses() throws Exception {
    RecordingResponder responder = new RecordingResponder();
    MockResponseSender sender = new MockResponseSender();
    responder.attach(sender);
    responder.addToResponse("hello");
    assertSubString("hello", sender.sentData());

    responder.setClosed();
    responder.addToResponse("after");
    assertNotSubString("after", sender.sentData());

    responder = new RecordingResponder();
    responder.attach(new ResponseSender() {
      @Override
      public void send(byte[] bytes) throws IOException {
        throw new IOException("closed");
      }

      @Override
      public void close() {
      }
    });
    responder.addToResponse("boom");
  }

  private static class RecordingResponder extends SuiteResponder {
    void attach(ResponseSender sender) throws Exception {
      response = new ChunkedResponse("text", this);
      Field field = ChunkedResponse.class.getDeclaredField("sender");
      field.setAccessible(true);
      field.set(response, sender);
    }
  }

  private String runSuite() throws Exception {
    Response response = responder.makeResponse(context, request);

    int status = response.getStatus();
    assertEquals(200, status);
    return getResponseContent(response);
  }

  private String getResponseContent(Response response) throws IOException {
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    return sender.sentData();
  }

  public static class FooFormatter extends BaseFormatter {

    private static boolean initialized;

    public FooFormatter() {
      initialized = true;
    }
  }

}
