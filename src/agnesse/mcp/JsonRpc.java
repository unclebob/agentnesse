package agnesse.mcp;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * One JSON-RPC 2.0 message, the framing MCP uses on stdio: a single line,
 * with no embedded newline.
 */
final class JsonRpc {
  static final String VERSION = "2.0";

  private JsonRpc() {
  }

  static Message read(String line) {
    String trimmed = line.trim();
    if (trimmed.startsWith("[")) {
      return Message.bad(null, -32600, "Invalid Request");
    }
    JSONObject json = object(trimmed);
    if (json == null) {
      return Message.bad(null, -32700, "Parse error");
    }
    Object id = id(json);
    if (id == JSONObject.NULL) {
      return Message.bad(null, -32600, "Invalid Request");
    }
    if (!VERSION.equals(json.optString("jsonrpc"))) {
      return Message.bad(id, -32600, "Invalid Request");
    }
    String method = method(json);
    if (method == null) {
      return Message.bad(id, -32600, "Invalid Request");
    }
    JSONObject params = params(json);
    if (params == null) {
      return Message.bad(id, -32602, "Invalid params");
    }
    return Message.ok(id, method, params, id == null);
  }

  static String result(Object id, JSONObject result) {
    return envelope(id).put("result", result).toString();
  }

  static String error(Object id, int code, String message) {
    JSONObject body = new JSONObject().put("code", code).put("message", message);
    return envelope(id).put("error", body).toString();
  }

  private static JSONObject envelope(Object id) {
    JSONObject message = new JSONObject();
    message.put("jsonrpc", VERSION);
    message.put("id", id == null ? JSONObject.NULL : id);
    return message;
  }

  private static JSONObject object(String line) {
    try {
      Object value = new org.json.JSONTokener(line).nextValue();
      if (value instanceof JSONObject json) {
        return json;
      }
      return null;
    } catch (JSONException e) {
      return null;
    }
  }

  private static Object id(JSONObject json) {
    if (!json.has("id")) {
      return null;
    }
    return json.get("id");
  }

  private static String method(JSONObject json) {
    return stringField(json, "method");
  }

  private static JSONObject params(JSONObject json) {
    return objectField(json, "params");
  }

  static String stringField(JSONObject json, String key) {
    if (!json.has(key)) {
      return null;
    }
    Object value = json.get(key);
    if (!(value instanceof String text) || text.isBlank()) {
      return null;
    }
    return text;
  }

  static JSONObject objectField(JSONObject json, String key) {
    if (!json.has(key) || json.isNull(key)) {
      return new JSONObject();
    }
    Object value = json.get(key);
    if (value instanceof JSONObject object) {
      return object;
    }
    return null;
  }

  static final class Message {
    final Object id;
    final String method;
    final JSONObject params;
    final boolean notification;
    final Integer code;
    final String error;

    private Message(Object id, String method, JSONObject params, boolean notification, Integer code, String error) {
      this.id = id;
      this.method = method;
      this.params = params;
      this.notification = notification;
      this.code = code;
      this.error = error;
    }

    static Message ok(Object id, String method, JSONObject params, boolean notification) {
      return new Message(id, method, params, notification, null, null);
    }

    static Message bad(Object id, int code, String error) {
      return new Message(id, null, new JSONObject(), false, code, error);
    }

    boolean failed() {
      return error != null;
    }
  }
}
