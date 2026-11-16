// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import agnesse.FitNesseContext;
import agnesse.Responder;
import agnesse.http.Request;
import agnesse.http.Response;

public class ErrorResponder implements Responder {
  private final Exception exception;
  private final String message;
  private final int statusCode;

  public ErrorResponder(Exception e) {
    this(e, null, 400);
  }

  public ErrorResponder(String message) {
    this(message, 400);
  }

  public ErrorResponder(String message, int statusCode) {
    this(null, message, statusCode);
  }

  private ErrorResponder(Exception exception, String message, int statusCode) {
    this.exception = exception;
    this.message = message;
    this.statusCode = statusCode;
  }

  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    String text = message != null ? message : (exception == null ? "" : exception.toString());
    return JsonResponses.afterRemoved(request, statusCode, "error", text);
  }

  public static String makeExceptionString(Throwable e) {
    StringBuilder builder = new StringBuilder();
    builder.append(e.toString()).append("\n");
    for (StackTraceElement stackTraceElement : e.getStackTrace()) {
      builder.append("\t").append(stackTraceElement).append("\n");
    }
    return builder.toString();
  }
}
