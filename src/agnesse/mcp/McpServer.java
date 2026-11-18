package agnesse.mcp;

import agnesse.AgNesseVersion;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MCP on stdio. One JSON-RPC message per line. Nothing else is written to
 * the output stream.
 */
public final class McpServer {
  public static final String LATEST = "2025-06-18";
  private static final List<String> PROTOCOLS = List.of("2024-11-05", "2025-03-26", LATEST);
  private static final String INSTRUCTIONS =
    "Tools: list_pages, run, and get_results. Pages are files. get_results returns schema version 1 JSON.";

  private final WikiTools tools;
  private final String version;
  private boolean initialized;

  public McpServer(WikiTools tools) {
    this(tools, new AgNesseVersion().toString().trim());
  }

  McpServer(WikiTools tools, String version) {
    this.tools = tools;
    this.version = version;
  }

  public void serve(InputStream in, OutputStream out) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    String line;
    while ((line = reader.readLine()) != null) {
      String response = dispatch(line);
      if (response != null) {
        writer.write(response);
        writer.write('\n');
        writer.flush();
      }
    }
  }

  private String dispatch(String line) {
    if (line.isBlank()) {
      return null;
    }
    JsonRpc.Message message = JsonRpc.read(line);
    if (message.notification) {
      return null;
    }
    if (message.failed()) {
      return JsonRpc.error(message.id, message.code, message.error);
    }
    if (!ready(message.method)) {
      return JsonRpc.error(message.id, -32002, "Server not initialized");
    }
    return respond(message);
  }

  private boolean ready(String method) {
    return initialized || "initialize".equals(method) || "ping".equals(method);
  }

  private String respond(JsonRpc.Message message) {
    return switch (message.method) {
      case "initialize" -> initialize(message);
      case "ping" -> JsonRpc.result(message.id, new JSONObject());
      case "tools/list" -> JsonRpc.result(message.id, new JSONObject().put("tools", ToolCatalog.tools()));
      case "tools/call" -> call(message);
      default -> JsonRpc.error(message.id, -32601, "Method not found");
    };
  }

  private String initialize(JsonRpc.Message message) {
    initialized = true;
    JSONObject result = new JSONObject()
      .put("protocolVersion", negotiate(requested(message.params)))
      .put("capabilities", new JSONObject().put("tools", new JSONObject()))
      .put("serverInfo", new JSONObject().put("name", "agnesse").put("version", version))
      .put("instructions", INSTRUCTIONS);
    return JsonRpc.result(message.id, result);
  }

  private static String requested(JSONObject params) {
    return JsonRpc.stringField(params, "protocolVersion");
  }

  static String negotiate(String requested) {
    if (requested != null && PROTOCOLS.contains(requested)) {
      return requested;
    }
    return LATEST;
  }

  private String call(JsonRpc.Message message) {
    if (!message.params.has("name")) {
      return JsonRpc.error(message.id, -32602, "name is required");
    }
    Object name = message.params.get("name");
    if (!(name instanceof String tool)) {
      return JsonRpc.error(message.id, -32602, "name is required");
    }
    JSONObject arguments = arguments(message.params);
    if (arguments == null) {
      return JsonRpc.error(message.id, -32602, "arguments must be an object");
    }
    return JsonRpc.result(message.id, tools.call(tool, arguments).toJson());
  }

  private static JSONObject arguments(JSONObject params) {
    return JsonRpc.objectField(params, "arguments");
  }
}
