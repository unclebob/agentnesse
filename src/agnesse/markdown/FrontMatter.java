package agnesse.markdown;

import agnesse.wiki.PageData;
import agnesse.wiki.PageType;
import agnesse.wiki.WikiPageProperty;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * JSON object at the start of a Markdown page. The body is everything after
 * the object's closing brace, with one leading newline removed.
 */
public final class FrontMatter {
  private static final Set<String> KEYS = Set.of("type", "tags", "help", "prune", "user");
  private static final Set<String> TYPES = Set.of("test", "suite", "static");

  private final boolean present;
  private final String type;
  private final List<String> tags;
  private final String help;
  private final boolean prune;
  private final String user;
  private final String body;
  private final String prefix;

  private FrontMatter(boolean present, String type, List<String> tags, String help, boolean prune, String user, String body, String prefix) {
    this.present = present;
    this.type = type;
    this.tags = tags;
    this.help = help;
    this.prune = prune;
    this.user = user;
    this.body = body;
    this.prefix = prefix;
  }

  public static FrontMatter from(PageData data) {
    String content = data.getContent() == null ? "" : data.getContent();
    FrontMatter inBody = read(content);
    if (inBody.present()) {
      return inBody;
    }
    return new FrontMatter(true, typeFrom(data), tagsFrom(data),
      data.getAttribute(WikiPageProperty.HELP),
      data.hasAttribute(WikiPageProperty.PRUNE),
      data.getAttribute(WikiPageProperty.LAST_MODIFYING_USER),
      content, "");
  }

  public static FrontMatter read(String text) {
    int start = indexOfObject(text);
    if (start < 0) {
      return absent(text);
    }
    int end = endOfObject(text, start);
    String rest = text.substring(end + 1);
    String prefix = text.substring(0, end + 1);
    if (rest.startsWith("\n")) {
      prefix = prefix + "\n";
      rest = rest.substring(1);
    }
    JSONObject json = object(text.substring(start, end + 1));
    return new FrontMatter(true, typeOf(json), tagsOf(json), helpOf(json), pruneOf(json), userOf(json), rest, prefix);
  }

  /** The JSON an editor submitted. Blank clears the front matter. Anything else must be an object. */
  public static FrontMatter readSubmitted(String submitted) {
    String json = submitted == null ? "" : submitted;
    if (json.isBlank()) {
      return absent("");
    }
    FrontMatter matter = read(json);
    if (!matter.present()) {
      throw new IllegalArgumentException("malformed JSON front matter");
    }
    return matter;
  }

  /**
   * Keep the file's front matter bytes when the submitted text is the text the editor showed.
   * Otherwise the JSON replaces type, tags, help, prune, and user.
   */
  public static void keepOrApply(PageData data, FrontMatter matter, String submitted, String pageName) {
    String json = submitted == null ? "" : submitted;
    if (!matter.present() || json.equals(objectText(data.getFrontMatterPrefix()))) {
      data.setPreserveFrontMatter(true);
      if (!matter.present()) {
        data.setFrontMatterPrefix("");
      }
      return;
    }
    apply(data, matter, pageName);
  }

  public static void apply(PageData data, FrontMatter matter, String pageName) {
    String type = matter.type() == null
      ? PageType.getPageTypeForPageName(pageName).toString().toLowerCase()
      : matter.type();
    data.getProperties().remove(PageType.TEST.toString());
    data.getProperties().remove(PageType.SUITE.toString());
    if ("test".equals(type)) {
      data.setAttribute(PageType.TEST.toString());
    } else if ("suite".equals(type)) {
      data.setAttribute(PageType.SUITE.toString());
    }
    if (matter.tags().isEmpty()) {
      data.removeAttribute(WikiPageProperty.SUITES);
    } else {
      data.setAttribute(WikiPageProperty.SUITES, String.join(", ", matter.tags()));
    }
    data.setOrRemoveAttribute(WikiPageProperty.HELP, matter.help());
    if (matter.prune()) {
      data.setAttribute(WikiPageProperty.PRUNE);
    } else {
      data.removeAttribute(WikiPageProperty.PRUNE);
    }
    data.setOrRemoveAttribute(WikiPageProperty.LAST_MODIFYING_USER, matter.user());
  }

