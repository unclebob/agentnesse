// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.editing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static util.RegexTestCase.assertHasRegexp;
import static util.RegexTestCase.assertSubString;

import agnesse.AgNesseContext;
import agnesse.http.MockRequest;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.FileBasedWikiPage;
import agnesse.wiki.fs.MemoryFileSystem;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class SaveResponderTest {
  private AgNesseContext context;
  private WikiPage root;
  private Response response;
  public MockRequest request;
  public SaveResponder responder;

  @Before
  public void setUp() throws Exception {
    context = AgNesseUtil.makeTestContext();
    root = context.getRootPage();
    request = new MockRequest();
    responder = new SaveResponder();
  }

  @Test
  public void testResponse() throws Exception {
    WikiPageUtil.addPage(root, PathParser.parse("ChildPage"));
    prepareRequest("ChildPage");

    Response response = responder.makeResponse(context, request);
    assertEquals(303, response.getStatus());
    assertHasRegexp("Location: /ChildPage", response.makeHttpHeaders());

    String newContent = root.getChildPage("ChildPage").getData().getContent();
    assertEquals("some new content", newContent);
  }

  private void prepareRequest(String pageName) {
    request.setResource(pageName);
    request.addInput(EditResponder.CONTENT_INPUT_NAME, "some new content");
    request.addInput(EditResponder.HELP_TEXT, "some help");
  }

  @Test
  public void testResponseWithRedirect() throws Exception {
    WikiPageUtil.addPage(root, PathParser.parse("ChildPage"));
    prepareRequest("ChildPage");
    request.addInput("redirect", "http://fitnesse.org:8080/SomePage");

    Response response = responder.makeResponse(context, request);
    assertEquals(303, response.getStatus());
    assertHasRegexp("Location: http://fitnesse.org:8080/SomePage", response.makeHttpHeaders());
  }

  @Test
  public void testCanCreatePage() throws Exception {
    prepareRequest("ChildPageTwo");

    responder.makeResponse(context, request);

    assertEquals(true, root.hasChildPage("ChildPageTwo"));
    String newContent = root.getChildPage("ChildPageTwo").getData().getContent();
    assertEquals("some new content", newContent);
  }

  @Test
  public void testCanCreatePageForNonWikiWord() throws Exception {
    prepareRequest("child_page_two");

    responder.makeResponse(context, request);

    assertEquals(true, root.hasChildPage("child_page_two"));
    String newContent = root.getChildPage("child_page_two").getData().getContent();
    assertEquals("some new content", newContent);
  }

  @Test
  public void testCanCreatePageWithoutTicketIdAndEditTime() throws Exception {
    request.setResource("ChildPageTwo");
    request.addInput(EditResponder.CONTENT_INPUT_NAME, "some new content");
    request.addInput(EditResponder.HELP_TEXT, "some help");
    request.addInput(EditResponder.SUITES, "some suite");

    responder.makeResponse(context, request);

    assertEquals(true, root.hasChildPage("ChildPageTwo"));
    PageData pageData = root.getChildPage("ChildPageTwo").getData();
    String newContent = pageData.getContent();
    assertEquals("some new content", newContent);
    assertEquals("some help", pageData.getAttribute(PageData.PropertyHELP));
    assertEquals("some suite", pageData.getAttribute(PageData.PropertySUITES));
  }

  @Test
  public void testRemovesHelpAndSuitesAttributeIfEmpty() throws Exception {
    request.setResource("ChildPageTwo");
    request.addInput(EditResponder.HELP_TEXT, "");
    request.addInput(EditResponder.SUITES, "");

    responder.makeResponse(context, request);

    PageData pageData = root.getChildPage("ChildPageTwo").getData();
    assertFalse("should not have help attribute", pageData.hasAttribute(PageData.PropertyHELP));
    assertFalse("should not have suites attribute", pageData.hasAttribute(PageData.PropertySUITES));
  }

  @Test
  public void testKnowsWhenToMerge() throws Exception {
    String simplePageName = "SimplePageName";
    createAndSaveANewPage(simplePageName);

    request.setResource(simplePageName);
    request.addInput(EditResponder.CONTENT_INPUT_NAME, "some new content");

    Response response = responder.makeResponse(context, request);

    assertEquals(303, response.getStatus());
    assertEquals("some new content", root.getChildPage(simplePageName).getData().getContent());
  }

  @Test
  public void testKnowWhenNotToMerge() throws Exception {
    String pageName = "NewPage";
    createAndSaveANewPage(pageName);
    String newContent = "some new Content work damn you!";
    request.setResource(pageName);
    request.addInput(EditResponder.CONTENT_INPUT_NAME, newContent);

    Response response = responder.makeResponse(context, request);
    assertEquals(303, response.getStatus());

    request.addInput(EditResponder.CONTENT_INPUT_NAME, newContent + " Ok I'm working now");
    response = responder.makeResponse(context, request);
    assertEquals(303, response.getStatus());
  }

  @Test
  public void testUsernameIsSavedInPageProperties() throws Exception {
    addRequestParameters();
    request.setCredentials("Aladdin", "open sesame");
    response = responder.makeResponse(context, request);

    String user = root.getChildPage("EditPage").getData().getAttribute(PageData.LAST_MODIFYING_USER);
    assertEquals("Aladdin", user);
  }

  private void createAndSaveANewPage(String pageName) throws Exception {
    WikiPage simplePage = WikiPageUtil.addPage(root, PathParser.parse(pageName));

    PageData data = simplePage.getData();
    simplePage.commit(data);
  }

  private void doSimpleEdit() throws Exception {
    WikiPageUtil.addPage(root, PathParser.parse("EditPage"));
    addRequestParameters();

    response = responder.makeResponse(context, request);
  }

  private void addRequestParameters() {
    prepareRequest("EditPage");
  }

  @Test
  public void testHasVersionHeader() throws Exception {
    doSimpleEdit();
    assertTrue("header missing", response.getHeader("Current-Version") != null);
  }

  @Test
  public void anUntouchedFrontMatterStaysByteForByte() throws Exception {
    MemoryFileSystem files = new MemoryFileSystem();
    context = AgNesseUtil.makeTestContext(files);
    root = context.getRootPage();
    File page = new File(((FileBasedWikiPage) root).getFileSystemPath(), "Kept.md");
    String json = "{ \"type\" : \"test\", \"tags\" : [ \"keep\" ], \"help\" : \"h\", \"prune\" : false, \"user\" : \"ada\" }";
    files.makeFile(page, json + "\nOLD");

    request.setResource("Kept");
    request.addInput(EditResponder.CONTENT_INPUT_NAME, "NEW");
    request.addInput(EditResponder.FRONT_MATTER, json);
    request.addInput(EditResponder.HELP_TEXT, "from the field");
    request.addInput(EditResponder.SUITES, "from-the-field");
    request.setCredentials("bob", "secret");

    response = responder.makeResponse(context, request);

    assertEquals(303, response.getStatus());
    assertEquals(json + "\nNEW", files.getContent(page));
    PageData data = root.getChildPage("Kept").getData();
    assertEquals("NEW", data.getContent());
    assertEquals("ada", data.getAttribute(PageData.LAST_MODIFYING_USER));
    assertEquals("h", data.getAttribute(PageData.PropertyHELP));
    assertEquals("keep", data.getAttribute(PageData.PropertySUITES));
    assertTrue(data.hasAttribute("Test"));
  }

  @Test
  public void editedFrontMatterReplacesTheProperties() throws Exception {
    MemoryFileSystem files = new MemoryFileSystem();
    context = AgNesseUtil.makeTestContext(files);
    root = context.getRootPage();
    File page = new File(((FileBasedWikiPage) root).getFileSystemPath(), "Kept.md");
    files.makeFile(page, "{ \"type\" : \"static\" }\nOLD");

    request.setResource("Kept");
    request.addInput(EditResponder.CONTENT_INPUT_NAME, "NEW");
    request.addInput(EditResponder.FRONT_MATTER, "{ \"type\": \"suite\", \"tags\": [\"later\"], \"help\": \"how\", \"prune\": true, \"user\": \"ada\" }");
    request.addInput(EditResponder.HELP_TEXT, "from the field");
    request.addInput(EditResponder.SUITES, "from-the-field");
    request.setCredentials("bob", "secret");

    response = responder.makeResponse(context, request);

    assertEquals(303, response.getStatus());
    String saved = files.getContent(page);
    assertTrue(saved.contains("\"type\": \"suite\""));
    assertTrue(saved.contains("\"later\""));
    assertFalse(saved.contains("\"type\" : \"static\""));
    PageData data = root.getChildPage("Kept").getData();
    assertEquals("NEW", data.getContent());
    assertTrue(data.hasAttribute("Suite"));
    assertFalse(data.hasAttribute("Test"));
    assertEquals("later", data.getAttribute(PageData.PropertySUITES));
    assertEquals("how", data.getAttribute(PageData.PropertyHELP));
    assertTrue(data.hasAttribute(PageData.PropertyPRUNE));
    assertEquals("ada", data.getAttribute(PageData.LAST_MODIFYING_USER));
  }

  @Test
  public void blankFrontMatterRemovesTheObject() throws Exception {
    MemoryFileSystem files = new MemoryFileSystem();
    context = AgNesseUtil.makeTestContext(files);
    root = context.getRootPage();
    File page = new File(((FileBasedWikiPage) root).getFileSystemPath(), "Kept.md");
    files.makeFile(page, "{ \"type\": \"test\" }\nOLD");

    request.setResource("Kept");
    request.addInput(EditResponder.CONTENT_INPUT_NAME, "NEW");
    request.addInput(EditResponder.FRONT_MATTER, "  \n");
    responder.makeResponse(context, request);

    assertEquals("NEW", files.getContent(page));
    assertFalse(root.getChildPage("Kept").getData().hasAttribute("Test"));
  }

  @Test
  public void invalidJsonDoesNotSaveAndShowsTheEditPage() throws Exception {
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("Kept"), "original");
    page.commit(page.getData());
    request.setResource("Kept");
    request.addInput(EditResponder.CONTENT_INPUT_NAME, "KEPT<b>");
    request.addInput(EditResponder.FRONT_MATTER, "{ \"type\":");
    request.addInput("redirect", "/elsewhere");

    response = responder.makeResponse(context, request);

    assertEquals(400, response.getStatus());
    String body = ((SimpleResponse) response).getContent();
    assertSubString("malformed JSON", body);
    assertSubString("KEPT&lt;b&gt;", body);
    assertSubString("{ \"type\":", body);
    assertSubString("Front Matter ▾", body);
    assertSubString("id=\"front-matter\" open", body);
    assertSubString("value=\"/elsewhere\"", body);
    assertEquals("original", root.getChildPage("Kept").getData().getContent());
  }

  @Test
  public void anInvalidTypeDoesNotSaveOrCreate() throws Exception {
    request.setResource("Missing");
    request.addInput(EditResponder.CONTENT_INPUT_NAME, "nope");
    request.addInput(EditResponder.FRONT_MATTER, "{ \"type\": \"wiki\" }");

    response = responder.makeResponse(context, request);

    assertEquals(400, response.getStatus());
    String body = ((SimpleResponse) response).getContent();
    assertSubString("wiki", body);
    assertSubString("nope", body);
    assertSubString("{ \"type\": \"wiki\" }", body);
    assertFalse(body.contains("name=\"redirect\""));
    assertFalse(root.hasChildPage("Missing"));
  }
}
