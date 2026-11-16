package agnesse.responders;

import java.util.List;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.http.Request;
import agnesse.http.Response;

public class FilteringResponder implements Responder {
  private final List<Responder> filters;
  private final Responder responder;

  public FilteringResponder(List<Responder> filters, Responder responder) {
    this.filters = filters;
    this.responder = responder;
  }

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    Response response;
    for (Responder filter : filters) {
      response = filter.makeResponse(context, request);
      if (response != null) {
        return response;
      }
    }
    return responder.makeResponse(context, request);
  }
}
