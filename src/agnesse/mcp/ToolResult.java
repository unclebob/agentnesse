package agnesse.mcp;

import org.json.JSONArray;
import org.json.JSONObject;

/** What {@code tools/call} returns to the agent. */
final class ToolResult {
  private final String text;
  private final boolean error;

  private ToolResult(String text, boolean error) {
    this.text = text;
    this.error = error;
  }

  static ToolResult ok(String text) {
    return new ToolResult(text, false);
  }

  static ToolResult error(String text) {
    return new ToolResult(text, true);
  }

  JSONObject toJson() {
    JSONArray content = new JSONArray();
    content.put(new JSONObject().put("type", "text").put("text", text));
    JSONObject result = new JSONObject().put("content", content);
    if (error) {
      result.put("isError", true);
    }
    return result;
  }
}
