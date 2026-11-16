// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.editing;

import agnesse.Responder;
import java.io.UnsupportedEncodingException;

import agnesse.AgNesseContext;
import agnesse.html.HtmlUtil;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.html.template.HtmlPage;
import agnesse.html.template.PageTitle;
import agnesse.wiki.MockingPageCrawler;
import agnesse.wiki.PageCrawler;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;

public class EditResponder implements Responder {
  public static final String CONTENT_INPUT_NAME = "pageContent";
  public static final String HELP_TEXT = "helpText";
  public static final String SUITES = "suites";
  public static final String PAGE_TYPE = "pageType";
  public static final String PAGE_NAME = "pageName";

  protected String content;
  protected WikiPage page;
  protected WikiPage root;
  protected PageData pageData;
  protected Request request;

  public EditResponder() {
  }

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    boolean nonExistent = request.hasInput("nonExistent");
    return doMakeResponse(context, request, nonExistent);
  }

  public Response makeResponseForNonExistentPage(AgNesseContext context, Request request) throws UnsupportedEncodingException {
    return doMakeResponse(context, request, true);
  }

  protected Response doMakeResponse(AgNesseContext context, Request request, boolean firstTimeForNewPage) throws UnsupportedEncodingException {
    initializeResponder(context.getRootPage(), request);

    SimpleResponse response = new SimpleResponse();
    String resource = request.getResource();
    WikiPagePath path = PathParser.parse(resource);
    PageCrawler crawler = root.getPageCrawler();

    page = crawler.getPage(path, new MockingPageCrawler());
    pageData = page.getData();
    content = createPageContent();

    String html = doMakeHtml(resource, context, firstTimeForNewPage);

    response.setContent(html);
    response.setMaxAge(0);

    return response;
  }

  protected void initializeResponder(WikiPage root, Request request) {
    this.root = root;
    this.request = request;
  }

  protected String createPageContent() {
    return pageData.getContent();
  }

  private String doMakeHtml(String resource, AgNesseContext context, boolean firstTimeForNewPage) {
    HtmlPage html = context.pageFactory.newPage();
    String title = firstTimeForNewPage ? "Page doesn't exist. Edit: " : "Edit: ";
    html.setTitle(title + resource);

    html.setPageTitle(new PageTitle(title + " Page:", PathParser.parse(resource), pageData.getAttribute(PageData.PropertySUITES)));
    html.setMainTemplate("editPage");
    makeEditForm(html, resource, firstTimeForNewPage, NewPageResponder.getDefaultContent(page));

    return html.html(request);
  }

  private void makeEditForm(HtmlPage html, String resource, boolean firstTimeForNewPage, String defaultNewPageContent) {
    html.put("resource", resource);

    if (request.hasInput("redirectToReferer") && request.hasHeader("Referer")) {
      String redirectUrl = request.getHeader("Referer");
      int questionMarkIndex = redirectUrl.indexOf("?");
      if (questionMarkIndex > 0)
        redirectUrl = redirectUrl.substring(0, questionMarkIndex);
      redirectUrl += "?" + request.getInput("redirectAction");
      html.put("redirect", redirectUrl);
    }

    html.put(HELP_TEXT, pageData.getAttribute(PageData.PropertyHELP));
    html.put("suites", pageData.getAttribute(PageData.PropertySUITES));
    html.put(CONTENT_INPUT_NAME, HtmlUtil.escapeHTML(firstTimeForNewPage ? defaultNewPageContent : content));
  }

}
