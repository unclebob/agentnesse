// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;

public class NotFoundResponder implements Responder {
  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    return JsonResponses.afterRemoved(request, 404, "notFound", "No page " + request.getResource());
  }
}
