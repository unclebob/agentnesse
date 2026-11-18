// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import agnesse.wiki.fs.InMemoryPage;
import org.junit.Before;
import org.junit.Test;

public class PageDataTest {
  public WikiPage page;
  private WikiPage root;

  @Before
  public void setUp() throws Exception {
    root = InMemoryPage.makeRoot("RooT");
    page = WikiPageUtil.addPage(root, PathParser.parse("PagE"), "some content");
  }

  @Test
  public void testAttributesAreTruelyCopiedInCopyConstructor() {
    PageData data = root.getData();
    data.setAttribute("myFriend", "Joe");
    PageData newData = new PageData(data);
    newData.setAttribute("myFriend", "Jane");

    assertEquals("Joe", data.getAttribute("myFriend"));
  }

  @Test
  public void emptyMeansNoContent() {
    assertTrue(new PageData(null, new WikiPageProperty()).isEmpty());
    assertTrue(new PageData("", new WikiPageProperty()).isEmpty());
    assertFalse(new PageData("body", new WikiPageProperty()).isEmpty());
  }

  @Test
  public void contentDropsCarriageReturnsAndBlankAttributesAreRemoved() {
    PageData data = new PageData("a\rb", new WikiPageProperty());
    assertEquals("ab", data.getContent());
    data.setOrRemoveAttribute("Help", null);
    data.setOrRemoveAttribute("Help", "");
    assertEquals(null, data.getAttribute("Help"));
    data.setOrRemoveAttribute("Help", "how");
    assertEquals("how", data.getAttribute("Help"));
    data.setFrontMatterPrefix(null);
    assertEquals("", data.getFrontMatterPrefix());
    data.setFrontMatterPrefix("{}\n");
    assertEquals("{}\n", data.getFrontMatterPrefix());
    assertFalse(data.preserveFrontMatter());
    data.setPreserveFrontMatter(true);
    assertTrue(data.preserveFrontMatter());
  }
}
