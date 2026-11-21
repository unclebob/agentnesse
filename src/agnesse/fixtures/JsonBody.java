package agnesse.fixtures;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * The JSON document or the NDJSON events in an HTTP response.
 * Chunk framing and headers are not part of the document.
 */
public final class JsonBody {
  private final String text;

  private JsonBody(String text) {
    this.text = text;
  }

  public static JsonBody parse(String http) {
    if (http == null) {
      return new JsonBody(null);
    }
    String body = new String(payload(http.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    return new JsonBody(document(body));
  }

  public boolean isJson() {
    return text != null;
  }

  public String text() {
    return text;
  }

  public boolean contains(String fragment) {
    return text != null && fragment != null && text.contains(fragment);
  }

  public boolean matches(String pattern) {
    if (text == null || pattern == null) {
      return false;
    }
    return Pattern.compile(pattern, Pattern.DOTALL).matcher(text).find();
  }

  private static byte[] payload(byte[] http) {
    String text = new String(http, StandardCharsets.UTF_8);
    String[] parts = text.split("\r\n\r\n", 2);
    if (parts.length == 1) {
      return http;
    }
    byte[] body = parts[1].getBytes(StandardCharsets.UTF_8);
    if (parts[0].toLowerCase().contains("transfer-encoding: chunked")) {
      return dechunk(body);
    }
    return body;
  }

  private static byte[] dechunk(byte[] body) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    String text = new String(body, StandardCharsets.US_ASCII);
    int cursor = 0;
    while (cursor < body.length) {
      int lineEnd = text.indexOf("\r\n", cursor);
      int sizeEnd = lineEnd < 0 ? text.length() : lineEnd;
      String digits = text.substring(cursor, sizeEnd).trim();
      if (digits.isEmpty()) {
        break;
      }
      int size;
      try {
        size = Integer.parseInt(digits, 16);
      } catch (NumberFormatException e) {
        return body;
      }
      if (size == 0) {
        break;
      }
      int start = sizeEnd + 2;
      if (start + size > body.length) {
        return body;
      }
      out.write(body, start, size);
      cursor = start + size + 2;
    }
    return out.toByteArray();
  }

  private static String document(String body) {
    if (body.trim().isEmpty()) {
      return null;
    }
    try {
      JSONTokener tokener = new JSONTokener(body);
      JSONObject object = new JSONObject(tokener);
      if (onlyWhitespaceRemains(tokener)) {
        return object.toString();
      }
    } catch (JSONException ignored) {
      // Several events, one object per line.
    }
    return lines(body);
  }

  private static boolean onlyWhitespaceRemains(JSONTokener tokener) {
    while (tokener.more()) {
      if (Character.isWhitespace(tokener.next())) {
        continue;
      }
      return false;
    }
    return true;
  }

  private static String lines(String body) {
    StringBuilder text = new StringBuilder();
    for (String line : body.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      try {
        text.append(new JSONObject(trimmed).toString()).append('\n');
      } catch (JSONException ignored) {
        return null;
      }
    }
    String document = text.toString();
    return document.isEmpty() ? null : document;
  }
}
