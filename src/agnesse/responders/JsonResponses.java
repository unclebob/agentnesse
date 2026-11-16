package agnesse.responders;

import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** JSON error bodies. These endpoints return JSON without an Accept header or format argument. */
public final class JsonResponses {
  private JsonResponses() {
  }

  private static final String[] REMOVED = {
    "ShowChildCount", "jsonp", "searchType", "isMethodSearch", "searchString"
  };

  /** Returns a 400 when the request still carries a parameter these endpoints no longer accept. */
  public static SimpleResponse removedParameter(Request request) throws UnsupportedEncodingException {
    for (String name : REMOVED) {
      if (request.hasInput(name)) {
        return error(400, "badRequest", "Unknown parameter " + name);
      }
    }
    return null;
  }

  /** Returns a 400 when the request carries a parameter this endpoint no longer accepts. */
  public static SimpleResponse unknownParameter(Request request, String... allowed) throws UnsupportedEncodingException {
    Set<String> ok = new HashSet<>(Arrays.asList(allowed));
    ok.add("responder");
    ok.add(Request.NOCHUNK);
    for (String key : request.getMap().keySet()) {
      if (!ok.contains(key)) {
        return error(400, "badRequest", "Unknown parameter " + key);
      }
    }
    return null;
  }

  public static Response afterRemoved(Request request, int status, String error, String message)
      throws UnsupportedEncodingException {
    Response rejected = removedParameter(request);
    if (rejected != null) {
      return rejected;
    }
    return error(status, error, message);
  }

  public static SimpleResponse error(int status, String error, String message) throws UnsupportedEncodingException {
    SimpleResponse response = new SimpleResponse(status);
    response.setContentType(Response.Format.JSON);
    response.setContent(new JSONObject()
      .put("status", status)
      .put("error", error)
      .put("message", message == null ? "" : message)
      .toString(2));
    return response;
  }
}
