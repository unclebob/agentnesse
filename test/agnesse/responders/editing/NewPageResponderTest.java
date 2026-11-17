package agnesse.responders.editing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static util.RegexTestCase.assertNotSubString;
import static util.RegexTestCase.assertSubString;

import agnesse.AgNesseContext;
import agnesse.http.MockRequest;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;
import org.junit.Before;
import org.junit.Test;

public class NewPageResponderTest {

  private AgNesseContext context;
  private MockRequest request;
  private NewPageResponder responder;

  @Before
  public void setUp() throws Exception {
    context = AgNesseUtil.makeTestContext();
    request = new MockRequest();
    responder = new NewPageResponder();
  }

  @Test
  public void testResponse() throws Exception {
    request.setResource("root");

    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals(200, response.getStatus());

    String body = response.getContent();
    assertSubString("<html>", body);
    assertSubString("<form", body);
    assertSubString("method=\"post\"", body);
    assertSubString("name=\"responder\"", body);
    assertSubString("name=\"" + EditResponder.FRONT_MATTER + "\"", body);
    assertSubString("Front Matter ▸", body);
    assertSubString("id=\"front-matter\">", body);
    assertSubString("\"type\": null", body);
    assertSubString("pageType_Default", body);
    assertSubString("checked=\"checked\"", body);
    assertNotSubString("name=\"" + EditResponder.HELP_TEXT + "\"", body);
    assertTrue(body.indexOf("id=\"front-matter\"") < body.indexOf("id=\"pageContent\""));
    assertSubString("type=\"submit\"", body);
    assertSubString("textarea class=\"wikitext no_wrap mousetrap\"", body);
  }

  @Test
  public void aRequestedTypePrefillsThatFrontMatter() throws Exception {
    request.setResource("root");
    request.addInput("pageType", "Test");

    String body = ((SimpleResponse) responder.makeResponse(context, request)).getContent();

    assertSubString("name=\"pageType\" value=\"Test\"", body);
    assertSubString("\"type\": \"test\"", body);
    assertSubString("input[type=radio][name=pageType]", body);
  }

}
