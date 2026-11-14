// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders;

import fitnesse.FitNesseContext;
import fitnesse.http.Request;
import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPage;
import fitnesse.http.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class NameWikiPageResponder extends BasicResponder {
  @Override
  protected String contentFrom(FitNesseContext context, Request request, WikiPage requestedPage) {
    JSONArray pages = new JSONArray();
    for (NamedPage named : addLines(request, requestedPage, "")) {
      JSONObject item = new JSONObject().put("name", named.name);
      if (request.hasInput("ShowTags")) {
        item.put("tags", new JSONArray(named.tags));
      }
      pages.put(item);
    }
    return pages.toString(2);
  }

  private List<NamedPage> addLines(Request request, WikiPage requestedPage, String prefix) {
    List<NamedPage> lines = new ArrayList<>();

    for (WikiPage child : requestedPage.getChildren()) {
      if (!request.hasInput("LeafOnly") || child.getChildren().isEmpty()) {
        lines.add(new NamedPage(prefix + child.getName(), request.hasInput("ShowTags") ? getTags(child) : Set.of()));
      }
      if (request.hasInput("Recursive")) {
        lines.addAll(addLines(request, child, prefix + child.getName() + "."));
      }
    }
    return lines;
  }

  private static final class NamedPage {
    private final String name;
    private final Set<String> tags;

    private NamedPage(String name, Set<String> tags) {
      this.name = name;
      this.tags = tags;
    }
  }

  private Set<String> getTags(WikiPage page) {
    Set<String> result = new TreeSet<>();

	// get the tags of the given page
    String tags = page.getData().getAttribute(PageData.PropertySUITES);
    if((tags != null) && !tags.isEmpty()) {
	  result.addAll(Arrays.asList(tags.split(", ")));
    }

	// recusrively collect all tags up to the root
	if(!page.isRoot()) {
	  result.addAll(getTags(page.getParent()));
	}

	return result;
  }

  @Override
  protected String getContentType() {
    return Response.Format.JSON.getContentType();
  }

}
