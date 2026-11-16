// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.http;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import agnesse.FitNesseContext;
import agnesse.responders.run.SuiteResponder;
import agnesse.testutil.FitNesseUtil;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import util.FileUtil;

public class ExposeThreadingIssueInMockResponseTest {
  private WikiPage root;
  private MockRequest request;
  private SuiteResponder responder;
  private FitNesseContext context;
  private String results;

  @Before
  public void setUp() throws Exception {
    request = new MockRequest();
    responder = new SuiteResponder();
    int port = 9123;
    context = FitNesseUtil.makeTestContext(port);
    root = context.getRootPage();
  }

  public static void assertHasRegexp(String regexp, String output) {
    Matcher match = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL).matcher(output);
    boolean found = match.find();
    if (!found)
      Assert.fail("The regexp <" + regexp + "> was not found in: " + output + ".");
  }

  @Test
  public void testDoSimpleSlimTable() throws Exception {
    doSimpleRun(simpleSlimDecisionTable());
    assertHasRegexp("\"status\":\"pass\"", results);
    assertHasRegexp("wow", results);
  }

  private String simpleSlimDecisionTable() {
    return "" + "|DT:agnesse.slim.test.TestSlim|\n" + "|string|get string arg?|\n"
      + "|wow|wow|\n";
  }

  private void doSimpleRun(String fixtureTable) throws Exception {
    String simpleRunPageName = "TestPage";
    WikiPage testPage = WikiPageUtil.addPage(root, PathParser.parse(simpleRunPageName), classpathWidgets() + fixtureTable);
    request.setResource(testPage.getName());

    Response response = responder.makeResponse(context, request);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    MockResponseSender sender = new MockResponseSender(output);
    sender.doSending(response);

    results = output.toString(FileUtil.CHARENCODING);
  }

  private String classpathWidgets() {
    return "!path classes\n";
  }
}
