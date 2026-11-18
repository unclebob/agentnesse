package agnesse.mcp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

/** The three tools an agent is allowed to call. Pages stay on the file system. */
final class ToolCatalog {
  private ToolCatalog() {
  }

  static JSONArray tools() {
    JSONArray tools = new JSONArray();
    tools.put(tool("list_pages",
      "List wiki pages. Each item has path, type, and tags.",
      new JSONObject()
        .put("path", string("List this page and its children. Omit for the whole wiki."))
        .put("type", string("Keep pages of this type: test, suite, static, or test-adjunct."))
        .put("tags", string("Comma-separated tags. A page is kept when it has any of them."))));
    tools.put(tool("run",
      "Run a page or a suite. Tag filters and a rerun-failures file match the command line. Returns the run id.",
      new JSONObject()
        .put("page", string("Wiki path of the page or suite."))
        .put("suite", bool("Run the children. Omit to follow the page type."))
        .put("tags", string("suiteFilter: comma-separated tags. A test matches when it has any of them."))
        .put("excludeTags", string("excludeSuiteFilter: skip a test that has any of these tags."))
        .put("rerunFailures", string("File of failed pages, or a full results document. Same as --rerun-failures.")),
      "page"));
    tools.put(tool("get_results",
      "Return the schema version 1 JSON for a run id.",
      new JSONObject().put("runId", string("The id returned by run.")),
      "runId"));
    return tools;
  }

  private static JSONObject tool(String name, String description, JSONObject properties, String... required) {
    JSONObject schema = new JSONObject()
      .put("type", "object")
      .put("properties", properties)
      .put("additionalProperties", false);
    if (required.length > 0) {
      schema.put("required", new JSONArray(Arrays.asList(required)));
    }
    return new JSONObject()
      .put("name", name)
      .put("description", description)
      .put("inputSchema", schema);
  }

  private static JSONObject string(String description) {
    return new JSONObject().put("type", "string").put("description", description);
  }

  private static JSONObject bool(String description) {
    return new JSONObject().put("type", "boolean").put("description", description);
  }
}
