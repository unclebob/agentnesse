package agnesse.responders;

import static org.junit.Assert.assertEquals;
import static util.RegexTestCase.assertHasRegexp;

import agnesse.FitNesseContext;
import agnesse.Responder;
import agnesse.http.MockRequest;
import agnesse.http.SimpleResponse;
import agnesse.testutil.FitNesseUtil;
import org.junit.Before;
import org.junit.Test;

public class DisabledResponderTest {

  private FitNesseContext context;

  @Before
  public void setUp() {
    context = FitNesseUtil.makeTestContext();
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
