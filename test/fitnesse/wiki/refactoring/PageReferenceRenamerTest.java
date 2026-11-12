// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.wiki.refactoring;

import fitnesse.wiki.*;
import fitnesse.wiki.fs.InMemoryPage;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PageReferenceRenamerTest {
  WikiPage root;
  WikiPage subWiki;
  WikiPage subWiki_pageOne;
  WikiPage subWiki_pageTwo;
  WikiPage subWiki_pageTwo_pageTwoChild;

  PageReferenceRenamer renamer;

  @Before
  public void setUp() throws Exception {
    root = InMemoryPage.makeRoot("RooT");
    subWiki = WikiPageUtil.addPage(root, PathParser.parse("SubWiki"), "");
    subWiki_pageTwo = WikiPageUtil.addPage(subWiki, PathParser.parse("PageTwo"), "");
    subWiki_pageTwo_pageTwoChild = WikiPageUtil.addPage(subWiki_pageTwo, PathParser.parse("PageTwoChild"), "");
  }

  @Test
  public void testReferencesOnPageOne_1() {
    checkChangesOnPageOne("Stuff PageTwo Stuff\n", "Stuff PageThree Stuff\n");
  }

  @Test
  public void testReferencesOnPageOne_2() {
    checkChangesOnPageOne("Stuff `PageTwo` Stuff\n", "Stuff `PageTwo` Stuff\n");
  }

  @Test
  public void testReferencesOnPageOne_3() {
    checkChangesOnPageOne("Stuff PageOne.PageTwo Stuff\n", "Stuff PageOne.PageTwo Stuff\n");
  }

  @Test
  public void testReferencesOnPageOne_4() {
    checkChangesOnPageOne("Stuff .SubWiki.PageTwo.PageTwoChild Stuff\n", "Stuff .SubWiki.PageThree.PageTwoChild Stuff\n");
  }

  @Test
  public void testReferencesOnPageOne_5() {
    checkChangesOnPageOne("Stuff ^PageTwo Stuff\n", "Stuff ^PageTwo Stuff\n");
  }

  @Test
  public void testReferencesOnPageOne_6() {
    checkChangesOnPageOne("<!-- Stuff PageTwo Stuff -->\n", "<!-- Stuff PageTwo Stuff -->\n");
  }

  @Test
  public void testReferencesOnPageOne_7() {
    checkChangesOnPageOne("```\nStuff PageTwo Stuff\n```\n", "```\nStuff PageTwo Stuff\n```\n");
  }

  @Test
  public void testReferencesOnPageOne_8() {
    checkChangesOnPageOne("Stuff .SubWiki.PageTwo Stuff\n", "Stuff .SubWiki.PageThree Stuff\n");
  }

  @Test
  public void testReferencesOnPageOne_9() {
    checkChangesOnPageOne("Stuff .SubWiki.PageTwo.NoPage Stuff\n", "Stuff .SubWiki.PageThree.NoPage Stuff\n");
  }

  @Test
  public void testTestReferencesToSubWiki_1() {
    PageData data = subWiki.getData();
    data.setContent("Stuff >PageTwo Stuff\n");
    subWiki.commit(data);

    renamer = new PageReferenceRenamer(subWiki_pageTwo, "PageThree");
    ReferenceRenamingTraverser.renameReferences(root, renamer);

    String updatedSubWikiContent = subWiki.getData().getContent();
    assertEquals("Stuff >PageThree Stuff\n", updatedSubWikiContent);
  }

  @Test
  public void testTestReferencesToSubWiki_2() {
    PageData data = subWiki.getData();
    data.setContent("Stuff >PageTwo.DeepPage Stuff\n");
    subWiki.commit(data);

    renamer = new PageReferenceRenamer(subWiki_pageTwo, "PageThree");
    ReferenceRenamingTraverser.renameReferences(root, renamer);
    String updatedSubWikiContent = subWiki.getData().getContent();
    assertEquals("Stuff >PageThree.DeepPage Stuff\n", updatedSubWikiContent);
  }

  private void checkChangesOnPageOne(String beforeText, String expectedAfterText) {
    subWiki_pageOne = WikiPageUtil.addPage(subWiki, PathParser.parse("PageOne"), beforeText);
    renamer = new PageReferenceRenamer(subWiki_pageTwo, "PageThree");
    ReferenceRenamingTraverser.renameReferences(root, renamer);
    subWiki_pageOne = subWiki.getChildPage("PageOne");
    String updatedPageOneContent = subWiki_pageOne.getData().getContent();
    assertEquals(expectedAfterText, updatedPageOneContent);
  }

  @Test
  public void testRenameParentPage() {
    PageData pageTwoChildData = subWiki_pageTwo_pageTwoChild.getData();
    pageTwoChildData.setContent("gunk .SubWiki.PageTwo gunk");
    subWiki_pageTwo_pageTwoChild.commit(pageTwoChildData);
    renamer = new PageReferenceRenamer(subWiki_pageTwo, "PageThree");
    ReferenceRenamingTraverser.renameReferences(root, renamer);
    String updatedContent = subWiki_pageTwo_pageTwoChild.getData().getContent();
    assertEquals("gunk .SubWiki.PageThree gunk", updatedContent);
  }

  @Test
  public void testSubPageReferenceUnchangedWhenParentRenamed() {
    WikiPage pageOne = WikiPageUtil.addPage(subWiki, PathParser.parse("PageOne"), "gunk ^SubPage gunk");
    renamer = new PageReferenceRenamer(subWiki, "RenamedSubWiki");
    ReferenceRenamingTraverser.renameReferences(root, renamer);
    String updatedContent = pageOne.getData().getContent();
    assertEquals("gunk ^SubPage gunk", updatedContent);
  }

  @Test
  public void testRenameParentWithSubPageReferenceOnSibling() {
    WikiPage pageOne = WikiPageUtil.addPage(subWiki, PathParser.parse("PageOne"), "gunk PageTwo gunk");
    renamer = new PageReferenceRenamer(subWiki, "RenamedSubWiki");
    ReferenceRenamingTraverser.renameReferences(root, renamer);
    String updatedContent = pageOne.getData().getContent();
    assertEquals("gunk PageTwo gunk", updatedContent);
  }

  @Test
  public void testRenameSiblingOfRoot() {
    WikiPage source = WikiPageUtil.addPage(root, PathParser.parse("SourcePage"), "gunk TargetPage gunk");
    WikiPage target = WikiPageUtil.addPage(root, PathParser.parse("TargetPage"));
    renamer = new PageReferenceRenamer(target, "RenamedPage");
    ReferenceRenamingTraverser.renameReferences(root, renamer);
    String updatedSourceContent = source.getData().getContent();
    assertEquals("gunk RenamedPage gunk", updatedSourceContent);
  }

  @Test
  public void testRenameSubpageOfRoot() {
    WikiPage source = WikiPageUtil.addPage(root, PathParser.parse("SourcePage"), "gunk ^TargetPage gunk");
    WikiPage target = WikiPageUtil.addPage(source, PathParser.parse("TargetPage"));
    renamer = new PageReferenceRenamer(target, "RenamedPage");
    ReferenceRenamingTraverser.renameReferences(root, renamer);

    source = root.getChildPage("SourcePage");
    String updatedSourceContent = source.getData().getContent();
    assertEquals("gunk >RenamedPage gunk", updatedSourceContent);
  }

  @Test
  public void testImageNotChanged() {
    final String IMAGE_WIDGET = "!img http://PageTwo.jpg";
    checkChangesOnPageOne(IMAGE_WIDGET, IMAGE_WIDGET);
  }

  @Test
  public void testLinkNotChanged() {
    final String LINK_WIDGET = "http://PageTwo";
    checkChangesOnPageOne(LINK_WIDGET, LINK_WIDGET);
  }

  @Test
  public void testPathNotChanged() {
    final String PATH_WIDGET = "```agentnesse\npath PageTwo\n```";
    checkChangesOnPageOne(PATH_WIDGET, PATH_WIDGET);
  }

  @Test
  public void testAliasTagChanged() {
    checkChangesOnPageOne("[[PageTwo][MyPageTwo]]", "[[PageThree][MyPageTwo]]");
  }

  @Test
  public void testAliasLinkRenamed() {
    checkChangesOnPageOne("gunk [[gunk][PageTwo]] gunk", "gunk [[gunk][PageThree]] gunk");
  }

  @Test
  public void testAliasLinWithLiteralDoesntGetCorrupted() {
    checkChangesOnPageOne("gunk [[!-gunk-!][PageTwo]] gunk", "gunk [[!-gunk-!][PageThree]] gunk");
  }

  @Test
  public void testXrefWidgetRenamed() {
    checkChangesOnPageOne("!see PageTwo", "!see PageThree");
  }

}

