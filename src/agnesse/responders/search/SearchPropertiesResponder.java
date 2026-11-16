package agnesse.responders.search;

import agnesse.FitNesseContext;
import agnesse.components.TraversalListener;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.markdown.FrontMatter;
import agnesse.responders.JsonResponses;
import agnesse.wiki.WikiPage;
import agnesse.wiki.search.FrontMatterFinder;
import agnesse.wiki.search.PageFinder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

public class SearchPropertiesResponder extends ResultResponder {
  private static final Set<String> TYPES = Set.of("test", "suite", "static");

  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    Response rejected = JsonResponses.unknownParameter(request, "searchProperties", "executeSearchProperties",
      "type", "tags", "help", "prune", "searchScope");
    if (rejected != null) {
      return rejected;
    }
    String type = request.getInput("type");
    if (type != null && !type.isEmpty() && !TYPES.contains(type)) {
      return JsonResponses.error(400, "badRequest", "Unknown page type " + type);
    }
    return super.makeResponse(context, request);
  }

  @Override
  protected JSONObject jsonFor(List<WikiPage> hits) {
    JSONArray pages = new JSONArray();
    for (WikiPage hit : hits) {
      FrontMatter matter = FrontMatter.read(hit.getData().getContent());
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
