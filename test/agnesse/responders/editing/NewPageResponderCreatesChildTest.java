package agnesse.responders.editing;

import static org.junit.Assert.*;
import static util.RegexTestCase.assertSubString;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.http.MockRequest;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;
import agnesse.markdown.FrontMatter;
import agnesse.wiki.PageCrawler;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;
import agnesse.wiki.WikiPageUtil;
import org.junit.Before;
import org.junit.Test;

public class NewPageResponderCreatesChildTest {
  private PageData childPageData;
  private PageCrawler crawler;
  private String childName;
  private MockRequest request;
  private AgNesseContext context;
  private Responder responder;
  private WikiPagePath path;
  private WikiPage root;

  @Before
  public void setUp() throws Exception {
    context = AgNesseUtil.makeTestContext();
    root = context.getRootPage();

    crawler = root.getPageCrawler();
    WikiPageUtil.addPage(root, PathParser.parse("TestPage"), "");
    childName = "ChildPage";
    String childContent = "child content";
    String pagetype = "";
    request = new MockRequest();
    request.setResource("TestPage");
    request.addInput("pageName", childName);
    request.addInput("pageContent", childContent);
    request.addInput("pageType", pagetype);
    responder = new NewPageResponder();
    path = PathParser.parse("TestPage.ChildPage");
  }

  @Test
  public void aMissingParentIsNotFound() throws Exception {
    request.setResource("NoSuchParent");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals(404, response.getStatus());
    assertSubString("No page NoSuchParent", response.getContent());
    assertEquals(null, crawler.getPage(path));
  }

  @Test
  public void missingContentAndTypeUseTheDefaults() throws Exception {
    request = new MockRequest();
    request.setResource("TestPage");
    request.addInput("pageName", "TestChild");
    responder.makeResponse(context, request);
    getChildPage("TestChild");
    assertTrue(isTest());
    assertFalse(isSuite());
    assertSubString("```agnesse\ncontents\n```", childPageData.getContent());
  }

  @Test
  public void canGetRedirectResponse() throws Exception {
    final SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    final String body = response.getContent();
    assertEquals("", body);
    assertEquals(response.getStatus(), 303);
  }

  @Test
  public void childPageIsMade() throws Exception {
    String helpText = "help text";
    String suites = "tag";
    request.addInput("helpText", helpText);
    request.addInput("suites", suites);
    assertTrue(crawler.getPage(path) == null);
    responder.makeResponse(context, request);
    assertTrue(crawler.getPage(path) != null);
    getChildPage(childName);
    assertEquals(suites, childPageData.getAttribute("Suites"));
    assertEquals(helpText, childPageData.getAttribute("Help"));
  }

  @Test
  public void noPageIsMadeIfNameIsNull() throws Exception {
    request.addInput("pageName", "");
    assertTrue(crawler.getPage(path) == null);
    responder.makeResponse(context, request);
    assertTrue(crawler.getPage(path) == null);
  }

  @Test
  public void noPageIsMadeIfPageAlreadyExists() throws Exception {
    WikiPageUtil.addPage(root, PathParser.parse("TestPage." + childName), "");

    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertTrue(response.getStatus() == 409);
    assertSubString("Child page already exists", response.getContent());
  }

  @Test
  public void givesAInvalidNameErrorForAInvalidName() throws Exception {
    request = makeInvalidRequest("");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals(400, response.getStatus());
    assertSubString("Invalid Child Name", response.getContent());

    request = makeInvalidRequest("hello goodbye");
    response = (SimpleResponse) responder.makeResponse(context, request);
    assertSubString("Invalid Child Name", response.getContent());

    request = makeInvalidRequest("1man+1mission");
    response = (SimpleResponse) responder.makeResponse(context, request);
    assertSubString("Invalid Child Name", response.getContent());

    request = makeInvalidRequest("PageOne.PageTwo");
    response = (SimpleResponse) responder.makeResponse(context, request);
    assertSubString("Invalid Child Name", response.getContent());
  }

  private MockRequest makeInvalidRequest(String name) {
    MockRequest request = new MockRequest();
    request.setResource("TestPage");
    request.addInput("pageName", name);
    request.addInput("pageContent", "hello");
    request.addInput("pageType", "");
    return request;
  }

  @Test
  public void withDefaultPageTypeAndPageNameForStaticThenNoAttributeShouldBeSet() throws Exception {
    request.addInput("pageName", "StaticPage");
    responder.makeResponse(context, request);
    getChildPage("StaticPage");
    assertFalse(isSuite());
    assertFalse(isTest());
  }

  @Test
  public void withDefaultPageTypeAndPageNameForTestTheTestAttributeShouldBeSet() throws Exception {
    request.addInput("pageName", "TestPage");
    responder.makeResponse(context, request);
    getChildPage("TestPage");
    assertFalse(isSuite());
    assertTrue(isTest());
  }

