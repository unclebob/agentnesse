// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import agnesse.Responder;
import agnesse.AgNesseContext;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;

public class RawContentResponder implements Responder {

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    String resource = request.getResource();
    WikiPagePath path = PathParser.parse(resource);
    WikiPage page = context.getRootPage().getPageCrawler().getPage(path);
    if (page == null)
      return new NotFoundResponder().makeResponse(context, request);
    PageData pageData = page.getData();

    SimpleResponse response = new SimpleResponse();
    response.setMaxAge(0);
    response.setContent(pageData.getContent());

    return response;
  }

}
