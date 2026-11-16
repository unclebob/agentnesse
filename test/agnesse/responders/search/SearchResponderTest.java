// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.search;

import agnesse.FitNesseContext;
import agnesse.http.MockRequest;
import agnesse.http.MockResponseSender;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.testutil.FitNesseUtil;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static util.RegexTestCase.assertDoesntHaveRegexp;
import static util.RegexTestCase.assertHasRegexp;
import static util.RegexTestCase.assertSubString;

public class SearchResponderTest {
  private SearchResponder responder;
  private MockRequest request;
  private FitNesseContext context;

  @Before
  public void setUp() throws Exception {
    context = FitNesseUtil.makeTestContext();
    WikiPage somePage = WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("SomePage"), "has something in it");

    WikiPageUtil.addPage(somePage, PathParser.parse("SomeTest"), "test page content");
    WikiPageUtil.addPage(somePage, PathParser.parse("SomeSuite"), "suite page content");
    request = new MockRequest();
    request.addInput("q", "blah");
    request.addInput("type", "text");
    responder = new SearchResponder();
  }

  @Test
  public void testHtmlWithMethodWithAllScope() throws Exception {
    WikiPage somePage = context.getRootPage().getChildPage("SomePage");
    WikiPageUtil.addPage(somePage, PathParser.parse("PageOne"), "has PageOne content");
    WikiPageUtil.addPage(somePage, PathParser.parse("PageOne.PageOneChild"),
      "|Some method|");
    WikiPageUtil.addPage(somePage, PathParser.parse("PageTwo"), "has PageTwo content");
    WikiPageUtil.addPage(somePage, PathParser.parse("PageTwo.PageTwoChild"),
      "|Some method|");

    String content = getResponseContentUsingMethodName("Some method", "root");

    assertHasRegexp("SomePage.PageOne.PageOneChild", content);
    assertHasRegexp("SomePage.PageTwo.PageTwoChild", content);
  }

  @Test
  public void testHtmlWithMethodWithDefinedScope() throws Exception {
    WikiPage somePage = context.getRootPage().getChildPage("SomePage");
    WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Test1"), "|Some method|");
    WikiPage suite11 = WikiPageUtil.addPage(somePage, PathParser.parse("Suite11"), "some content");
    WikiPageUtil.addPage(suite11, PathParser.parse("Suite11Test1"), "|Some method|");

    String content = getResponseContentUsingMethodName("Some method", "Suite11");

    assertHasRegexp("SomePage.Suite11.Suite11Test1", content);
    //Exception is thrown if page is not formed properly. Below assert ensures, the page content is valid one.
    assertDoesntHaveRegexp("NullPointerException", content);
    assertDoesntHaveRegexp("SomePage.Test1", content);
  }

  @Test
  public void testHtmlWithMethod() throws Exception {

    WikiPage somePage = context.getRootPage().getChildPage("SomePage").getChildPage("SomeSuite");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test1"), "|Some method|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test2"), "|Some           method|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test3"), "|ensure|Some method|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test4"), "|reject|Some method|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test5"), "|show|Some method|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test6"), "|note|Some method|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test7"), "|check|Some method|checkValue|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test8"), "|check not|Some method|checkNotValue|");

    WikiPageUtil.addPage(somePage, PathParser.parse("Test11"), "|Some methods|");

    String content = getResponseContentUsingMethodName("Some method", "");

    assertHasRegexp("Test1", content);
    assertHasRegexp("Test2", content);
    assertHasRegexp("Test3", content);
    assertHasRegexp("Test4", content);
    assertHasRegexp("Test5", content);
    assertHasRegexp("Test6", content);
    assertHasRegexp("Test7", content);
    assertHasRegexp("Test8", content);
    assertDoesntHaveRegexp("Test11", content);
  }

  @Test
  public void testHtmlWithMethodWithOneParameter() throws Exception {

    WikiPage somePage = context.getRootPage().getChildPage("SomePage").getChildPage("SomeSuite");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test1"), "|Method with|one|param|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test2"), "|Method     with   |some|   param|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test3"), "|ensure|Method with|1|param|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test4"), "|reject|Method with|1|param|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test5"), "|show|Method with|1|param|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test6"), "|note|Method with|1|param|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test7"), "|check|Method with|1|param|checkValue|");
    WikiPageUtil.addPage(somePage, PathParser.parse("Test8"), "|check not|Method with|1|param|checkNotValue|");

    WikiPageUtil.addPage(somePage, PathParser.parse("Test11"), "|Method with params|");

    String content = getResponseContentUsingMethodName("Method with param", "root");

    assertHasRegexp("Test1", content);
    assertHasRegexp("Test2", content);
    assertHasRegexp("Test3", content);
    assertHasRegexp("Test4", content);
    assertHasRegexp("Test5", content);
    assertHasRegexp("Test6", content);
    assertHasRegexp("Test7", content);
    assertHasRegexp("Test8", content);
    assertDoesntHaveRegexp("Test11", content);
  }

  @Test
  public void testHtml() throws Exception {
    String content = getResponseContentUsingSearchString("something");

    assertHasRegexp("something", content);
    assertHasRegexp("SomePage", content);
  }

  @Test
  public void testNoSearchStringBringsUpNoResults() throws Exception {
    String content = getResponseContentUsingSearchString("");
    assertSubString("\"pages\": []", content);
  }

  @Test
  public void unknownSearchTypeIsRejected() throws Exception {
    request.addInput("type", "nope");
    request.addInput("q", "something");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals(400, response.getStatus());
    assertSubString("Unknown search type nope", response.getContent());
  }

  private String getResponseContentUsingSearchString(String searchString) throws Exception {
    request.addInput("q", searchString);
    request.addInput("type", "text");
    request.addInput(Request.NOCHUNK, "");
    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    return sender.sentData();
  }

  private String getResponseContentUsingMethodName(String searchString, String searchScope) throws Exception {
    request.addInput("q", searchString);
    request.addInput("type", "method");
    request.addInput("searchScope", searchScope);
    if (!(searchScope.equals("root") || searchScope.isEmpty())) {
      //This is specific to test data defined in testHtmlWithMethodWithDefinedScope when specific option is selected.
      request.setResource("SomePage.Suite11.Suite11Test1");
    }
    request.addInput(Request.NOCHUNK, "");
    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    return sender.sentData();
  }

}
