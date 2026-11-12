// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.fixtures;

import java.util.List;
import java.util.StringTokenizer;

import fitnesse.wiki.*;
import fitnesse.wiki.fs.FileBasedWikiPage;

public class PageCreator {
  public String pageName;
  public String pageContents;
  public String pageAttributes;

  public boolean valid() throws Exception {
    return markdown();
  }

  public boolean markdown() throws Exception {
    return store();
  }

  private boolean store() throws Exception {
    if (pageContents != null) {
      pageContents = pageContents.replaceAll("<br>", "\n");
      pageContents = pageContents.replaceAll("<br/>", "\n");
    }
    WikiPage root = FitnesseFixtureContext.context.getRootPage();
    WikiPagePath pagePath = PathParser.parse(pageName);
    WikiPage thePage = addMarkdown(root, pagePath);
    PageData data = thePage.getData();
    data.setContent(pageContents == null ? "" : pageContents);
    if (pageAttributes != null && !pageAttributes.isEmpty()) {
      setAttributes(data);
    }
    thePage.commit(data);
    setPageAttributes("");
    return true;
  }

  private WikiPage addMarkdown(WikiPage root, WikiPagePath path) {
    List<String> names = path.getNames();
    WikiPage parent = root;
    for (int i = 0; i < names.size() - 1; i++) {
      parent = markdownChild(parent, names.get(i));
    }
    return markdownChild(parent, names.get(names.size() - 1));
  }

  private WikiPage markdownChild(WikiPage parent, String name) {
    WikiPage existing = parent.getChildPage(name);
    if (existing != null) {
      return existing;
    }
    return ((FileBasedWikiPage) parent).addMarkdownChild(name);
  }

  private void setAttributes(PageData data) throws Exception {
    if (pageAttributes != null) {
      StringTokenizer tokenizer = new StringTokenizer(pageAttributes, ",");
      while (tokenizer.hasMoreTokens()) {
        String nameValuePair = tokenizer.nextToken();
        int equals = nameValuePair.indexOf("=");
        if (equals < 0)
          throw new Exception("Attribute must have form name=value");
        String name = nameValuePair.substring(0, equals);
        String value = nameValuePair.substring(equals + 1);
        data.setAttribute(name, value);
      }
    }
  }

  public void setPageName(String pageName) {
    this.pageName = pageName;
  }

  public void setPageContents(String pageContents) {
    this.pageContents = pageContents;
  }

  public void setPageAttributes(String pageAttributes) {
    this.pageAttributes = pageAttributes;
  }
}
