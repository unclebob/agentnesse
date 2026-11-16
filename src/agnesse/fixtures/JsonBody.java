package agnesse.fixtures;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
    return new JsonBody(document(body.trim()));
  }

  public boolean isJson() {
    return text != null;
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
    int split = indexOf(http, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), 0);
    if (split < 0) {
      return http;
    }
    String headers = new String(http, 0, split, StandardCharsets.UTF_8).toLowerCase();
    byte[] body = copy(http, split + 4, http.length);
    if (headers.contains("transfer-encoding: chunked")) {
      return dechunk(body);
    }
    return body;
  }

  private static byte[] dechunk(byte[] body) {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    int cursor = 0;
    while (cursor < body.length) {
      int lineEnd = indexOf(body, new byte[] {'\r', '\n'}, cursor);
      if (lineEnd < 0) {
        return body;
      }
      int size = hex(body, cursor, lineEnd);
      if (size < 0) {
        return body;
      }
      if (size == 0) {
        break;
      }
      int start = lineEnd + 2;
      if (start + size > body.length) {
        return body;
      }
      out.write(body, start, size);
      cursor = start + size + 2;
    }
    return out.toByteArray();
  }

  private static String document(String body) {
    if (body.isEmpty()) {
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
      if (!Character.isWhitespace(tokener.next())) {
        return false;
      }
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
    return text.length() == 0 ? null : text.toString();
  }

  private static int hex(byte[] body, int start, int end) {
    try {
      return Integer.parseInt(new String(body, start, end - start, StandardCharsets.US_ASCII).trim(), 16);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private static int indexOf(byte[] data, byte[] needle, int from) {
    for (int i = from; i + needle.length <= data.length; i++) {
      if (matches(data, needle, i)) {
        return i;
      }
    }
    return -1;
  }

  private static boolean matches(byte[] data, byte[] needle, int at) {
    for (int i = 0; i < needle.length; i++) {
      if (data[at + i] != needle[i]) {
        return false;
      }
    }
    return true;
  }

  private static byte[] copy(byte[] data, int start, int end) {
    byte[] slice = new byte[end - start];
    System.arraycopy(data, start, slice, 0, slice.length);
    return slice;
  }
}
