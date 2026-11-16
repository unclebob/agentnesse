package agnesse.responders;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.http.Request;
import agnesse.http.Response;

public class DisabledResponder implements Responder {
  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    return JsonResponses.afterRemoved(request, 403, "disabled", "This responder is disabled");
  }
}
