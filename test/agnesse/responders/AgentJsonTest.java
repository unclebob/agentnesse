package agnesse.responders;

import agnesse.FitNesseContext;
import agnesse.http.MockRequest;
import agnesse.http.MockResponseSender;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.markdown.FrontMatter;
import agnesse.responders.run.InstructionResponder;
import agnesse.responders.search.SearchPropertiesResponder;
import agnesse.responders.search.SearchResponder;
import agnesse.testutil.FitNesseUtil;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AgentJsonTest {
  private FitNesseContext context;
  private MockRequest request;

  @Before
  public void setUp() {
    context = FitNesseUtil.makeTestContext();
    request = new MockRequest();
  }

  @Test
  public void namesAreJson() throws Exception {
    WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Suite"), "");
    request.setResource("");
    SimpleResponse response = (SimpleResponse) new NameWikiPageResponder().makeResponse(context, request);
    assertEquals("application/json", response.getContentType());
    assertJsonEquals("[{\"name\":\"Suite\"}]", response.getContent());
  }

  @Test
  public void packetIsTheBlockModel() throws Exception {
    WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("TablePage"), "|a|b|\n|1|2|\n");
    request.setResource("TablePage");
    SimpleResponse response = (SimpleResponse) new PacketResponder().makeResponse(context, request);
    assertJsonEquals("{\"page\":\"TablePage\",\"tables\":[{\"rows\":[[\"a\",\"b\"],[\"1\",\"2\"]]}]}", response.getContent());
    assertTrue(response.getContent().startsWith("{"));
  }

  @Test
  public void titleTextAndMethodSearchAreJson() throws Exception {
    WikiPage parent = WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Suite"), "notes");
    WikiPageUtil.addPage(parent, PathParser.parse("Alpha"), "find this text");
    WikiPageUtil.addPage(parent, PathParser.parse("Beta"), "|echo method|\n");
    request.setResource("Suite");
    request.addInput(Request.NOCHUNK, "");
    request.addInput("q", "Alpha");
    request.addInput("type", "title");
    assertJsonEquals("{\"type\":\"title\",\"q\":\"Alpha\",\"pages\":[\"Suite.Alpha\"]}",
      body(new SearchResponder()));

    request.addInput("q", "find this");
    request.addInput("type", "text");
    assertJsonEquals("{\"type\":\"text\",\"q\":\"find this\",\"pages\":[\"Suite.Alpha\"]}",
      body(new SearchResponder()));

    request.addInput("q", "echo method");
    request.addInput("type", "method");
    assertJsonEquals("{\"type\":\"method\",\"q\":\"echo method\",\"pages\":[\"Suite.Beta\"]}",
      body(new SearchResponder()));
  }

  @Test
  public void propertySearchReadsFrontMatter() throws Exception {
    WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Kept"),
      FrontMatter.write("test", List.of("ready"), "how to start", false, "body"));
    WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Other"),
      FrontMatter.write("static", List.of(), null, true, ""));
    request.setResource("");
    request.addInput(Request.NOCHUNK, "");
    request.addInput("type", "test");
    request.addInput("tags", "ready");
    request.addInput("help", "start");
    request.addInput("prune", "false");
    assertJsonEquals("{\"pages\":[{\"path\":\"Kept\",\"type\":\"test\",\"tags\":[\"ready\"],\"help\":\"how to start\",\"prune\":false}]}",
      body(new SearchPropertiesResponder()));
  }

  @Test
  public void instructionIsADryRun() throws Exception {
    WikiPage sample = WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Sample"),
      "|script|SampleClass|\n|set|SomeProperty|in put|\n");
    var data = sample.getData();
    data.setAttribute(agnesse.wiki.PageType.TEST.toString());
    sample.commit(data);
    request.setResource("Sample");
    SimpleResponse response = (SimpleResponse) new InstructionResponder().makeResponse(context, request);
    assertEquals("application/json", response.getContentType());
    assertTrue(response.getContent(), response.getContent().contains("\"path\": \"Sample\""));
    assertTrue(response.getContent(), response.getContent().contains("SampleClass"));
    assertTrue(response.getContent(), response.getContent().contains("SomeProperty"));
  }

  @Test
  public void variablesComeFromDefineBlocks() throws Exception {
    WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Page"),
      "```agentnesse\ndefine MAX\n3\n```\n");
    request.setResource("Page");
    SimpleResponse response = (SimpleResponse) new ScopeVariablesResponder().makeResponse(context, request);
    assertJsonEquals("{\"variables\":[{\"name\":\"MAX\",\"value\":\"3\",\"page\":\"Page\"}]}", response.getContent());
  }

  @Test
  public void errorsAreJsonWithoutAnAcceptHeader() throws Exception {
    request.setResource("Missing");
    assertJsonEquals("{\"status\":404,\"error\":\"notFound\",\"message\":\"No page Missing\"}",
      ((SimpleResponse) new NotFoundResponder().makeResponse(context, request)).getContent());

    MockRequest accepted = new MockRequest();
    accepted.setResource("Missing");
    assertJsonEquals("{\"status\":400,\"error\":\"error\",\"message\":\"boom\"}",
      ((SimpleResponse) new ErrorResponder("boom").makeResponse(context, accepted)).getContent());
    assertJsonEquals("{\"status\":403,\"error\":\"disabled\",\"message\":\"This responder is disabled\"}",
      ((SimpleResponse) new DisabledResponder().makeResponse(context, accepted)).getContent());
    assertJsonEquals("{\"status\":404,\"error\":\"default\",\"message\":\"No responder for Missing\"}",
      ((SimpleResponse) new DefaultResponder().makeResponse(context, accepted)).getContent());

    accepted.addInput("jsonp", "load");
    assertJsonEquals("{\"status\":400,\"error\":\"badRequest\",\"message\":\"Unknown parameter jsonp\"}",
      ((SimpleResponse) new NotFoundResponder().makeResponse(context, accepted)).getContent());
  }

  private String body(agnesse.Responder responder) throws Exception {
    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    String sent = sender.sentData();
    int json = sent.indexOf('{');
    return json < 0 ? sent : sent.substring(json);
  }
}
