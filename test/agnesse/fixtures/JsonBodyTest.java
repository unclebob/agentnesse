package agnesse.fixtures;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonBodyTest {

  @Test
  public void aChunkedEventStreamIsTheJsonNotTheFraming() {
    String event = new JSONObject().put("event", "page-done").put("status", "fail").put("assertions", "0 right, 1 wrong, 0 ignored, 0 exceptions").toString();
    JsonBody json = JsonBody.parse(chunked(event));

    assertTrue(json.isJson());
    assertTrue(json.contains("\"status\":\"fail\""));
    assertTrue(json.contains("0 right, 1 wrong, 0 ignored, 0 exceptions"));
    assertFalse(json.contains("\r\n"));
  }

  @Test
  public void aPrettyErrorObjectIsJson() {
    String http = "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\n\r\n"
      + new JSONObject().put("status", 400).put("error", "error").put("message", "Cannot move").toString(2);
    JsonBody json = JsonBody.parse(http);

    assertTrue(json.contains("Cannot move"));
    assertTrue(json.contains("\"error\":\"error\""));
    assertTrue(json.matches("Cannot move"));
  }

  @Test
  public void htmlIsNotJson() {
    JsonBody json = JsonBody.parse("HTTP/1.1 200 OK\r\n\r\n<html>Cannot move</html>");

    assertFalse(json.isJson());
    assertFalse(json.contains("Cannot move"));
    assertFalse(json.matches("Cannot"));
  }

  @Test
  public void severalEventsStayInOrder() {
    String first = new JSONObject().put("event", "page-done").put("text", "uncle setup").toString();
    String second = new JSONObject().put("event", "page-done").put("text", "hi").toString();
    JsonBody json = JsonBody.parse(first + "\n" + second + "\n");

    assertTrue(json.matches("uncle setup.*hi"));
  }

  @Test
  public void aMissingBodyIsNotJson() {
    JsonBody json = JsonBody.parse(null);

    assertFalse(json.isJson());
    assertFalse(json.contains(""));
    assertFalse(json.matches(""));
  }

  private static String chunked(String event) {
    byte[] bytes = event.getBytes(StandardCharsets.UTF_8);
    String size = Integer.toHexString(bytes.length);
    return "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n"
      + size + "\r\n" + event + "\r\n0\r\n\r\n";
  }
}
