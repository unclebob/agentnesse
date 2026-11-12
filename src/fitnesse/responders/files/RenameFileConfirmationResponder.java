// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.files;

import fitnesse.Responder;
import fitnesse.FitNesseContext;
import fitnesse.html.template.HtmlPage;
import fitnesse.html.template.PageTitle;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;

public class RenameFileConfirmationResponder implements Responder {

  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    String resource = request.getResource();
    String filename = request.getInput("filename");
    
    HtmlPage page = context.pageFactory.newPage();
    page.setTitle("Rename " + filename);
    page.setPageTitle(new PageTitle("Rename File", resource + filename, "/"));
    page.setMainTemplate("renameFileConfirmation");
    page.put("filename", filename);
    page.put("resource", resource);

    SimpleResponse response = new SimpleResponse();
    response.setContent(page.html(request));
    return response;
  }

}
