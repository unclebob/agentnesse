package fitnesse.responders.run;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.testrunner.MultipleTestsRunner;
import fitnesse.testrunner.SuiteContentsFinder;
import fitnesse.testrunner.SuiteFilter;
import fitnesse.testrunner.run.PagesByTestSystemTestRunFactory;
import fitnesse.testrunner.run.TestRun;
import fitnesse.testsystems.Descriptor;
import fitnesse.testsystems.TestExecutionException;
import fitnesse.testsystems.TestSystem;
import fitnesse.testsystems.TestSystemFactory;
import fitnesse.testsystems.slim.InstructionTestSystem;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InstructionResponder implements Responder {

  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    WikiPage root = context.getRootPage(request.getMap());
    WikiPage suite = root.getPageCrawler().getPage(PathParser.parse(request.getResource()));

    SuiteContentsFinder suiteTestFinder = new SuiteContentsFinder(suite, SuiteFilter.MATCH_ALL, root);
    List<WikiPage> pages = suiteTestFinder.getAllPagesToRunForThisSuite();

    TestRun run = new PagesByTestSystemTestRunFactory().createRun(pages);
    StringBuilder result = new StringBuilder();
    MultipleTestsRunner runner = new MultipleTestsRunner(run, new InstructionTestSystemFactory(result));
    try {
      runner.executeTestPages();
    } catch (TestExecutionException e) {
      e.printStackTrace();
    }

    SimpleResponse response = new SimpleResponse();
    response.setContentType(Response.Format.JSON);
    response.setContent(json(result.toString()).toString(2));
    return response;
  }

  private static JSONObject json(String lines) {
    Map<String, JSONArray> byPath = new LinkedHashMap<>();
    for (String line : lines.split("\\R")) {
      if (line.isEmpty()) {
        continue;
      }
      int bar = line.indexOf('|');
      String path = bar < 0 ? line : line.substring(0, bar);
      String instruction = bar < 0 ? "" : line.substring(bar + 1);
      byPath.computeIfAbsent(path, key -> new JSONArray()).put(instruction);
    }
    JSONArray pages = new JSONArray();
    for (Map.Entry<String, JSONArray> entry : byPath.entrySet()) {
      pages.put(new JSONObject().put("path", entry.getKey()).put("instructions", entry.getValue()));
    }
    return new JSONObject().put("pages", pages);
  }

  static class InstructionTestSystemFactory implements TestSystemFactory {

    public InstructionTestSystemFactory(StringBuilder result) {
      this.result = result;
    }

    @Override
    public TestSystem create(Descriptor descriptor) {
      return new InstructionTestSystem(result);
    }

    private final StringBuilder result;
  }
}
