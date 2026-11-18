package agnesse.mcp;

import agnesse.AgNesseContext;
import agnesse.markdown.FrontMatter;
import agnesse.results.RunQuery;
import agnesse.wiki.PageType;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;
import agnesse.wiki.WikiPageProperty;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code list_pages}, {@code run}, and {@code get_results}. A run is the
 * command the CLI would have passed to {@code executeSingleCommand}. The
 * stored document is the schema version 1 JSON that command wrote.
 */
public final class WikiTools {
  private final AgNesseContext context;
  private final Map<String, String> results = new LinkedHashMap<>();

  public WikiTools(AgNesseContext context) {
    this.context = context;
  }

  ToolResult call(String name, JSONObject arguments) {
    JSONObject args = arguments == null ? new JSONObject() : arguments;
    try {
      return switch (name) {
        case "list_pages" -> list(args);
        case "run" -> run(args);
        case "get_results" -> results(args);
        default -> ToolResult.error("unknown tool: " + name);
      };
    } catch (IllegalArgumentException e) {
      return ToolResult.error(e.getMessage());
    }
  }

  private ToolResult list(JSONObject args) {
    String path = text(args, "path");
    String type = text(args, "type");
    List<String> tags = FrontMatter.tagList(text(args, "tags"));
    if (path != null && page(path) == null) {
      return ToolResult.error("no such page: " + path);
    }
    List<JSONObject> found = new ArrayList<>();
    collect(context.getRootPage(), found);
    found.removeIf(item -> !kept(item, path, type, tags));
    found.sort(Comparator.comparing(item -> item.getString("path")));
    return ToolResult.ok(new JSONArray(found).toString());
  }

  private void collect(WikiPage page, List<JSONObject> found) {
    String path = PathParser.render(page.getFullPath());
    if (!path.isEmpty()) {
      found.add(describe(page, path));
    }
    for (WikiPage child : page.getChildren()) {
      collect(child, found);
    }
  }

  private static JSONObject describe(WikiPage page, String path) {
    return new JSONObject()
      .put("path", path)
      .put("type", PageType.storedType(page.getData()))
      .put("tags", new JSONArray(tagsOf(page)));
  }

  private static boolean kept(JSONObject page, String path, String type, List<String> tags) {
    return under(page.getString("path"), path)
      && typed(page.getString("type"), type)
      && tagged(page.getJSONArray("tags"), tags);
  }

  private static boolean under(String page, String root) {
    if (root == null) {
      return true;
    }
    return page.equals(root) || page.startsWith(root + ".");
  }

  private static boolean typed(String actual, String wanted) {
    return wanted == null || wanted.equals(actual);
  }

  private static boolean tagged(JSONArray actual, List<String> wanted) {
    if (wanted.isEmpty()) {
      return true;
    }
    for (String tag : wanted) {
      if (contains(actual, tag)) {
        return true;
      }
    }
    return false;
  }

  private static boolean contains(JSONArray tags, String wanted) {
    for (int i = 0; i < tags.length(); i++) {
      if (wanted.equals(tags.getString(i))) {
        return true;
      }
    }
    return false;
  }

  private ToolResult run(JSONObject args) {
    String name = text(args, "page");
    if (name == null) {
      return ToolResult.error("page is required");
    }
    if (name.contains("?") || name.contains("&")) {
      return ToolResult.error("page is a wiki path");
    }
    WikiPage page = page(name);
    if (page == null) {
      return ToolResult.error("no such page: " + name);
    }
    String command = RunQuery.json(
      command(name, asSuite(args, page), text(args, "tags"), text(args, "excludeTags")),
      text(args, "rerunFailures"));
    try {
      ByteArrayOutputStream sink = new ByteArrayOutputStream();
      context.agNesse.executeSingleCommand(command, sink);
      String document = sink.toString(StandardCharsets.UTF_8);
      String id = new JSONObject(document).getJSONObject("run").getString("id");
      results.put(id, document);
      return ToolResult.ok(new JSONObject().put("runId", id).toString());
    } catch (Exception e) {
      return ToolResult.error(failure(e));
    }
  }

  static String failure(Exception e) {
    String message = e.getMessage();
    return message == null || message.isBlank() ? "run failed" : message;
  }

  private static String command(String page, boolean suite, String tags, String exclude) {
    StringBuilder command = new StringBuilder(page);
    command.append(suite ? "?suite" : "?test");
    add(command, "suiteFilter", tags);
    add(command, "excludeSuiteFilter", exclude);
    return command.toString();
  }

  private static void add(StringBuilder command, String name, String value) {
    if (value == null) {
      return;
    }
    command.append('&').append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
  }

  private static boolean asSuite(JSONObject args, WikiPage page) {
    if (!args.has("suite") || args.isNull("suite")) {
      return PageType.fromWikiPage(page) == PageType.SUITE;
    }
    Object value = args.get("suite");
    if (!(value instanceof Boolean)) {
      throw new IllegalArgumentException("suite must be a boolean");
    }
    return (Boolean) value;
  }

  private ToolResult results(JSONObject args) {
    String id = text(args, "runId");
    if (id == null) {
      return ToolResult.error("runId is required");
    }
    String document = results.get(id);
    if (document == null) {
      return ToolResult.error("no such run: " + id);
    }
    return ToolResult.ok(document);
  }

  private WikiPage page(String path) {
    WikiPagePath parsed = PathParser.parse(path);
    if (parsed == null) {
      return null;
    }
    return context.getRootPage().getPageCrawler().getPage(parsed);
  }

  private static List<String> tagsOf(WikiPage page) {
    return FrontMatter.tagList(page.getData().getAttribute(WikiPageProperty.SUITES));
  }

  private static String text(JSONObject args, String key) {
    if (!args.has(key) || args.isNull(key)) {
      return null;
    }
    Object value = args.get(key);
    if (!(value instanceof String text)) {
      throw new IllegalArgumentException(key + " must be a string");
    }
    return text.isBlank() ? null : text;
  }
}
