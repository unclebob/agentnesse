package agnesse.fixtures;

import agnesse.markdown.FrontMatter;
import agnesse.mcp.McpServer;
import agnesseMain.AgNesseMain;
import agnesseMain.Arguments;
import org.json.JSONArray;
import org.json.JSONObject;
import util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Speaks JSON-RPC to an in-process MCP server. The guide page drives the
 * tools through this fixture.
 */
public class McpSession {
  private final File home;
  private final Writer toServer;
  private final BufferedReader fromServer;
  private final Thread server;
  private int nextId = 1;
  private String protocol = "";

  public McpSession() throws Exception {
    this(Files.createTempDirectory("agnesse-mcp").toFile());
  }

  McpSession(File home) throws Exception {
    this.home = home;
    new File(home, "Wiki").mkdirs();
    PipedOutputStream clientOut = new PipedOutputStream();
    PipedInputStream serverInput = new PipedInputStream(clientOut, 1 << 20);
    PipedOutputStream serverOut = new PipedOutputStream();
    PipedInputStream clientIn = new PipedInputStream(serverOut, 1 << 20);
    toServer = new OutputStreamWriter(clientOut, StandardCharsets.UTF_8);
    fromServer = new BufferedReader(new InputStreamReader(clientIn, StandardCharsets.UTF_8));
    server = new Thread(() -> serve(serverInput, serverOut), "mcp-session");
    server.setDaemon(true);
    server.start();
    protocol = initialize();
  }

  public String protocol() {
    return protocol;
  }

  public void page(String name, String type, String body) throws Exception {
    writePage(name, type, List.of(), body);
  }

  public void taggedPage(String name, String type, String tags, String body) throws Exception {
    writePage(name, type, FrontMatter.tagList(tags), body);
  }

  public String tools() throws Exception {
    JSONObject response = request("tools/list", new JSONObject());
    JSONArray listed = response.getJSONObject("result").getJSONArray("tools");
    List<String> names = new ArrayList<>();
    for (int i = 0; i < listed.length(); i++) {
      names.add(listed.getJSONObject(i).getString("name"));
    }
    return String.join(", ", names);
  }

  public String typeOf(String path) throws Exception {
    return page(path).getString("type");
  }

  public String tagsOf(String path) throws Exception {
    return join(page(path).getJSONArray("tags"));
  }

  public int pagesOfType(String type) throws Exception {
    return pages(null, type, null).length();
  }

  public int pagesWithTag(String tags) throws Exception {
    return pages(null, null, tags).length();
  }

  public int pagesUnder(String path) throws Exception {
    return pages(path, null, null).length();
  }

  public String run(String page) throws Exception {
    return runId(new JSONObject().put("page", page));
  }

  public String runAsSuite(String page) throws Exception {
    return runId(new JSONObject().put("page", page).put("suite", true));
  }

  public String runAsTest(String page) throws Exception {
    return runId(new JSONObject().put("page", page).put("suite", false));
  }

  public String runWithTags(String page, String tags) throws Exception {
    return runId(new JSONObject().put("page", page).put("tags", tags));
  }

  public String runExcluding(String page, String tags) throws Exception {
    return runId(new JSONObject().put("page", page).put("excludeTags", tags));
  }

  public String rerun(String page) throws Exception {
    File failures = new File(home, ".agnesse/last-failures.json");
    return runId(new JSONObject().put("page", page).put("rerunFailures", failures.getPath()));
  }

  public boolean resultsForContain(String runId, String fragment) throws Exception {
    return document(runId).contains(fragment);
  }

  public int failuresIn(String runId) throws Exception {
    return new JSONObject(document(runId)).getJSONObject("run").getJSONArray("failedPages").length();
  }

  void end() throws Exception {
    toServer.close();
    server.join(10_000);
    stopped(server);
    FileUtil.deleteFileSystemDirectory(home);
  }

  static void stopped(Thread server) {
    if (server.isAlive()) {
      throw new IllegalStateException("mcp did not exit");
    }
  }

  static void launch(File home, InputStream in, OutputStream out) throws Exception {
    Integer code = new AgNesseMain().launchAgNesse(
      new Arguments("-d", home.getPath(), "-r", "Wiki", "-mcp"), in, out);
    if (code != 0) {
      throw new IllegalStateException("mcp exited " + code);
    }
  }

  static void close(OutputStream out) {
    try {
      out.close();
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
    }
  }

  private JSONObject page(String path) throws Exception {
    JSONArray listed = pages(path, null, null);
    for (int i = 0; i < listed.length(); i++) {
      JSONObject item = listed.getJSONObject(i);
      if (path.equals(item.getString("path"))) {
        return item;
      }
    }
    throw new IllegalStateException("no such page: " + path);
  }

  private JSONArray pages(String path, String type, String tags) throws Exception {
    JSONObject args = new JSONObject();
    put(args, "path", path);
    put(args, "type", type);
    put(args, "tags", tags);
    return new JSONArray(tool("list_pages", args));
  }

  private String runId(JSONObject args) throws Exception {
    return new JSONObject(tool("run", args)).getString("runId");
  }

  private String document(String runId) throws Exception {
    return tool("get_results", new JSONObject().put("runId", runId));
  }

  private String tool(String name, JSONObject arguments) throws Exception {
    JSONObject response = request("tools/call", new JSONObject().put("name", name).put("arguments", arguments));
    JSONObject result = response.getJSONObject("result");
    String text = result.getJSONArray("content").getJSONObject(0).getString("text");
    if (result.optBoolean("isError")) {
      throw new IllegalStateException(text);
    }
    return text;
  }

  private String initialize() throws Exception {
    JSONObject response = request("initialize", new JSONObject()
      .put("protocolVersion", McpServer.LATEST)
      .put("capabilities", new JSONObject())
      .put("clientInfo", new JSONObject().put("name", "agnesse-guide").put("version", "1")));
    send(new JSONObject().put("jsonrpc", "2.0").put("method", "notifications/initialized"));
    return response.getJSONObject("result").getString("protocolVersion");
  }

  private JSONObject request(String method, JSONObject params) throws Exception {
    int id = nextId++;
    send(new JSONObject().put("jsonrpc", "2.0").put("id", id).put("method", method).put("params", params));
    return read();
  }

  private void send(JSONObject message) throws Exception {
    toServer.write(message.toString());
    toServer.write("\n");
    toServer.flush();
  }

  private JSONObject read() throws Exception {
    String line = fromServer.readLine();
    if (line == null) {
      throw new IllegalStateException("mcp closed");
    }
    return new JSONObject(line);
  }

  private void serve(InputStream in, OutputStream out) {
    try {
      launch(home, in, out);
    } catch (Exception e) {
      close(out);
    }
  }

  private void writePage(String name, String type, List<String> tags, String body) throws Exception {
    File file = new File(new File(home, "Wiki"), name.replace('.', '/') + ".md");
    file.getParentFile().mkdirs();
    String text = body == null ? "" : body.replace("<br>", "\n").replace("<br/>", "\n");
    Files.writeString(file.toPath(), FrontMatter.write(type, tags, null, false, text));
  }

  private static void put(JSONObject args, String key, String value) {
    if (value != null) {
      args.put(key, value);
    }
  }

  private static String join(JSONArray tags) {
    List<String> names = new ArrayList<>();
    for (int i = 0; i < tags.length(); i++) {
      names.add(tags.getString(i));
    }
    return String.join(", ", names);
  }
}
