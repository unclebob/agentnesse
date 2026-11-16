package agnesse.responders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

import agnesse.wiki.WikiPageUtil;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.http.MockRequest;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;

public class PacketResponderTest {
  protected WikiPage root;
  protected MockRequest request;
  protected Responder responder;
  protected AgNesseContext context;

  @Before
  public void setUp() throws Exception {
    context = AgNesseUtil.makeTestContext();
    root = context.getRootPage();
    request = new MockRequest();
    responder = new PacketResponder();
  }

  private SimpleResponse makeResponse() throws Exception {
    return (SimpleResponse) responder.makeResponse(context, request);
  }

  private void assertResponseContentEquals(String expected, SimpleResponse response) {
    if (expected.startsWith("{")) {
      assertJsonEquals(expected, response.getContent());
    } else {
      assertEquals(expected, response.getContent());
    }
  }

  private void assertPageWithTableResponseWith(String table, String expected) throws Exception {
    WikiPageUtil.addPage(root, PathParser.parse("TablePage"), table);
    request.setResource("TablePage");
    SimpleResponse response = makeResponse();
    assertResponseContentEquals(expected, response);
  }

  @Test
  public void noSuchPage() throws Exception {
    request.setResource("NoSuchPage");
    SimpleResponse response = makeResponse();
    assertEquals(404, response.getStatus());
  }

  @Test
  public void pageWithNoTables() throws Exception {
    WikiPageUtil.addPage(root, PathParser.parse("SimplePage"), "simple content");
    request.setResource("SimplePage");
    SimpleResponse response = makeResponse();
    assertEquals(200, response.getStatus());
    assertJsonEquals("{\"page\":\"SimplePage\",\"tables\":[]}", response.getContent());
  }

  @Test
  public void pageWithOneSimpleTable() throws Exception {
    assertPageWithTableResponseWith("|cell|\n", "{\"page\":\"TablePage\",\"tables\":[{\"rows\":[[\"cell\"]]}]}");
  }

  @Test
  public void pageWithOneSimplePair() throws Exception {
    assertPageWithTableResponseWith("|name|value|\n", "{\"page\":\"TablePage\",\"tables\":[{\"rows\":[[\"name\",\"value\"]]}]}");
  }

  @Test
  public void pageWithTwoPairs() throws Exception {
    assertPageWithTableResponseWith("|name1|value1|\n|name2|value2|\n",
      "{\"page\":\"TablePage\",\"tables\":[{\"rows\":[[\"name1\",\"value1\"],[\"name2\",\"value2\"]]}]}");
  }

  @Test
  public void twoTablesWithSimplePairs() throws Exception {
    assertPageWithTableResponseWith("|n|v|\n\n|n2|v2|\n",
      "{\"page\":\"TablePage\",\"tables\":[{\"rows\":[[\"n\",\"v\"]]},{\"rows\":[[\"n2\",\"v2\"]]}]}");
  }

  @Test
  public void oneTableWithNestedPair() throws Exception {
    assertPageWithTableResponseWith("|n|\n||m|v|\n",
      "{\"page\":\"TablePage\",\"tables\":[{\"rows\":[[\"n\"],[\"\",\"m\",\"v\"]]}]}");
  }

  @Test
  public void tableWithBlankLinesIgnored() throws Exception {
    assertPageWithTableResponseWith("|n|\n||||\n||m|v|\n",
      "{\"page\":\"TablePage\",\"tables\":[{\"rows\":[[\"n\"],[\"\"],[\"\",\"m\",\"v\"]]}]}");
  }

  @Test
  public void deeplyNestedTable() throws Exception {
    String table =
      "|bob|\n" +
        "||Angela|\n" +
        "|||Lexy|6|\n" +
        "|||Sami|4|\n" +
        "|||Mandy|2|\n" +
        "||Micah|\n" +
        "|||Luka|5|\n" +
        "||Gina|\n" +
        "||Justin|\n";
    assertPageWithTableResponseWith(table,
      "{\"page\":\"TablePage\",\"tables\":[{\"rows\":[" +
        "[\"bob\"],[\"\",\"Angela\"],[\"\",\"\",\"Lexy\",\"6\"],[\"\",\"\",\"Sami\",\"4\"],[\"\",\"\",\"Mandy\",\"2\"]," +
        "[\"\",\"Micah\"],[\"\",\"\",\"Luka\",\"5\"],[\"\",\"Gina\"],[\"\",\"Justin\"]" +
        "]}]}");
  }

  @Test
  public void jsonpQueryArgument() throws Exception {
    request.addInput("jsonp", "load");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals(400, response.getStatus());
    assertTrue(response.getContent().contains("Unknown parameter jsonp"));
  }
}
