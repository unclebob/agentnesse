// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.editing;

import agnesse.Responder;
import agnesse.AgNesseContext;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.markdown.FrontMatter;
import agnesse.wiki.*;

public class SaveResponder implements Responder {

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    if (request.hasInput(EditResponder.FRONT_MATTER)) {
      return saveFrontMatter(context, request);
    }
    return saveLegacy(context, request);
  }

  private Response saveLegacy(AgNesseContext context, Request request) {
    WikiPage page = getPage(request.getResource(), context);
    PageData data = page.getData();
    setData(data, request.getInput(EditResponder.CONTENT_INPUT_NAME), request.getInput(EditResponder.HELP_TEXT),
      request.getInput(EditResponder.SUITES), request.getAuthorizationUsername());
    return commitAndRedirect(request, page, data);
  }

  private Response saveFrontMatter(AgNesseContext context, Request request) throws Exception {
    String json = request.getInput(EditResponder.FRONT_MATTER);
    final FrontMatter matter;
    try {
      matter = FrontMatter.readSubmitted(json);
    } catch (IllegalArgumentException e) {
      return new EditResponder().makeErrorResponse(context, request, e.getMessage());
    }
    WikiPage page = getPage(request.getResource(), context);
    PageData data = page.getData();
    data.setContent(request.getInput(EditResponder.CONTENT_INPUT_NAME));
    FrontMatter.keepOrApply(data, matter, json, page.getName());
    return commitAndRedirect(request, page, data);
  }

  private Response commitAndRedirect(Request request, WikiPage page, PageData data) {
    Response response = new SimpleResponse();
    VersionInfo commitRecord = page.commit(data);
    if (commitRecord != null) {
      response.addHeader("Current-Version", commitRecord.getName());
    }
    if (request.hasInput("redirect"))
      response.redirect(request.getInput("redirect"));
    else
      response.redirect(request.getResource());
    return response;
  }

  private WikiPage getPage(String resource, AgNesseContext context) {
    WikiPagePath path = PathParser.parse(resource);
    PageCrawler pageCrawler = context.getRootPage().getPageCrawler();
    WikiPage page = pageCrawler.getPage(path);
    if (page == null)
      page = WikiPageUtil.addPage(context.getRootPage(), PathParser.parse(resource));
    return page;
  }

  private void setData(final PageData data, final String savedContent, final String helpText, final String suites, String user) {
    data.setContent(savedContent);
    data.setOrRemoveAttribute(PageData.PropertyHELP, helpText);
    data.setOrRemoveAttribute(PageData.PropertySUITES, suites);
    data.setOrRemoveAttribute(PageData.LAST_MODIFYING_USER, user);
  }

}
