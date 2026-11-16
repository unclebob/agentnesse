// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.editing;

import agnesse.Responder;
import agnesse.AgNesseContext;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.wiki.*;

public class SaveResponder implements Responder {

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    String resource = request.getResource();
    WikiPage page = getPage(resource, context);
    return saveEdits(context, request, page);
  }

  private Response saveEdits(AgNesseContext context, Request request, WikiPage page) {
    String savedContent = request.getInput(EditResponder.CONTENT_INPUT_NAME);
    String helpText = request.getInput(EditResponder.HELP_TEXT);
    String suites = request.getInput(EditResponder.SUITES);
    String user = request.getAuthorizationUsername();
    PageData data = page.getData();

    Response response = new SimpleResponse();
    setData(data, savedContent, helpText, suites, user);
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
