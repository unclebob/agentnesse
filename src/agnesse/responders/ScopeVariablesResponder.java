package agnesse.responders;

import agnesse.AgNesseContext;
import org.json.JSONArray;
import org.json.JSONObject;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.markdown.MarkdownPage;
import agnesse.wiki.*;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class ScopeVariablesResponder extends BasicResponder {
  private List<ScopeVariable> variables;

  @Override
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    Response rejected = JsonResponses.unknownParameter(request, "variables");
    if (rejected != null) {
      return rejected;
    }

    WikiPage requestedPage = getRequestedPage(request, context);
    String pageName = request.getResource();
    WikiPagePath path = PathParser.parse(pageName);
    PageCrawler crawler = context.getRootPage().getPageCrawler();
    WikiPage page = crawler.getPage(path);

    Response response;
    if (requestedPage == null)
      response = pageNotFoundResponse(context, request);
    else {
      variables = new ArrayList<>();
      listVariablesLoc(page);
      response = jsonResponse();
    }
    return response;
  }

  private void listVariablesLoc(WikiPage page) {
    Map<String, String> variableList = page instanceof MarkdownPage
      ? ((MarkdownPage) page).definedVariables()
      : java.util.Map.of();

    for (Map.Entry<String, String> var : variableList.entrySet()) {
      if (variables.stream().noneMatch(variable -> var.getKey().equals(variable.getKey()))) {
        variables.add(new ScopeVariable(var.getKey(), page.getFullPath().toString(), var.getValue()));
      }
    }

    if (page.getParent() != page) listVariablesLoc(page.getParent());
  }

  private Response jsonResponse() throws UnsupportedEncodingException {
    JSONArray values = new JSONArray();
    for (ScopeVariable variable : variables) {
      values.put(new JSONObject()
        .put("name", variable.getKey())
        .put("value", variable.getValue())
        .put("page", variable.getLocation()));
    }
    SimpleResponse response = new SimpleResponse();
    response.setContentType(Response.Format.JSON);
    response.setContent(new JSONObject().put("variables", values).toString(2));
    return response;
  }

  public class ScopeVariable {
    private String key;
    private String location;
    private String value;

    public ScopeVariable(String key, String location, String value) {
      this.key = key;
      this.location = location;
      this.value = value;
    }
    
    public String getKey() {
      return this.key;
    }

    public String getLocation() {
      return this.location;
    }

    public String getValue() {
      return this.value;
    }
  }
}
