package agnesse.responders.search;

import agnesse.components.TraversalListener;
import agnesse.markdown.FrontMatter;
import agnesse.wiki.WikiPage;
import agnesse.wiki.search.FrontMatterFinder;
import agnesse.wiki.search.PageFinder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

public class SearchPropertiesResponder extends ResultResponder {
  public SearchPropertiesResponder() {
    super("page", Set.of("test", "suite", "static"), "searchProperties", "executeSearchProperties",
      "type", "tags", "help", "prune", "searchScope");
  }

  @Override
  protected JSONObject jsonFor(List<WikiPage> hits) {
    JSONArray pages = new JSONArray();
    for (WikiPage hit : hits) {
      FrontMatter matter = FrontMatter.from(hit.getData());
      pages.put(new JSONObject()
        .put("path", hit.getFullPath().toString())
        .put("type", matter.type() == null ? "static" : matter.type())
        .put("tags", new JSONArray(matter.tags()))
        .put("help", matter.help() == null ? JSONObject.NULL : matter.help())
        .put("prune", matter.prune()));
    }
    return new JSONObject().put("pages", pages);
  }

  @Override
  protected PageFinder getPageFinder(TraversalListener<WikiPage> observer) {
    return new FrontMatterFinder(observer, request.getInput("type"), request.getInput("tags"),
      request.getInput("help"), request.getInput("prune"));
  }
}
