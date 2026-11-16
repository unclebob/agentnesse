// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import agnesse.Responder;
import java.util.logging.Level;
import java.util.logging.Logger;

import agnesse.AgNesseContext;
import agnesse.html.template.HtmlPage;
import agnesse.html.template.PageTitle;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;

public class ShutdownResponder implements Responder {
  private static final Logger LOG = Logger.getLogger(ShutdownResponder.class.getName());

  @Override
  public Response makeResponse(final AgNesseContext context, Request request) throws Exception {
    SimpleResponse response = new SimpleResponse();

    HtmlPage html = context.pageFactory.newPage();
    html.setTitle("Shutdown");
    html.setPageTitle(new PageTitle("Shutdown"));

    html.setMainTemplate("shutdownPage.vm");
    response.setContent(html.html(request));

    Thread shutdownThread = new Thread() {
      @Override
      public void run() {
        try {
          context.agNesse.stop();
        }
        catch (Exception e) {
          LOG.log(Level.WARNING, "Error while stopping AgNesse", e);
        }
      }
    };
    shutdownThread.start();

    return response;
  }

}
