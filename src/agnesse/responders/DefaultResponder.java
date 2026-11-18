// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.http.Request;
import agnesse.http.Response;

public class DefaultResponder implements Responder {
  @Override public Response makeResponse(AgNesseContext context, Request request) throws Exception { return JsonResponses.afterRemoved(request, 404, "default", "No responder for " + request.getResource()); }
}
