package agnesse.responders.search;

import agnesse.FitNesseContext;
import agnesse.http.MockRequest;
import agnesse.http.MockResponseSender;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.markdown.FrontMatter;
import agnesse.testutil.FitNesseUtil;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPageUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static util.RegexTestCase.assertSubString;

public class SearchPropertiesResponderTest {
  private FitNesseContext context;
  private MockRequest request;
  private SearchPropertiesResponder responder;

  @Before
  public void setUp() throws Exception {
    context = FitNesseUtil.makeTestContext();
    WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Kept"),
      FrontMatter.write("test", List.of("ready"), "how to start", false, "body"));
    WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Other"),
      FrontMatter.write("static", List.of(), null, true, ""));
    request = new MockRequest();
    request.addInput(Request.NOCHUNK, "");
    responder = new SearchPropertiesResponder();
  }

  @Test
  public void frontMatterFiltersAreJson() throws Exception {
    request.addInput("type", "test");
    request.addInput("tags", "ready");
    request.addInput("help", "start");
    request.addInput("prune", "false");
    assertSubString("\"path\": \"Kept\"", body());
    assertSubString("\"prune\": false", body());
  }

  @Test
  public void anUnknownPageTypeIsRejected() throws Exception {
    request.addInput("type", "nope");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals(400, response.getStatus());
    assertSubString("Unknown page type nope", response.getContent());
  }

  @Test
  public void aRemovedAttributeIsRejected() throws Exception {
    request.addInput("Suites", "ready");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals(400, response.getStatus());
    assertSubString("Unknown parameter Suites", response.getContent());
  }

  private String body() throws Exception {
    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    return sender.sentData();
  }
}
