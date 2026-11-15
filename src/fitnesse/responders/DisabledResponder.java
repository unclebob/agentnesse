package fitnesse.responders;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;

public class DisabledResponder implements Responder {
  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    return JsonResponses.afterRemoved(request, 403, "disabled", "This responder is disabled");
  }
}
