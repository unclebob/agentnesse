// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.run.slimResponder;

import java.io.IOException;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.html.HtmlUtil;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.html.template.HtmlPage;
import agnesse.testrunner.WikiPageDescriptor;
import agnesse.testrunner.WikiTestPage;
import agnesse.testsystems.*;
import agnesse.testsystems.slim.SlimTestSystem;
import agnesse.wiki.PageCrawler;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;

/*
This responder is a test rig for SlimTestSystemTest, which makes sure that the SlimTestSystem works nicely with
responders in general.
*/
public abstract class SlimResponder implements Responder, TestSystemListener {
  protected boolean fastTest = false;
  SlimTestSystem testSystem;
  private WikiPage page;
  private AgNesseContext context;
  private Throwable slimException;
  private StringBuilder output;
  private TestSummary testSummary;

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    this.context = context;
    loadPage(request.getResource(), context);

    SimpleResponse response = new SimpleResponse();
    HtmlPage html = context.pageFactory.newPage();
    html.setMainTemplate("render.vm");
    html.put("content", new SlimRenderer());
    response.setContent(html.html(request));
    return response;
  }

  protected void loadPage(String pageName, AgNesseContext context) {
    WikiPagePath path = PathParser.parse(pageName);
    PageCrawler crawler = context.getRootPage().getPageCrawler();
    page = crawler.getPage(path);
  }

  public AgNesseContext getContext() {
    return context;
  }

  public WikiPage getPage() {
    return page;
  }

  protected Descriptor getDescriptor() {
    return new WikiPageDescriptor(page, true, false, "");
  }

  public class SlimRenderer {

    public String render() {

      try {
        output = new StringBuilder(512);
        testSystem = getTestSystem();
        testSystem.start();
        testSystem.runTests(new WikiTestPage(page));
      } catch (Exception e) {
        slimException = e;
      } finally {
        try {
          if (testSystem != null) testSystem.bye();
        } catch (Exception e) {
          if (slimException == null) {
            slimException = e;
          }
        }
      }
      String exceptionString = "";
      if (slimException != null) {
        exceptionString = String.format("<div class='error'>%s</div>", HtmlUtil.escapeHTML(slimException.getMessage()));
      }
      return exceptionString + output.toString();
    }
  }

  protected abstract SlimTestSystem getTestSystem() throws IOException;

  public TestSummary getTestSummary() {
    return testSummary;
  }

  protected void setFastTest(boolean fastTest) {
    this.fastTest = fastTest;
  }

  @Override
  public void testSystemStarted(TestSystem testSystem) {
  }

  @Override
  public void testOutputChunk(TestPage testPage, String output) {
    this.output.append(output);
  }

  @Override
  public void testStarted(TestPage testPage) {
    //
  }

  @Override
  public void testComplete(TestPage testPage, TestSummary testSummary)  {
    this.testSummary = testSummary;
  }

  @Override
  public void testSystemStopped(TestSystem testSystem, Throwable throwable) {
    slimException = throwable;
  }

  @Override
  public void testAssertionVerified(Assertion assertion, TestResult testResult) {
  }

  @Override
  public void testExceptionOccurred(Assertion assertion, ExceptionResult exceptionResult) {
  }
}

