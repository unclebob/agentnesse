package fitnesse.responders.editing;

import static org.junit.Assert.assertEquals;
import static util.RegexTestCase.assertSubString;

import fitnesse.FitNesseContext;
import fitnesse.http.MockRequest;
import fitnesse.http.SimpleResponse;
import fitnesse.testutil.FitNesseUtil;
import org.junit.Before;
import org.junit.Test;

public class NewPageResponderTest {

  private FitNesseContext context;
  private MockRequest request;
  private NewPageResponder responder;

  @Before
  public void setUp() throws Exception {
    context = FitNesseUtil.makeTestContext();
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
    assertSubString("name=\"" + EditResponder.HELP_TEXT + "\"", body);
    assertSubString("type=\"submit\"", body);
    assertSubString("textarea class=\"wikitext no_wrap mousetrap\"", body);
  }

}
