// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders.search;

import agnesse.components.TraversalListener;
import agnesse.wiki.WikiPage;
import agnesse.wiki.search.MethodWikiPageFinder;
import agnesse.wiki.search.PageFinder;
import agnesse.wiki.search.RegularExpressionWikiPageFinder;
import agnesse.wiki.search.TitleWikiPageFinder;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.LITERAL;

public class SearchResponder extends ResultResponder {
  public SearchResponder() {
    super("search", Set.of("title", "text", "method"), "search", "searchForm", "type", "q", "searchScope");
  }

  private String query() {
    String q = request.getInput("q");
    return q == null ? "" : q;
  }

  private String type() {
    String type = request.getInput("type");
    return type == null || type.isEmpty() ? "title" : type;
  }

  @Override
  protected PageFinder getPageFinder(TraversalListener<WikiPage> observer) {
    String q = query();
    if (q.isEmpty()) {
      return null;
    }
    if ("title".equals(type())) {
      return new TitleWikiPageFinder(q, observer);
    }
    if ("method".equals(type())) {
      return new MethodWikiPageFinder(methodLine(q), observer);
    }
    Pattern regularExpression = Pattern.compile(q, CASE_INSENSITIVE + LITERAL);
    return new RegularExpressionWikiPageFinder(regularExpression, observer);
  }

  private static String methodLine(String q) {
    if (q.startsWith("|") && q.endsWith("|")) {
      return q;
    }
    return "|" + q + "|";
  }

  @Override
  protected JSONObject jsonFor(List<WikiPage> hits) {
    return super.jsonFor(hits).put("type", type()).put("q", query());
  }

  @Override
  protected boolean shouldRespondWith404() {
    return false;
  }
}
