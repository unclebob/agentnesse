package fitnesse.responders;

import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

/** JSON error bodies for callers that ask for JSON. */
public final class JsonResponses {
  private JsonResponses() {
  }

  public static boolean wantsJson(Request request) {
    if (request == null) {
      return false;
    }
    if ("json".equalsIgnoreCase(request.getInput("format"))) {
      return true;
    }
    String accept = request.getHeader("Accept");
    return accept != null && accept.toLowerCase(Locale.ROOT).contains("application/json");
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
