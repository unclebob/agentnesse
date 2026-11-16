package agnesse.responders;

import static org.junit.Assert.assertEquals;
import static util.RegexTestCase.assertHasRegexp;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.http.MockRequest;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;
import org.junit.Before;
import org.junit.Test;

public class DisabledResponderTest {

  private AgNesseContext context;

  @Before
  public void setUp() {
    context = AgNesseUtil.makeTestContext();
  }

  @Test
  public void testResponse() throws Exception {
    Responder responder = new DisabledResponder();
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, new MockRequest());

    assertEquals(403, response.getStatus());
    assertHasRegexp("\"error\": \"disabled\"", response.getContent());
    assertHasRegexp("This responder is disabled", response.getContent());
  }

}
