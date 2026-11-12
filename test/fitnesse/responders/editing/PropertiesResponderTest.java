// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.editing;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.MockRequest;
import fitnesse.http.SimpleResponse;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PageType;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageProperty;
import fitnesse.wiki.WikiPageUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static util.RegexTestCase.assertDoesntHaveRegexp;
import static util.RegexTestCase.assertHasRegexp;
import static util.RegexTestCase.assertMatches;
import static util.RegexTestCase.assertNotSubString;
import static util.RegexTestCase.assertSubString;

public class PropertiesResponderTest {
  private FitNesseContext context;

  private WikiPage root;

  private MockRequest request;

  private Responder responder;

  private String content;

  @Before
  public void setUp() throws Exception {
    context = FitNesseUtil.makeTestContext();
    root = context.getRootPage();
    request = new MockRequest();
  }

  @Test
  public void testResponse() throws Exception {
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("PageOne"), "");
    PageData data = page.getData();
    data.setContent("some content");
    WikiPageProperty properties = data.getProperties();
    properties.set(PageData.PropertySUITES, "Page Tags");
    properties.set("Test", "true");
    page.commit(data);

    MockRequest request = new MockRequest();
    request.setResource("PageOne");

    Responder responder = new PropertiesResponder();
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals("max-age=0", response.getHeader("Cache-Control"));

    String content = response.getContent();
    assertSubString("PageOne", content);
    assertDoesntHaveRegexp("textarea name=\"extensionXml\"", content);
    assertHasRegexp("<input.*value=\"Save Properties\".*>", content);

    assertHasRegexp("<input.*value=\"saveProperties\"", content);
    assertSubString(" value=\"Page Tags\" ", content);
    for (String attribute : new String[]{"Search", "Edit", "Properties", "Versions", "Refactor", "WhereUsed", "RecentChanges"})
      assertCheckboxChecked(attribute, content);

