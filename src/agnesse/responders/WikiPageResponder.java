// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import agnesse.Responder;
import agnesse.AgNesseContext;
import agnesse.html.template.HtmlPage;
import agnesse.html.template.PageTitle;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.responders.editing.EditResponder;
import agnesse.testrunner.TestPageWithSuiteSetUpAndTearDown;
import agnesse.testrunner.WikiTestPage;
import agnesse.testrunner.WikiTestPageUtil;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;
import agnesse.wiki.WikiPageUtil;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class WikiPageResponder implements Responder {

  private Request request;

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    this.request = request;
    WikiPage page = loadPage(context, request.getResource(), request.getMap());
    if (page == null)
      return notFoundResponse(context, request);
    else
      return makePageResponse(context, page);
  }

  protected WikiPage loadPage(AgNesseContext context, String pageName, Map<String,String> inputs) {
    return context.getPage(pageName, inputs);
  }

  private Response notFoundResponse(AgNesseContext context, Request request) throws Exception {
    if (dontCreateNonExistentPage(request))
      return new NotFoundResponder().makeResponse(context, request);
    return new EditResponder().makeResponseForNonExistentPage(context, request);
  }

  private boolean dontCreateNonExistentPage(Request request) {
    String dontCreate = request.getInput("dontCreatePage");
    return dontCreate != null && (dontCreate.isEmpty() || Boolean.parseBoolean(dontCreate));
  }

  private SimpleResponse makePageResponse(AgNesseContext context, WikiPage page) throws UnsupportedEncodingException {
      String html = makeHtml(context, page);

      SimpleResponse response = new SimpleResponse();
      response.setMaxAge(0);
      response.setContent(html);
      return response;
  }

  public String makeHtml(AgNesseContext context, WikiPage page) {
    PageData pageData = page.getData();
    HtmlPage html = context.pageFactory.newPage();
    WikiPagePath fullPath = page.getFullPath();
    String fullPathName = PathParser.render(fullPath);
    PageTitle pt = new PageTitle(fullPath);

    String tags = pageData.getAttribute(PageData.PropertySUITES);

    pt.setPageTags(tags);

    html.setTitle(fullPathName);
    html.setPageTitle(pt.notLinked());

    html.setNavTemplate("wikiNav.vm");
    html.put("actions", new WikiPageActions(page));
    html.put("helpText", pageData.getProperties().get(PageData.PropertyHELP));

    if (WikiPageUtil.isTestPage(page)) {
      // Add test url inputs to context's variableSource.
      WikiTestPage testPage = new TestPageWithSuiteSetUpAndTearDown(page);
      html.put("content", new WikiTestPageRenderer(testPage));
    } else {
      html.put("content", new WikiPageRenderer(page));
    }

    html.setMainTemplate("wikiPage");
    html.setFooterTemplate("wikiFooter");
    html.put("footerContent", new WikiPageFooterRenderer(page));
    return html.html(request);
  }

  public static class WikiPageRenderer {
    private WikiPage page;

    public WikiPageRenderer(WikiPage page){
      this.page = page;
    }

    public String render() {
        return WikiPageUtil.makePageHtml(page);
    }
  }

  public static class WikiTestPageRenderer {
    private WikiTestPage page;

    public WikiTestPageRenderer(WikiTestPage page){
      this.page = page;
    }

    public String render() {
      return WikiTestPageUtil.makePageHtml(page);
    }
  }

  public class WikiPageFooterRenderer {
    private WikiPage page;

    public WikiPageFooterRenderer(WikiPage page){
      this.page = page;
    }

    public String render() {
        return WikiPageUtil.getFooterPageHtml(page);
    }
  }

}
