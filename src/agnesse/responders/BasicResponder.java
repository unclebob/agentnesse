// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import java.io.IOException;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.html.template.HtmlPage;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;

public class BasicResponder implements Responder {

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    WikiPage requestedPage = getRequestedPage(request, context);

    Response response;
    if (requestedPage == null)
      response = pageNotFoundResponse(context, request);
    else
      response = responseWith(contentFrom(context, request, requestedPage));

    return response;
  }

  protected HtmlPage prepareResponseDocument(AgNesseContext context) {
    HtmlPage responseDocument = context.pageFactory.newPage();
    responseDocument.addTitles("Default Responder");
    responseDocument.setMainTemplate("defaultPage.vm");
    return responseDocument;
  }

  protected WikiPage getRequestedPage(Request request, AgNesseContext context) {
    WikiPagePath path = PathParser.parse(request.getResource());
    WikiPage requestedPage = context.getRootPage().getPageCrawler().getPage(path);
    return requestedPage;
  }

  protected String contentFrom(AgNesseContext context, Request request, WikiPage requestedPage) {
    return prepareResponseDocument(context).html(request);
  }

  protected Response pageNotFoundResponse(AgNesseContext context, Request request) throws Exception {
    return new NotFoundResponder().makeResponse(context, request);
  }

  protected Response responseWith(String content) throws IOException {
    SimpleResponse response = new SimpleResponse();
    response.setContentType(getContentType());
    response.setContent(content);
    return response;
  }

  protected String getContentType() {
    return Response.Format.HTML.getContentType();
  }

}
