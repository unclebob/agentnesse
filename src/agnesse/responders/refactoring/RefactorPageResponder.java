// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.refactoring;

import agnesse.Responder;
import agnesse.AgNesseContext;
import agnesse.html.template.HtmlPage;
import agnesse.html.template.PageTitle;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.util.StringUtils;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;
import agnesse.wiki.WikiPageProperty;

public class RefactorPageResponder implements Responder {

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    String resource = request.getResource();
    String tags = "";
    WikiPage wikiPage = null;
    if(context.getRootPage() != null){
      WikiPagePath path = PathParser.parse(resource);
      wikiPage = context.getRootPage().getPageCrawler().getPage(path);
      if(wikiPage != null) {
        PageData pageData = wikiPage.getData();
        tags = pageData.getAttribute(WikiPageProperty.SUITES);
      }
    }

    HtmlPage page = context.pageFactory.newPage();
    String type = request.getInput("type");

    page.setMainTemplate("refactorForm");
    page.setTitle("Refactor: " + resource);
    page.setPageTitle(new PageTitle("Refactor", PathParser.parse(resource), tags));
    page.put("refactoredRootPage", resource);
    page.put("request", request);
    page.put("type", type);
    page.put("viewLocation", request.getResource());
    if ("move".equals(type)) {
      String rootPath = context.getProperty("refactorStartPath");
      page.put("rootpath", StringUtils.isBlank(rootPath) ? "ROOT" : rootPath);
    }
    SimpleResponse response = new SimpleResponse();
    response.setContent(page.html(request));
    return response;
  }

}
