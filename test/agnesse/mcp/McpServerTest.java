package agnesse.mcp;

import agnesse.AgNesseContext;
import agnesse.ContextConfigurator;
import agnesse.markdown.FrontMatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** JSON-RPC over a pipe, the way an agent talks to {@code -mcp}. */
public class McpServerTest {
  private Path home;
  private AgNesseContext context;
  private Writer toServer;
  private BufferedReader fromServer;
  private Thread server;
  private final ExecutorService readers = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "mcp-test-reader");
    thread.setDaemon(true);
    return thread;
  });

  @Before
  public void start() throws Exception {
    home = Files.createTempDirectory("agnesse-mcp-protocol");
    Files.createDirectories(home.resolve("Wiki"));
    write("Mix", "suite", List.of());
    write("Mix.Child", "test", List.of("red", "blue"));
    write("Mixed", "static", List.of("red"));
    write("SetUp", "test-adjunct", List.of());
    context = ContextConfigurator.systemDefaults()
      .withRootPath(home.toString())
      .withRootDirectoryName("Wiki")
      .makeAgNesseContext();
    PipedOutputStream clientOut = new PipedOutputStream();
    PipedInputStream serverIn = new PipedInputStream(clientOut, 1 << 20);
    PipedOutputStream serverOut = new PipedOutputStream();
    PipedInputStream clientIn = new PipedInputStream(serverOut, 1 << 20);
    toServer = new OutputStreamWriter(clientOut, StandardCharsets.UTF_8);
    fromServer = new BufferedReader(new InputStreamReader(clientIn, StandardCharsets.UTF_8));
    McpServer mcp = new McpServer(new WikiTools(context), "test-version");
    server = new Thread(() -> {
      try {
        mcp.serve(serverIn, serverOut);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }, "mcp-protocol");
    server.setDaemon(true);
    server.start();
  }

  @After
  public void stop() throws Exception {
    toServer.close();
    server.join(10_000);
    readers.shutdownNow();
    FileUtil.deleteFileSystemDirectory(home.toFile());
  }

  @Test
  public void nullToolArgumentsListTheWiki() {
    ToolResult result = new WikiTools(context).call("list_pages", null);
    assertFalse(result.toJson().optBoolean("isError"));
  }

  @Test
  public void handshakeNegotiatesTheRequestedProtocolAndListsTheTools() throws Exception {
    JSONObject initialized = request("initialize", new JSONObject()
      .put("protocolVersion", "2024-11-05")
      .put("capabilities", new JSONObject())
      .put("clientInfo", new JSONObject().put("name", "test").put("version", "1")));
    assertEquals("2024-11-05", initialized.getJSONObject("result").getString("protocolVersion"));
    JSONObject info = initialized.getJSONObject("result").getJSONObject("serverInfo");
    assertEquals("agnesse", info.getString("name"));
    assertEquals("test-version", info.getString("version"));
    assertTrue(initialized.getJSONObject("result").getString("instructions").contains("schema version 1"));
    notify("notifications/initialized");

    JSONObject listed = request("tools/list", new JSONObject());
    JSONArray tools = listed.getJSONObject("result").getJSONArray("tools");
    assertEquals(List.of("list_pages", "run", "get_results"), names(tools));
    JSONObject run = tool(tools, "run");
    assertEquals(List.of("page"), strings(run.getJSONObject("inputSchema").getJSONArray("required")));
    assertFalse(run.getJSONObject("inputSchema").getBoolean("additionalProperties"));
    assertFalse(tool(tools, "list_pages").getJSONObject("inputSchema").has("required"));
    assertEquals(List.of("runId"), strings(tool(tools, "get_results").getJSONObject("inputSchema").getJSONArray("required")));
    assertEquals(-32601, request("nope", new JSONObject()).getJSONObject("error").getInt("code"));
    assertTrue(request("initialize", new JSONObject().put("protocolVersion", McpServer.LATEST)).has("result"));
  }

  @Test
  public void anUnknownProtocolFallsBackToTheLatest() throws Exception {
    JSONObject initialized = request("initialize", new JSONObject().put("protocolVersion", "1999-01-01"));
    assertEquals(McpServer.LATEST, initialized.getJSONObject("result").getString("protocolVersion"));
    JSONObject missing = request("initialize", new JSONObject());
    assertEquals(McpServer.LATEST, missing.getJSONObject("result").getString("protocolVersion"));
    JSONObject absent = request("initialize", new JSONObject().put("protocolVersion", JSONObject.NULL));
    assertEquals(McpServer.LATEST, absent.getJSONObject("result").getString("protocolVersion"));
    JSONObject blank = request("initialize", new JSONObject().put("protocolVersion", "  "));
    assertEquals(McpServer.LATEST, blank.getJSONObject("result").getString("protocolVersion"));
  }

  @Test
  public void aNonStringProtocolVersionFallsBackToTheLatest() throws Exception {
    JSONObject initialized = request("initialize", new JSONObject().put("protocolVersion", 1));
    assertEquals(McpServer.LATEST, initialized.getJSONObject("result").getString("protocolVersion"));
  }

  @Test
  public void eachSupportedProtocolIsEchoed() throws Exception {
    assertEquals("2025-03-26", request("initialize", new JSONObject().put("protocolVersion", "2025-03-26"))
      .getJSONObject("result").getString("protocolVersion"));
  }

  @Test
  public void pingWorksBeforeInitializeAndAToolCallDoesNot() throws Exception {
    JSONObject pong = request("ping", new JSONObject());
    assertEquals(0, pong.getJSONObject("result").length());
    JSONObject rejected = request("tools/list", null);
    assertEquals(-32002, rejected.getJSONObject("error").getInt("code"));
    assertEquals("Server not initialized", rejected.getJSONObject("error").getString("message"));
  }

  @Test
  public void aNotificationGetsNoResponseAndANotifiedMethodWithAnIdIsARequest() throws Exception {
    initialize();
    notify("notifications/cancelled");
    JSONObject pong = request("ping", null);
    assertEquals(1, pong.getInt("id"));
    JSONObject answered = request("notifications/cancelled", new JSONObject());
    assertEquals(-32601, answered.getJSONObject("error").getInt("code"));
  }

  @Test
  public void aBlankLineIsIgnored() throws Exception {
    raw("   ");
    JSONObject pong = request("ping", null);
    assertTrue(pong.has("result"));
  }

  @Test
  public void malformedMessagesAreJsonRpcErrors() throws Exception {
    assertEquals(-32700, error("{"));
    assertEquals(-32600, error("[{\"jsonrpc\":\"2.0\"}]"));
    JSONObject wrongVersion = roundTrip("{\"jsonrpc\":\"1.0\",\"id\":7,\"method\":\"ping\"}");
    assertEquals(7, wrongVersion.getInt("id"));
    assertEquals(-32600, wrongVersion.getJSONObject("error").getInt("code"));
    assertEquals(-32600, error("{\"jsonrpc\":\"2.0\",\"id\":null,\"method\":\"ping\"}"));
    assertEquals(-32600, error("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"\"}"));
    assertEquals(-32600, error("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":1}"));
    assertEquals(-32602, error("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\",\"params\":[]}"));
    assertEquals(-32002, error("{\"jsonrpc\":\"2.0\",\"id\":\"abc\",\"method\":\"nope\"}"));
    assertEquals(-32700, error("\"not-an-object\""));
    assertEquals(-32600, error("{\"jsonrpc\":\"2.0\",\"id\":1}"));
    assertEquals(-32600, error("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":null}"));
    JSONObject nullParams = roundTrip("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\",\"params\":null}");
    assertTrue(nullParams.has("result"));
  }

  @Test
  public void aStringIdIsEchoedAndANullIdIsRejectedAsNull() throws Exception {
    JSONObject missing = roundTrip("{\"jsonrpc\":\"2.0\",\"id\":null,\"method\":\"ping\"}");
    assertTrue(missing.isNull("id"));
    JSONObject echoed = request("ping", null, "abc");
    assertEquals("abc", echoed.getString("id"));
  }

  @Test
  public void listPagesFiltersByPathTypeAndTagAndSeesAPageWrittenLater() throws Exception {
    initialize();
    JSONArray all = pages(new JSONObject());
    assertEquals(List.of("Mix", "Mix.Child", "Mixed", "SetUp"), paths(all));

    JSONArray under = pages(new JSONObject().put("path", "Mix"));
    assertEquals(List.of("Mix", "Mix.Child"), paths(under));

    JSONArray tests = pages(new JSONObject().put("type", "test"));
    assertEquals(List.of("Mix.Child"), paths(tests));

    JSONArray tagged = pages(new JSONObject().put("tags", "blue, green"));
    assertEquals(List.of("Mix.Child"), paths(tagged));
    assertEquals(0, pages(new JSONObject().put("tags", "Red")).length());

    write("Later", "static", List.of());
    assertTrue(paths(pages(new JSONObject())).contains("Later"));

    JSONObject missing = call("list_pages", new JSONObject().put("path", "Nope"));
    assertTrue(missing.getBoolean("isError"));
    assertTrue(text(missing).contains("no such page: Nope"));
  }

  @Test
  public void runRejectsABadPageAndAnAdjunctAndAnUnknownTool() throws Exception {
    initialize();
    assertTrue(call("run", new JSONObject()).getBoolean("isError"));
    assertTrue(text(call("run", new JSONObject().put("page", "Mix?test"))).contains("wiki path"));
    assertTrue(text(call("run", new JSONObject().put("page", "Mix&x"))).contains("wiki path"));
    assertTrue(text(call("run", new JSONObject().put("page", "Missing"))).contains("no such page"));
    assertTrue(text(call("run", new JSONObject().put("page", "SetUp"))).contains("error loading page"));
    assertTrue(text(call("run", new JSONObject().put("page", "Mix").put("suite", "yes"))).contains("suite must be a boolean"));
    assertTrue(text(call("run", new JSONObject().put("page", 1))).contains("page must be a string"));
    assertTrue(text(call("nope", new JSONObject())).contains("unknown tool"));
    assertTrue(text(call("get_results", new JSONObject())).contains("runId is required"));
    assertTrue(text(call("get_results", new JSONObject().put("runId", "missing"))).contains("no such run"));
    JSONObject unnamed = request("tools/call", new JSONObject());
    assertEquals(-32602, unnamed.getJSONObject("error").getInt("code"));
    JSONObject nullName = request("tools/call", new JSONObject().put("name", JSONObject.NULL));
    assertEquals(-32602, nullName.getJSONObject("error").getInt("code"));
    JSONObject numberName = request("tools/call", new JSONObject().put("name", 1));
    assertEquals(-32602, numberName.getJSONObject("error").getInt("code"));
    JSONObject badArgs = request("tools/call", new JSONObject().put("name", "list_pages").put("arguments", new JSONArray()));
    assertEquals(-32602, badArgs.getJSONObject("error").getInt("code"));
    JSONObject nullArgs = request("tools/call", new JSONObject().put("name", "list_pages").put("arguments", JSONObject.NULL));
    assertTrue(nullArgs.getJSONObject("result").has("content"));
    JSONObject noArgs = request("tools/call", new JSONObject().put("name", "list_pages"));
    assertTrue(noArgs.getJSONObject("result").has("content"));
    JSONObject nullPage = call("run", new JSONObject().put("page", JSONObject.NULL));
    assertTrue(text(nullPage), text(nullPage).contains("page is required"));
    JSONObject inferred = call("run", new JSONObject().put("page", "SetUp").put("suite", JSONObject.NULL));
    assertTrue(text(inferred), text(inferred).contains("error loading page"));
    JSONObject blank = call("run", new JSONObject().put("page", "   "));
    assertTrue(text(blank), text(blank).contains("page is required"));
    JSONObject spaced = call("run", new JSONObject().put("page", "not a page"));
    assertTrue(text(spaced), text(spaced).contains("no such page"));
    assertEquals("run failed", WikiTools.failure(new Exception()));
    assertEquals("run failed", WikiTools.failure(new Exception(" ")));
    assertEquals("boom", WikiTools.failure(new Exception("boom")));
    write("BlankTags", "static", List.of(" "));
    write("Hole", "static", List.of("a", "", "b"));
    JSONArray blankTags = pages(new JSONObject().put("path", "BlankTags"));
    assertEquals(0, blankTags.getJSONObject(0).getJSONArray("tags").length());
    assertEquals(List.of("a", "b"), strings(pages(new JSONObject().put("path", "Hole")).getJSONObject(0).getJSONArray("tags")));
  }

  @Test
  public void aColonInAFilterOrARerunPathIsEncodedAndTheRunStillWorks() throws Exception {
    write("Nest", "suite", List.of(), "");
    write("Nest.Bird", "test", List.of("a:b"), echo("hello", "hello"));
    write("Nest.TaggedFail", "test", List.of("a:b"), echo("hello", "goodbye"));
    write("Nest.UntaggedFail", "test", List.of(), echo("hello", "goodbye"));
    writeFile(home.resolve("a:b.json"), "{\"failedPages\":[\"Nest.Bird\",\"Nest.UntaggedFail\"]}");
    initialize();
    JSONObject started = call("run", new JSONObject()
      .put("page", "Nest")
      .put("tags", "a:b")
      .put("rerunFailures", home.resolve("a:b.json").toString()));
    assertFalse(text(started), started.optBoolean("isError"));
    String id = new JSONObject(text(started)).getString("runId");
    JSONObject results = call("get_results", new JSONObject().put("runId", id));
    String document = text(results);
    assertTrue(document, document.contains("\"schemaVersion\": 1"));
    assertTrue(document, document.contains("\n"));
    assertTrue(document, document.contains("Nest.Bird"));
    assertFalse(document, document.contains("TaggedFail"));
    assertFalse(document, document.contains("UntaggedFail"));
    assertEquals(0, new JSONObject(document).getJSONObject("run").getJSONArray("failedPages").length());
    assertFalse(results.has("isError"));
  }

  private void initialize() throws Exception {
    request("initialize", new JSONObject().put("protocolVersion", McpServer.LATEST));
    notify("notifications/initialized");
  }

  private JSONArray pages(JSONObject arguments) throws Exception {
    JSONObject result = call("list_pages", arguments);
    assertFalse(text(result), result.optBoolean("isError"));
    return new JSONArray(text(result));
  }

  private JSONObject call(String name, JSONObject arguments) throws Exception {
    JSONObject response = request("tools/call", new JSONObject().put("name", name).put("arguments", arguments));
    return response.getJSONObject("result");
  }

  private static String text(JSONObject result) {
    return result.getJSONArray("content").getJSONObject(0).getString("text");
  }

  private JSONObject request(String method, JSONObject params) throws Exception {
    return request(method, params, 1);
  }

  private JSONObject request(String method, JSONObject params, Object id) throws Exception {
    JSONObject message = new JSONObject().put("jsonrpc", "2.0").put("id", id).put("method", method);
    if (params != null) {
      message.put("params", params);
    }
    return roundTrip(message.toString());
  }

  private void notify(String method) throws Exception {
    raw(new JSONObject().put("jsonrpc", "2.0").put("method", method).toString());
  }

  private int error(String line) throws Exception {
    return roundTrip(line).getJSONObject("error").getInt("code");
  }

  private JSONObject roundTrip(String line) throws Exception {
    raw(line);
    String response = readLine();
    assertNotNull(response);
    assertTrue(response, response.startsWith("{"));
    assertFalse(response, response.contains("\n"));
    return new JSONObject(response);
  }

  private void raw(String line) throws Exception {
    toServer.write(line);
    toServer.write("\n");
    toServer.flush();
  }

  private String readLine() throws Exception {
    Future<String> line = readers.submit(() -> fromServer.readLine());
    try {
      return line.get(10, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      fail("no response");
      return null;
    } catch (ExecutionException e) {
      throw new IllegalStateException(e.getCause());
    }
  }

  private void write(String name, String type, List<String> tags) throws Exception {
    write(name, type, tags, "");
  }

  private void write(String name, String type, List<String> tags, String body) throws Exception {
    Path file = home.resolve("Wiki").resolve(name.replace('.', '/') + ".md");
    Files.createDirectories(file.getParent());
    Files.writeString(file, FrontMatter.write(type, tags, null, false, body));
  }

  private static void writeFile(Path file, String text) throws Exception {
    Files.writeString(file, text);
  }

  private static String echo(String sent, String expected) {
    return """
      | script | agnesse.fixtures.EchoFixture |
      | --- | --- | --- |
      | check | echo | %s | %s |
      """.formatted(sent, expected);
  }

  private static List<String> names(JSONArray tools) {
    List<String> names = new ArrayList<>();
    for (int i = 0; i < tools.length(); i++) {
      names.add(tools.getJSONObject(i).getString("name"));
    }
    return names;
  }

  private static JSONObject tool(JSONArray tools, String name) {
    for (int i = 0; i < tools.length(); i++) {
      JSONObject tool = tools.getJSONObject(i);
      if (name.equals(tool.getString("name"))) {
        return tool;
      }
    }
    throw new AssertionError(name);
  }

  private static List<String> strings(JSONArray values) {
    List<String> strings = new ArrayList<>();
    for (int i = 0; i < values.length(); i++) {
      strings.add(values.getString(i));
    }
    return strings;
  }

  private static List<String> paths(JSONArray pages) {
    List<String> paths = new ArrayList<>();
    for (int i = 0; i < pages.length(); i++) {
      paths.add(pages.getJSONObject(i).getString("path"));
    }
    return paths;
  }
}
