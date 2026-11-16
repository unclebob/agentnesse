// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse;

import agnesse.http.Request;
import agnesse.http.Response;

public interface Responder {
  Response makeResponse(FitNesseContext context, Request request) throws Exception;
}