  public static String defaultJson(String pageType) {
    return objectText(write(jsonType(pageType), List.of(), null, false, null, ""));
  }

  public static String objectText(String prefix) {
    if (prefix == null) {
      return "";
    }
    if (prefix.endsWith("\n")) {
      return prefix.substring(0, prefix.length() - 1);
    }
    return prefix;
  }

  private static String jsonType(String pageType) {
    if ("Test".equals(pageType)) {
      return "test";
    }
    if ("Suite".equals(pageType)) {
      return "suite";
    }
    if ("Static".equals(pageType)) {
      return "static";
    }
    return null;
  }

  public static String write(String type, List<String> tags, String help, boolean prune, String body) {
    return write(type, tags, help, prune, null, body);
  }

  public static String write(String type, List<String> tags, String help, boolean prune, String user, String body) {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"type\": ").append(type == null ? "null" : JSONObject.quote(type)).append(",\n");
    json.append("  \"tags\": ").append(tagsJson(tags)).append(",\n");
    json.append("  \"help\": ").append(help == null ? "null" : JSONObject.quote(help)).append(",\n");
    json.append("  \"prune\": ").append(prune);
    if (user != null) {
      json.append(",\n  \"user\": ").append(JSONObject.quote(user));
    }
    json.append("\n");
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

  public String user() {
    return user;
  }

  public String body() {
    return body;
  }

  public String prefix() {
    return prefix;
  }

  static String typeFrom(PageData data) {
    if (data.hasAttribute(PageType.SUITE.toString())) {
      return "suite";
    }
    if (data.hasAttribute(PageType.TEST.toString())) {
      return "test";
    }
    return "static";
  }

  static List<String> tagsFrom(PageData data) {
    String suites = data.getAttribute(WikiPageProperty.SUITES);
    if (suites == null || suites.isBlank()) {
      return List.of();
    }
    return Arrays.stream(suites.split("\\s*,\\s*")).filter(tag -> !tag.isEmpty()).toList();
  }

  private static FrontMatter absent(String text) {
    return new FrontMatter(false, null, List.of(), null, false, null, text, "");
  }

  private static int indexOfObject(String text) {
    String trimmed = text.stripLeading();
    if (trimmed.isEmpty() || trimmed.charAt(0) != '{') {
      return -1;
    }
    return text.length() - trimmed.length();
  }

  private static int endOfObject(String text, int start) {
    Scan scan = new Scan();
    for (int i = start; i < text.length(); i++) {
      if (scan.closes(text.charAt(i))) {
        return i;
      }
    }
    throw new IllegalArgumentException("malformed JSON front matter");
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
    if (json.isNull("type")) {
      return null;
    }
    Object value = json.get("type");
    if (!(value instanceof String) || !TYPES.contains(value)) {
      throw new IllegalArgumentException("front matter type \"" + value + "\" is not test, suite, or static");
    }
    return (String) value;
  }

  private static List<String> tagsOf(JSONObject json) {
    if (json.isNull("tags")) {
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

  private static String userOf(JSONObject json) {
    return value(json, "user", String.class, null, "front matter user must be a string");
  }

  private static boolean pruneOf(JSONObject json) {
    return value(json, "prune", Boolean.class, false, "front matter prune must be a boolean");
  }

  private static <T> T value(JSONObject json, String key, Class<T> type, T fallback, String message) {
    if (json.isNull(key)) {
      return fallback;
    }
    Object found = json.get(key);
    if (!type.isInstance(found)) {
      throw new IllegalArgumentException(message);
    }
    return type.cast(found);
  }

  private static String tagsJson(List<String> tags) {
    JSONArray array = new JSONArray();
    for (String tag : tags) {
      array.put(tag);
    }
    return array.toString();
  }
}
