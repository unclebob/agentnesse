package fitnesse.fixtures;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PagesRunInSuite {
  private String prefix;
  private String testSystemID;

  public PagesRunInSuite(String prefix, String testSystemID) {
    this.prefix = prefix;
    this.testSystemID = testSystemID;
  }

  public List<Object> query() throws Exception {
    return buildQueryResponse(getPagesRunInSuite());

  }

  private List<String> getPagesRunInSuite() throws Exception {
    List<String> testPages = new ArrayList<>();

    String content = FitnesseFixtureContext.sender.sentData();
    if (content.contains("Testing was interrupted and results are incomplete.")) {
      throw new Exception("Test result page says: Testing was interrupted and results are incomplete.");
    }
    if (content.contains("\"event\":\"page-started\"")) {
      return pagesFromEvents(content);
    }
    String[] testSystems = content.split("slim:");
    for (String testSystem : testSystems) {
      addPagesForThisTestSystem(testPages, testSystem);
    }
    return testPages;
  }

  private List<String> pagesFromEvents(String content) {
    List<String> testPages = new ArrayList<>();
    for (String line : content.split("\n")) {
      JSONObject event = eventOn(line);
      if (event == null || !"page-started".equals(event.optString("event"))) {
        continue;
      }
      if (!systemMatches(event.optString("system"))) {
        continue;
      }
      String path = event.optString("path");
      int prefixAt = path.indexOf(prefix);
      if (prefixAt >= 0) {
        testPages.add(path.substring(prefixAt));
      }
    }
    return testPages;
  }

  private static JSONObject eventOn(String line) {
    int start = line.indexOf('{');
    int end = line.lastIndexOf('}');
    if (start < 0) {
      return null;
    }
    try {
      return new JSONObject(line.substring(start, end + 1));
    } catch (Exception ignored) {
      return null;
    }
  }

  private boolean systemMatches(String system) {
    return system.startsWith(testSystemID) || system.contains(":" + testSystemID);
  }

  private void addPagesForThisTestSystem(List<String> testPages, String testSystem) {
    if (testSystem.startsWith(testSystemID)) {
      String[] pageString = testSystem.split("test_name\">");
      addPages(testPages, pageString);
    }
  }

  private void addPages(List<String> testPages, String[] pageString) {
    for (int i = 1; i < pageString.length; i++) {
      int fragmentEnd = pageString[i].indexOf("<");
      String testPage = pageString[i].substring(0, fragmentEnd);
      if (testPage.startsWith(prefix)) {
        testPages.add(testPage);
      }
    }
  }

  private List<Object> buildQueryResponse(List<String> testPages) {
    List<Object> rows = new ArrayList<>();
    for (String testPage : testPages) {
      List<Object> columns = new ArrayList<>();
      List<String> pageNameCell = Arrays.asList("page name", testPage);
      columns.add(pageNameCell);
      rows.add(columns);
    }
    return rows;
  }
}
