package agnesse.responders;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.http.MockRequest;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScopeVariablesResponderTest {
  protected WikiPage root;
  protected MockRequest request;
  protected Responder responder;
  protected AgNesseContext context;

  @Before
  public void setUp() throws Exception {
    context = AgNesseUtil.makeTestContext();
    root = context.getRootPage();
    request = new MockRequest();
    responder = new ScopeVariablesResponder();
    WikiPageUtil.addPage(root, PathParser.parse("SimplePageForInclude"), define("y", "Y"));
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("SimplePage"),
      define("x", "X") + """
      ```agnesse
      include .SimplePageForInclude
      ```
      """);
    WikiPage childPage = WikiPageUtil.addPage(page, PathParser.parse("ChildPage"), define("z", "Z") + define("a", "A"));
    WikiPageUtil.addPage(childPage, PathParser.parse("GrandChildPage"), define("a", "B") + define("b", "BB"));
  }

  private static String define(String name, String value) {
    return "```agnesse\ndefine " + name + "\n" + value + "\n```\n";
  }

  @Test
  public void shouldListVariables() throws Exception {
    request.setResource("SimplePage.ChildPage.GrandChildPage");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context,request);
    JSONArray variables = new JSONObject(response.getContent()).getJSONArray("variables");
    assertEquals("X", value(variables, "x", "SimplePage"));
    assertEquals("Y", value(variables, "y", "SimplePage"));
    assertEquals("Z", value(variables, "z", "SimplePage.ChildPage"));
    assertEquals("B", value(variables, "a", "SimplePage.ChildPage.GrandChildPage"));
    assertEquals("BB", value(variables, "b", "SimplePage.ChildPage.GrandChildPage"));
  }

  @Test
  public void anUnknownParameterIsRejected() throws Exception {
    request.setResource("SimplePage");
    request.addInput("extra", "1");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals(400, response.getStatus());
    assertTrue(response.getContent().contains("Unknown parameter extra"));
  }

  @Test
  public void aMissingPageIsNotFound() throws Exception {
    request.setResource("MissingPage");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals(404, response.getStatus());
    assertTrue(response.getContent().contains("No page MissingPage"));
  }

  private static String value(JSONArray variables, String name, String page) {
    for (int i = 0; i < variables.length(); i++) {
      JSONObject variable = variables.getJSONObject(i);
      if (name.equals(variable.getString("name")) && page.equals(variable.getString("page"))) {
        return variable.getString("value");
      }
    }
    return null;
  }
}
