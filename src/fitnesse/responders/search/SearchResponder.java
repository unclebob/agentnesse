// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.search;

import fitnesse.FitNesseContext;
import fitnesse.components.TraversalListener;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.responders.JsonResponses;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.search.MethodWikiPageFinder;
import fitnesse.wiki.search.PageFinder;
import fitnesse.wiki.search.RegularExpressionWikiPageFinder;
import fitnesse.wiki.search.TitleWikiPageFinder;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.LITERAL;

public class SearchResponder extends ResultResponder {
  private static final Set<String> TYPES = Set.of("title", "text", "method");

  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    Response rejected = JsonResponses.unknownParameter(request, "search", "searchForm", "type", "q", "searchScope");
    if (rejected != null) {
      return rejected;
    }
    String type = request.getInput("type");
    if (type != null && !type.isEmpty() && !TYPES.contains(type)) {
      return JsonResponses.error(400, "badRequest", "Unknown search type " + type);
    }
    return super.makeResponse(context, request);
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
