// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static util.RegexTestCase.assertNotSubString;
import static util.RegexTestCase.assertSubString;

import java.io.File;
import java.util.List;

import agnesse.FitNesseContext;
import agnesse.Responder;
import agnesse.http.MockRequest;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.responders.ResponderTestCase;
import agnesse.testutil.FitNesseUtil;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.DiskFileSystem;
import agnesse.wiki.fs.WikiFiles;
import org.junit.Test;

public class DeletePageResponderTest extends ResponderTestCase {
  private final String level1Name = "LevelOne";
  private final WikiPagePath level1Path = PathParser.parse(this.level1Name);
  private final String level2Name = "LevelTwo";
  private final WikiPagePath level2Path = PathParser.parse(this.level2Name);
  private final WikiPagePath level2FullPath = this.level1Path.copy().addNameToEnd(this.level2Name);
  private final String qualifiedLevel2Name = PathParser.render(this.level2FullPath);

  @Test
  public void testDeleteConfirmation() throws Exception {
    WikiPage level1 = WikiPageUtil.addPage(this.root, this.level1Path, "");
    WikiPageUtil.addPage(level1, this.level2Path, "");
    MockRequest request = new MockRequest();
    request.setResource(this.qualifiedLevel2Name);
    request.addInput("deletePage", "");

    SimpleResponse response = (SimpleResponse) this.responder.makeResponse(context, request);
    String content = response.getContent();
    assertSubString("Are you sure you want to delete " + this.qualifiedLevel2Name, content);
  }

  @Test
  public void testDeletePage() throws Exception {
    WikiPage level1 = WikiPageUtil.addPage(this.root, this.level1Path, "");
    WikiPageUtil.addPage(level1, this.level2Path, "");
    assertTrue(this.root.getPageCrawler().pageExists(this.level1Path));
    MockRequest request = new MockRequest();
    request.setResource(this.level1Name);
    request.addInput("confirmed", "yes");

    SimpleResponse response = (SimpleResponse) this.responder.makeResponse(context, request);
    String page = response.getContent();
    assertNotSubString("Are you sure you want to delete", page);
    assertEquals(303, response.getStatus());
    assertEquals("/root", response.getHeader("Location"));
    assertFalse(this.root.getPageCrawler().pageExists(PathParser.parse(this.level1Name)));
    assertFalse(this.root.getPageCrawler().pageExists(this.level2FullPath));

    List<?> children = this.root.getChildren();
    assertEquals(0, children.size());
  }

  @Test
  public void testDontDeleteFrontPage() throws Exception {
    WikiPageUtil.addPage(this.root, PathParser.parse("FrontPage"), "Content");
    this.request.setResource("FrontPage");
    this.request.addInput("confirmed", "yes");
    Response response = this.responder.makeResponse(context, this.request);
    assertEquals(303, response.getStatus());
    assertEquals("/FrontPage", response.getHeader("Location"));
  }

  @Override
  protected Responder responderInstance() {
    return new DeletePageResponder();
  }
}
