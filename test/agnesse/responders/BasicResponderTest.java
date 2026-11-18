package agnesse.responders;

import agnesse.AgNesseContext;
import agnesse.http.MockRequest;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPageUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static util.RegexTestCase.assertNotSubString;
import static util.RegexTestCase.assertSubString;

public class BasicResponderTest {
  private AgNesseContext context;
  private MockRequest request;

  @Before
  public void setUp() throws Exception {
    context = AgNesseUtil.makeTestContext();
    request = new MockRequest();
  }

  @Test
  public void defaultPageNamesTheAgNesseProject() throws Exception {
    WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("SomePage"), "content");
    request.setResource("SomePage");

    SimpleResponse response = (SimpleResponse) new BasicResponder().makeResponse(context, request);
    String body = response.getContent();

    assertSubString("The AgNesse project.", body);
    assertNotSubString("objectmentor.com", body);
    assertNotSubString("FitNesse development team", body);
  }

  @Test
  public void aMissingPageIsNotFound() throws Exception {
    request.setResource("MissingPage");
    SimpleResponse response = (SimpleResponse) new BasicResponder().makeResponse(context, request);
    assertEquals(404, response.getStatus());
    assertSubString("No page MissingPage", response.getContent());
  }
}