  @Test
  public void withDefaultPageTypeAndPageNameForSuiteTheSuiteAttributeShouldBeSet() throws Exception {
    request.addInput("pageName", "SuitePage");
    responder.makeResponse(context, request);
    getChildPage("SuitePage");
    assertTrue(isSuite());
    assertFalse(isTest());
  }

  private boolean isSuite() {
    return childPageData.hasAttribute("Suite");
  }

  @Test
  public void correctAttributeWhenNameHasTestButAttributeIsStatic() throws Exception {
    request.addInput("pageName", "TestChildPage");
    request.addInput("pageType", "Static");
    responder.makeResponse(context, request);
    getChildPage("TestChildPage");
    assertFalse(isTest());
    assertFalse(isSuite());
  }

  @Test
  public void pageTypeShouldBeTestWhenAttributeIsTest() throws Exception {
    request.addInput("pageType", "Test");
    responder.makeResponse(context, request);
    getChildPage(childName);
    assertTrue(isTest());
    assertFalse(isSuite());
  }

  @Test
  public void pageTypeShouldBeSuiteWhenAttributeIsSuite() throws Exception {
    request.addInput("pageType", "Suite");
    responder.makeResponse(context, request);
    getChildPage(childName);
    assertFalse(isTest());
    assertTrue(isSuite());
  }

  @Test
  public void pageTypeShouldBeSuiteWhenPageNameWouldQualifyForTestPage() throws Exception {
    request.addInput("pageType", "Suite");
    request.addInput(EditResponder.PAGE_NAME, "TestChildPage");
    responder.makeResponse(context, request);
    getChildPage("TestChildPage");
    assertFalse(isTest());
    assertTrue(isSuite());
  }

  @Test
  public void jsonWinsOverThePageTypeRadioHelpAndSuites() throws Exception {
    request.addInput("pageType", "Suite");
    request.addInput("helpText", "from the field");
    request.addInput("suites", "from-the-field");
    request.addInput("frontMatter", "{ \"type\": \"test\", \"tags\": [\"from-json\"], \"help\": \"from json\", \"prune\": true, \"user\": \"ada\" }");
    request.setCredentials("bob", "secret");

    responder.makeResponse(context, request);
    getChildPage(childName);

    assertTrue(isTest());
    assertFalse(isSuite());
    assertEquals("from-json", childPageData.getAttribute("Suites"));
    assertEquals("from json", childPageData.getAttribute("Help"));
    assertTrue(childPageData.hasAttribute("Prune"));
    assertEquals("ada", childPageData.getAttribute(PageData.LAST_MODIFYING_USER));
  }

  @Test
  public void aNullTypeOnAnIncludedNameIsTestAdjunct() throws Exception {
    request.addInput("pageName", "SetUp");
    request.addInput("pageType", "Default");
    request.addInput("frontMatter", FrontMatter.defaultJson("Default"));

    responder.makeResponse(context, request);
    getChildPage("SetUp");

    assertTrue(childPageData.hasAttribute("test-adjunct"));
    assertFalse(isTest());
    assertFalse(isSuite());
  }

  @Test
  public void aNullTypeOnPageHeaderStaysStatic() throws Exception {
    request.addInput("pageName", "PageHeader");
    request.addInput("pageType", "Default");
    request.addInput("frontMatter", FrontMatter.defaultJson("Default"));

    responder.makeResponse(context, request);
    getChildPage("PageHeader");

    assertFalse(childPageData.hasAttribute("test-adjunct"));
    assertFalse(isTest());
    assertFalse(isSuite());
  }

  @Test
  public void aNullTypeFollowsThePageName() throws Exception {
    request.addInput("pageName", "TestChild");
    request.addInput("pageType", "Static");
    request.addInput("frontMatter", FrontMatter.defaultJson("Default"));

    responder.makeResponse(context, request);
    getChildPage("TestChild");

    assertTrue(isTest());
    assertFalse(isSuite());
  }

  @Test
  public void invalidFrontMatterDoesNotCreateThePage() throws Exception {
    request.addInput("pageName", "BadChild");
    request.addInput("pageContent", "KEPT<b>");
    request.addInput("pageType", "Suite");
    request.addInput("frontMatter", "{ \"type\": \"wiki\" }");

    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);

    assertEquals(400, response.getStatus());
    assertSubString("wiki", response.getContent());
    assertSubString("KEPT&lt;b&gt;", response.getContent());
    assertSubString("value=\"BadChild\"", response.getContent());
    assertSubString("Front Matter ▾", response.getContent());
    assertSubString("id=\"front-matter\" open", response.getContent());
    assertSubString("pageType_Suite", response.getContent());
    assertEquals(null, crawler.getPage(PathParser.parse("TestPage.BadChild")));
  }

  private boolean isTest() {
    return childPageData.hasAttribute("Test");
  }

  private void getChildPage(String childName) throws Exception {
    WikiPage childPage = crawler.getPage(PathParser.parse("TestPage." + childName));
    childPageData = childPage.getData();
  }
}