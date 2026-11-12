// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.wiki.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.VersionInfo;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import fitnesse.wiki.fs.InMemoryPage;
import org.junit.Before;
import org.junit.Test;

public class InMemoryPageTest {
  private WikiPage page1;

  @Before
  public void setUp() throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    page1 = WikiPageUtil.addPage(root, PathParser.parse("PageOne"), "page one");
  }

  @Test
  public void commitReplacesThePageInPlace() throws Exception {
    PageData data = page1.getData();
    data.setContent("version 2");
    VersionInfo saved = page1.commit(data);

    assertEquals("version 2", page1.getData().getContent());
    assertEquals(0, page1.getVersions().size());
    assertEquals(null, saved.getAuthor());
  }

  @Test
  public void commitRecordsTheAuthorOnTheSave() throws Exception {
    PageData data = page1.getData();
    data.setAttribute(PageData.LAST_MODIFYING_USER, "Joe");

    VersionInfo saved = page1.commit(data);

    assertEquals("Joe", saved.getAuthor());
  }
}
