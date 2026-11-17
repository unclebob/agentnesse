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
  public void htmlAndAMissingBodyAreNotJson() {
    JsonBody html = JsonBody.parse("HTTP/1.1 200 OK\r\n\r\n<html>Cannot move</html>");
    JsonBody missing = JsonBody.parse(null);

    assertFalse(html.isJson());
    assertFalse(html.contains("Cannot move"));
    assertFalse(html.matches("Cannot"));
    assertFalse(missing.isJson());
    assertFalse(missing.contains(""));
    assertFalse(missing.matches(""));
  }

  @Test
  public void severalEventsStayInOrder() {
    String first = new JSONObject().put("event", "page-done").put("text", "uncle setup").toString();
    String second = new JSONObject().put("event", "page-done").put("text", "hi").toString();

    assertTrue(JsonBody.parse(first + "\n" + second + "\n").matches("uncle setup.*hi"));
    JsonBody chunkedEvents = JsonBody.parse(chunked(first + "\n", second + "\n"));
    assertTrue(chunkedEvents.matches("uncle setup.*hi"));
    assertFalse(chunkedEvents.contains("\r\n"));
  }

  @Test
  public void aJsonObjectMayContainTheHeaderSeparatorAsWhitespace() {
    String json = "{\r\n\r\n\"a\":1}";

    assertTrue(JsonBody.parse("\r\n\r\n" + json).contains("\"a\":1"));
    assertTrue(JsonBody.parse("HTTP/1.1 200 OK\r\n\r\n" + json).contains("\"a\":1"));
  }

  @Test
  public void trailingWhitespaceAfterAnObjectIsStillThatObject() {
    JsonBody json = JsonBody.parse("HTTP/1.1 200 OK\r\n\r\n{\"k\":\"v\"}   ");

    assertTrue(json.contains("\"k\":\"v\""));
    assertFalse(json.contains("\n"));
  }

  @Test
  public void chunkedJsonIsReadFromIncompleteFraming() {
    String event = new JSONObject().put("a", 1).toString();
    String size = hexSize(event);

    assertContainsA1(chunkedHeaders(size + "\r\n" + event + "\r\n"));
    assertContainsA1(chunkedHeaders(size + "\r\n" + event));
    assertContainsA1(chunkedHeaders(size + "\r\n" + event + "\r\n0\r\n"));
    assertContainsA1(chunkedHeaders("1\r\n{\r\n6\r\n\"a\":1}\r\n0\r\n\r\n"));
  }

  @Test
  public void brokenChunkedFramingIsNotJson() {
    String event = new JSONObject().put("a", 1).toString();
    String size = hexSize(event);

    assertFalse(JsonBody.parse(chunkedHeaders("\r\n" + event)).isJson());
    assertFalse(JsonBody.parse(chunkedHeaders("64\r\n" + event)).isJson());
    assertFalse(JsonBody.parse(chunkedHeaders(size + "\r\n" + event + "\r\nGG\r\n")).isJson());
    assertFalse(JsonBody.parse(chunkedHeaders(size + "\r\n" + event + "\r\nNOTHEX")).isJson());
  }

  private static void assertContainsA1(String http) {
    assertTrue(JsonBody.parse(http).contains("\"a\":1"));
  }

  private static String hexSize(String event) {
    return Integer.toHexString(event.getBytes(StandardCharsets.UTF_8).length);
  }

  private static String chunkedHeaders(String body) {
    return "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n" + body;
  }

  private static String chunked(String... events) {
    StringBuilder body = new StringBuilder();
    for (String event : events) {
      body.append(hexSize(event)).append("\r\n").append(event).append("\r\n");
    }
    body.append("0\r\n\r\n");
    return chunkedHeaders(body.toString());
  }
}
