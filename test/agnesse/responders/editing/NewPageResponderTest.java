package agnesse.responders.editing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static util.RegexTestCase.assertNotSubString;
import static util.RegexTestCase.assertSubString;

import agnesse.AgNesseContext;
import agnesse.http.MockRequest;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
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

  @Test
  public void theNewPageFormOffersTestAdjunctAndTheIncludedNames() throws Exception {
    request.setResource("root");

    String body = ((SimpleResponse) responder.makeResponse(context, request)).getContent();

    assertSubString("pageType_test-adjunct", body);
    assertSubString("value=\"test-adjunct\"", body);
    assertSubString("'SetUp'", body);
    assertSubString("'TearDown'", body);
    assertSubString("'SuiteSetUp'", body);
    assertSubString("'SuiteTearDown'", body);
    assertSubString("'ScenarioLibrary'", body);
    assertNotSubString("'PageHeader'", body);
    assertNotSubString("'PageFooter'", body);
  }

  @Test
  public void aRequestedAdjunctTypePrefillsThatFrontMatter() throws Exception {
    request.setResource("root");
    request.addInput("pageType", "test-adjunct");

    String body = ((SimpleResponse) responder.makeResponse(context, request)).getContent();

    assertSubString("\"type\": \"test-adjunct\"", body);
  }

  @Test
  public void theFormUsesThePageDefaultContent() throws Exception {
    WikiPage parent = WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Parent"),
      "```agnesse\ndefine NewPage.default.content\nUse this starter\n```\n");
    request.setResource("Parent");

    String body = ((SimpleResponse) responder.makeResponse(context, request)).getContent();

    assertSubString("Use this starter", body);
    assertEquals("Use this starter", NewPageResponder.getDefaultContent(parent));
  }

  @Test
  public void theDeprecatedDefaultIsUsedWhenTheNewNameIsUnset() throws Exception {
    WikiPage parent = WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Legacy"),
      "```agnesse\ndefine newpage.default.content\nLegacy starter\n```\n");
    assertEquals("Legacy starter", NewPageResponder.getDefaultContent(parent));
  }

  @Test
  public void theNewDefaultWinsOverTheDeprecatedName() throws Exception {
    WikiPage parent = WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Both"),
      """
      ```agnesse
      define NewPage.default.content
      New starter
      ```
      ```agnesse
      define newpage.default.content
      Legacy starter
      ```
      """);
    assertEquals("New starter", NewPageResponder.getDefaultContent(parent));
  }

  @Test
  public void aPageWithoutADefaultUsesTheBuiltInContentsBlock() throws Exception {
    WikiPage parent = WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("Blank"), "");
    assertEquals(NewPageResponder.DEFAULT_PAGE_CONTENT, NewPageResponder.getDefaultContent(parent));
  }

}
