package fitnesse.markdown;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JSON object at the start of a Markdown page. The body is everything after
 * the object's closing brace, with one leading newline removed.
 */
public final class FrontMatter {
  private static final Set<String> KEYS = Set.of("type", "tags", "help", "prune");
  private static final Set<String> TYPES = Set.of("test", "suite", "static");

  private final boolean present;
  private final String type;
  private final List<String> tags;
  private final String help;
  private final boolean prune;
  private final String body;

  private FrontMatter(boolean present, String type, List<String> tags, String help, boolean prune, String body) {
    this.present = present;
    this.type = type;
    this.tags = tags;
    this.help = help;
    this.prune = prune;
    this.body = body;
  }

  public static FrontMatter read(String text) {
    int start = indexOfObject(text);
    if (start < 0) {
      return absent(text);
    }
    int end = endOfObject(text, start);
    if (end < 0) {
      throw new IllegalArgumentException("malformed JSON front matter");
    }
    JSONObject json = object(text.substring(start, end + 1));
    return new FrontMatter(true, typeOf(json), tagsOf(json), helpOf(json), pruneOf(json), bodyAfter(text, end));
  }

  public static String write(String type, List<String> tags, String help, boolean prune, String body) {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"type\": ").append(JSONObject.quote(type)).append(",\n");
    json.append("  \"tags\": ").append(tagsJson(tags)).append(",\n");
    json.append("  \"help\": ").append(help == null ? "null" : JSONObject.quote(help)).append(",\n");
    json.append("  \"prune\": ").append(prune).append("\n");
    json.append("}\n");
    if (body != null) {
      json.append(body);
    }
    return json.toString();
  }

  public boolean present() {
    return present;
  }

  public String type() {
    return type;
  }

  public List<String> tags() {
    return tags;
  }

  public String help() {
    return help;
  }

  public boolean prune() {
    return prune;
  }

  public String body() {
    return body;
  }

  private static FrontMatter absent(String text) {
    return new FrontMatter(false, null, List.of(), null, false, text);
  }

  private static int indexOfObject(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '{') {
        return i;
      }
      if (!Character.isWhitespace(c)) {
        return -1;
      }
    }
    return -1;
  }

  private static int endOfObject(String text, int start) {
    Scan scan = new Scan();
    for (int i = start; i < text.length(); i++) {
      if (scan.closes(text.charAt(i))) {
        return i;
      }
    }
    return -1;
  }

  private static final class Scan {
    private int depth;
    private boolean inString;
    private boolean escape;

    private boolean closes(char c) {
      if (inString) {
        scanString(c);
        return false;
      }
      return scanStructure(c);
    }

    private void scanString(char c) {
      if (escape) {
        escape = false;
      } else if (c == '\\') {
        escape = true;
      } else if (c == '"') {
        inString = false;
      }
    }

    private boolean scanStructure(char c) {
      if (c == '"') {
        inString = true;
        return false;
      }
      if (c == '{') {
        depth++;
        return false;
      }
      if (c == '}') {
        depth--;
        return depth == 0;
      }
      return false;
    }
  }

  private static JSONObject object(String json) {
    try {
      return new JSONObject(json);
    } catch (JSONException e) {
      throw new IllegalArgumentException("malformed JSON front matter");
    }
  }

  private static void knownKeys(JSONObject json) {
    for (String key : json.keySet()) {
      if (!KEYS.contains(key)) {
        throw new IllegalArgumentException("unknown front matter key \"" + key + "\"");
      }
    }
  }

  private static String typeOf(JSONObject json) {
    knownKeys(json);
    if (!json.has("type") || json.isNull("type")) {
      return null;
    }
    Object value = json.get("type");
    if (!(value instanceof String) || !TYPES.contains(value)) {
      throw new IllegalArgumentException("front matter type \"" + value + "\" is not test, suite, or static");
    }
    return (String) value;
  }

  private static List<String> tagsOf(JSONObject json) {
    if (!json.has("tags") || json.isNull("tags")) {
      return List.of();
    }
    Object value = json.get("tags");
    if (!(value instanceof JSONArray)) {
      throw new IllegalArgumentException("front matter tags must be an array of strings");
    }
    JSONArray array = (JSONArray) value;
    List<String> tags = new ArrayList<>();
    for (int i = 0; i < array.length(); i++) {
      Object tag = array.get(i);
      if (!(tag instanceof String)) {
        throw new IllegalArgumentException("front matter tags must be an array of strings");
      }
      tags.add((String) tag);
    }
    return List.copyOf(tags);
  }

  private static String helpOf(JSONObject json) {
    return value(json, "help", String.class, null, "front matter help must be a string");
  }

  private static boolean pruneOf(JSONObject json) {
    return value(json, "prune", Boolean.class, false, "front matter prune must be a boolean");
  }

  private static <T> T value(JSONObject json, String key, Class<T> type, T fallback, String message) {
    if (!json.has(key) || json.isNull(key)) {
      return fallback;
    }
    Object found = json.get(key);
    if (!type.isInstance(found)) {
      throw new IllegalArgumentException(message);
    }
    return type.cast(found);
  }

  private static String bodyAfter(String text, int end) {
    int body = end + 1;
    if (body < text.length() && text.charAt(body) == '\n') {
      body++;
    }
    return text.substring(body);
  }

  private static String tagsJson(List<String> tags) {
    JSONArray array = new JSONArray();
    for (String tag : tags) {
      array.put(tag);
    }
    return array.toString();
  }
}
