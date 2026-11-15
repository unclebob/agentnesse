package fitnesse.testsystems.slim;

import fitnesse.html.HtmlUtil;

/** Collapsed sections in a test result, opened when the section failed. */
public final class ResultSections {
  private ResultSections() {
  }

  public static String details(String title, String body) {
    String open = failed(body) ? " open" : "";
    return "<details class=\"agentnesse-include\"" + open + "><summary>"
      + HtmlUtil.escapeHTML(title) + "</summary>" + body + "</details>\n";
  }

  public static boolean failed(String html) {
    return html.contains("class=\"fail\"") || html.contains("class=\"error\"") || html.contains("exception");
  }

  public static String section(String origin, String testPage, String firstCell) {
    if (structural(origin)) {
      return origin;
    }
    if (firstCell != null && firstCell.trim().equalsIgnoreCase("import")) {
      return "import";
    }
    if (origin != null && !origin.isEmpty() && !origin.equals(testPage)) {
      return origin;
    }
    return "";
  }

  private static boolean structural(String name) {
    return "SetUp".equals(name) || "TearDown".equals(name)
      || "SuiteSetUp".equals(name) || "SuiteTearDown".equals(name)
      || "ScenarioLibrary".equals(name);
  }
}
