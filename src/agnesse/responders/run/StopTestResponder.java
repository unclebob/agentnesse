// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.run;

import agnesse.Responder;
import agnesse.AgNesseContext;
import agnesse.html.template.HtmlPage;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;

public class StopTestResponder implements Responder {

  String testId = null;

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    SimpleResponse response = new SimpleResponse();
    if (request.hasInput("id")) {
      testId = request.getInput("id");
    }

    response.setContent(html(context, request));

    return response;
  }

  private String html(AgNesseContext context, Request request) {
    HtmlPage page = context.pageFactory.newPage();
    page.addTitles("Stopping tests");
    page.put("testId", testId);
    page.put("runningTestingTracker", SuiteResponder.runningTestingTracker);
    page.setMainTemplate("stopTestPage.vm");
    return page.html(request);
  }

}
