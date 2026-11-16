// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.search;

import agnesse.components.TraversalListener;
import agnesse.components.Traverser;
import agnesse.responders.ChunkingResponder;
import agnesse.wiki.PageCrawler;
import agnesse.wiki.WikiPage;
import agnesse.wiki.search.PageFinder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ResultResponder extends ChunkingResponder implements Traverser<WikiPage> {

  @Override
  protected PageCrawler getPageCrawler() {
    return root.getPageCrawler();
  }

  protected WikiPage getSearchScope() {
    String searchScope = request.getInput("searchScope");

    if (searchScope == null || searchScope.isEmpty())
      return page;
    else if(searchScope.equalsIgnoreCase("root"))
      return root;
    else{
      WikiPage scope = page.getPageCrawler().findAncestorWithName(searchScope);
      return scope;
    }
  }

  @Override
  protected void doSending() throws IOException {
    if (page == null)
      page = root;
    List<WikiPage> hits = new ArrayList<>();
    traverse(hits::add);
    response.setContentType("application/json");
    response.add(jsonFor(hits).toString(2));
    response.close();
  }

  @Override
  public final void traverse(TraversalListener<WikiPage> observer) {
    PageFinder pageFinder = getPageFinder(observer);
    if (pageFinder != null) {
      pageFinder.search(getSearchScope());
    }
  }

  protected abstract PageFinder getPageFinder(TraversalListener<WikiPage> observer);

  protected JSONObject jsonFor(List<WikiPage> hits) {
    JSONArray pages = new JSONArray();
    for (WikiPage hit : hits) {
      pages.put(hit.getFullPath().toString());
    }
    return new JSONObject().put("pages", pages);
  }

}

