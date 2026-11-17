// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.search;

import agnesse.AgNesseContext;
import agnesse.components.TraversalListener;
import agnesse.components.Traverser;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.responders.ChunkingResponder;
import agnesse.responders.JsonResponses;
import agnesse.wiki.PageCrawler;
import agnesse.wiki.WikiPage;
import agnesse.wiki.search.PageFinder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class ResultResponder extends ChunkingResponder implements Traverser<WikiPage> {
  private final String typeLabel;
  private final Set<String> types;
  private final String[] parameters;

  protected ResultResponder(String typeLabel, Set<String> types, String... parameters) {
    this.typeLabel = typeLabel;
    this.types = types;
    this.parameters = parameters;
  }

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    Response rejected = JsonResponses.unknownParameter(request, parameters);
    if (rejected != null) {
      return rejected;
    }
    String type = request.getInput("type");
    if (type != null && !type.isEmpty() && !types.contains(type)) {
      return JsonResponses.error(400, "badRequest", "Unknown " + typeLabel + " type " + type);
    }
    return super.makeResponse(context, request);
  }

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