    assertCheckboxNotChecked(content, "Prune");
  }

  private void assertCheckboxNotChecked(String content, String attribute) {
    assertSubString("<input type=\"checkbox\" id=\"" + attribute + "\" name=\"" + attribute + "\"/>", content);
  }

  private void assertCheckboxChecked(String attribute, String content) {
    assertSubString("<input type=\"checkbox\" id=\"" + attribute + "\" name=\"" + attribute + "\" checked=\"checked\"/>", content);
  }

  @Test
  public void testJsonResponse() throws Exception {
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("PageOne"));
    PageData data = page.getData();
    WikiPageProperty properties = data.getProperties();
    properties.set(PageType.TEST.toString(), "true");
    page.commit(data);

    MockRequest request = new MockRequest();
    request.setResource("PageOne");
    request.addInput("format", "json");

    Responder responder = new PropertiesResponder();
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals("application/json", response.getContentType());
    String jsonText = response.getContent();
    JSONObject jsonObject = new JSONObject(jsonText);
    assertTrue(jsonObject.getBoolean("Test"));
    assertTrue(jsonObject.getBoolean("Search"));
    assertTrue(jsonObject.getBoolean("Edit"));
    assertTrue(jsonObject.getBoolean("Properties"));
    assertTrue(jsonObject.getBoolean("Versions"));
    assertTrue(jsonObject.getBoolean("Refactor"));
    assertTrue(jsonObject.getBoolean("WhereUsed"));
    assertTrue(jsonObject.getBoolean("RecentChanges"));
    assertTrue(jsonObject.getBoolean("Files"));

    assertFalse(jsonObject.has("Help"));
    assertFalse(jsonObject.getBoolean("Suite"));
    assertFalse(jsonObject.getBoolean("Prune"));
  }

  @Test
  public void testJsonResponseWithHelpTextAndTags() throws Exception {
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("PageOne"));
    PageData data = page.getData();
    WikiPageProperty properties = data.getProperties();
    properties.set(WikiPageProperty.HELP, "help text");
    properties.set(WikiPageProperty.SUITES, "foo,bar");
    page.commit(data);

    MockRequest request = new MockRequest();
    request.setResource("PageOne");
    request.addInput("format", "json");

    Responder responder = new PropertiesResponder();
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals("application/json", response.getContentType());
    String jsonText = response.getContent();
    JSONObject jsonObject = new JSONObject(jsonText);

    assertEquals("help text", jsonObject.getString("Help"));
    JSONArray suites = jsonObject.getJSONArray("Suites");
    assertEquals("foo", suites.getString(0));
    assertEquals("bar", suites.getString(1));

    assertFalse(jsonObject.getBoolean("Test"));
    assertFalse(jsonObject.getBoolean("Suite"));
    assertFalse(jsonObject.getBoolean("Prune"));
  }

  @Test
  public void testUsernameDisplayed() throws Exception {
    WikiPage page = getContentFromSimplePropertiesPage();

    assertSubString("Last modified anonymously", content);

    PageData data = page.getData();
    data.setAttribute(PageData.LAST_MODIFYING_USER, "Bill");
    page.commit(data);

    request.setResource("SomePage");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    content = response.getContent();

    assertSubString("Last modified by Bill", content);
  }

  private WikiPage getContentFromSimplePropertiesPage() throws Exception {
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("SomePage"));

    return getPropertiesContentFromPage(page);
  }

  private WikiPage getPropertiesContentFromPage(WikiPage page) throws Exception {
    request = new MockRequest();
    request.setResource(page.getName());
    responder = new PropertiesResponder();
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    content = response.getContent();
    return page;
  }

  @Test
  public void testPageTypePropertiesHtml() throws Exception {
    SimpleResponse response = (SimpleResponse) new PropertiesResponder().makeResponse(context, request);
    String html = response.getContent();
    assertSubString("Page type:", html);
    assertSubString("<input type=\"radio\" id=\"Static\" name=\"pageType\" value=\"Static\" checked=\"checked\"/>", html);
    assertSubString("<input type=\"radio\" id=\"Test\" name=\"pageType\" value=\"Test\"/>", html);
    assertSubString("<input type=\"radio\" id=\"Suite\" name=\"pageType\" value=\"Suite\"/>", html);
    assertSubString("<input type=\"checkbox\" id=\"Prune\" name=\"Prune\"/>", html);
  }

  @Test
  public void testPageTypePropertiesSuiteHtml() throws Exception {
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("SomePage"));
    PageData data = page.getData();
    data.setAttribute("Suite");
    page.commit(data);
    assertEquals(page, context.getRootPage().getPageCrawler().getPage(PathParser.parse(".SomePage")));
    request.setResource(page.getFullPath().toString());
    SimpleResponse response = (SimpleResponse) new PropertiesResponder().makeResponse(context, request);
    String html = response.getContent();
    assertSubString("Page type:", html);
    assertSubString("<input type=\"radio\" id=\"Static\" name=\"pageType\" value=\"Static\"/>", html);
    assertSubString("<input type=\"radio\" id=\"Test\" name=\"pageType\" value=\"Test\"/>", html);
    assertSubString("<input type=\"radio\" id=\"Suite\" name=\"pageType\" value=\"Suite\" checked=\"checked\"/>", html);
    assertSubString("<input type=\"checkbox\" id=\"Prune\" name=\"Prune\"/>", html);
  }

  @Test
  public void testPageTypePropertiesTestHtml() throws Exception {
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("SomePage"));
    PageData data = page.getData();
    data.setAttribute("Test");
    page.commit(data);
    request.setResource(page.getFullPath().toString());
    SimpleResponse response = (SimpleResponse) new PropertiesResponder().makeResponse(context, request);
    String html = response.getContent();
    assertSubString("Page type:", html);
    assertSubString("<input type=\"radio\" id=\"Static\" name=\"pageType\" value=\"Static\"/>", html);
    assertSubString("<input type=\"radio\" id=\"Test\" name=\"pageType\" value=\"Test\" checked=\"checked\"/>", html);
    assertSubString("<input type=\"radio\" id=\"Suite\" name=\"pageType\" value=\"Suite\"/>", html);
    assertSubString("<input type=\"checkbox\" id=\"Prune\" name=\"Prune\"/>", html);
  }

  @Test
  public void testPageTypePropertiesSkippedHtml() throws Exception {
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("SomePage"));
    PageData data = page.getData();
    data.setAttribute("Prune");
    page.commit(data);
    request.setResource(page.getFullPath().toString());
    SimpleResponse response = (SimpleResponse) new PropertiesResponder().makeResponse(context, request);
    String html = response.getContent();
    assertSubString("Page type:", html);
    assertSubString("<input type=\"checkbox\" id=\"Prune\" name=\"Prune\" checked=\"checked\"/>", html);
  }

  @Test
  public void testActionPropertiesHtml() throws Exception {
    SimpleResponse response = (SimpleResponse) new PropertiesResponder().makeResponse(context, request);
    String html = response.getContent();
    assertSubString("Actions:", html);
    assertSubString("<input type=\"checkbox\" id=\"Edit\" name=\"Edit\" checked=\"checked\"/>", html);
    assertSubString("<input type=\"checkbox\" id=\"Versions\" name=\"Versions\" checked=\"checked\"/>", html);
    assertSubString("<input type=\"checkbox\" id=\"Properties\" name=\"Properties\" checked=\"checked\"/>", html);
    assertSubString("<input type=\"checkbox\" id=\"Refactor\" name=\"Refactor\" checked=\"checked\"/>", html);
    assertSubString("<input type=\"checkbox\" id=\"WhereUsed\" name=\"WhereUsed\" checked=\"checked\"/>", html);
  }

  @Test
  public void testMakeNavigationPropertiesHtml() throws Exception {
    SimpleResponse response = (SimpleResponse) new PropertiesResponder().makeResponse(context, request);
    String html = response.getContent();
    assertSubString("Navigation:", html);
    assertSubString("<input type=\"checkbox\" id=\"Files\" name=\"Files\" checked=\"checked\"/>", html);
    assertSubString("<input type=\"checkbox\" id=\"RecentChanges\" name=\"RecentChanges\" checked=\"checked\"/>", html);
    assertSubString("<input type=\"checkbox\" id=\"Search\" name=\"Search\" checked=\"checked\"/>", html);
  }

  @Test
  public void testEmptySuitesForm() throws Exception {
    getContentFromSimplePropertiesPage();

    assertSubString("Suites", content);
    assertSubString("<input type=\"text\" id=\"Suites\" title=\"Separate tags by a comma\" name=\"Suites\" value=\"\"", content);
  }

  @Test
  public void testSuitesDisplayed() throws Exception {
    WikiPage page = getContentFromSimplePropertiesPage();
    PageData data = page.getData();
    data.setAttribute(WikiPageProperty.SUITES, "smoke");
    page.commit(data);

    getPropertiesContentFromPage(page);

    assertSubString("Suites", content);
    assertSubString("<input type=\"text\" id=\"Suites\" title=\"Separate tags by a comma\" name=\"Suites\" value=\"smoke\"", content);
    assertSubString(" value=\"smoke\" ", content);
  }
}
