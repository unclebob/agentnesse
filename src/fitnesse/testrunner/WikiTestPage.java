package fitnesse.testrunner;

import fitnesse.markdown.MarkdownPage;
import fitnesse.testsystems.ClassPath;
import fitnesse.testsystems.TestPage;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPagePath;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

// TODO: need 2 implementations, one for wiki text pages (Fit, Slim) and one for non-wiki text pages. See PagesByTestSystem
public class WikiTestPage implements TestPage {
  public static final String TEAR_DOWN = "TearDown";
  public static final String SET_UP = "SetUp";
  public static final String SCENARIO_LIBRARY = "ScenarioLibrary";

  private final WikiPage sourcePage;
  private List<WikiPage> scenarioLibraries;
  private WikiPage setUp;
  private WikiPage tearDown;

  public WikiTestPage(WikiPage sourcePage) {
    this.sourcePage = sourcePage;
  }

  public PageData getData() {
    return sourcePage.getData();
  }

  @Override
  public String getHtml() {

    // -AJM- Okay, this is not as clean as I'd like it to be, but for now it does the trick
    if (sourcePage instanceof MarkdownPage) {
      return markdownHtml();
    }
    return sourcePage.getHtml();
  }

  private String markdownHtml() {
    StringBuilder html = new StringBuilder();
    addLeadingHtml(html);
    html.append(sourcePage.getHtml());
    addTrailingHtml(html);
    return html.toString();
  }

  protected void addLeadingHtml(StringBuilder html) {
    appendPages(html, getScenarioLibraries());
    appendPage(html, getSetUp());
  }

  protected void addTrailingHtml(StringBuilder html) {
    appendTearDown(html, getTearDown());
  }

  protected void appendPages(StringBuilder html, List<WikiPage> pages) {
    appendPages(html, pages, false);
  }

  protected void appendTearDowns(StringBuilder html, List<WikiPage> pages) {
    appendPages(html, pages, true);
  }

  private void appendPages(StringBuilder html, List<WikiPage> pages, boolean tearDown) {
    if (pages == null) {
      return;
    }
    for (WikiPage page : pages) {
      appendIncluded(html, page, tearDown);
    }
  }

  private String includedHtml(WikiPage page) {
    if (page instanceof MarkdownPage && page != sourcePage) {
      return ((MarkdownPage) page).getHtmlWithin(sourcePage);
    }
    return page.getHtml();
  }

  private void appendPage(StringBuilder html, WikiPage page) {
    appendIncluded(html, page, false);
  }

  private void appendTearDown(StringBuilder html, WikiPage page) {
    appendIncluded(html, page, true);
  }

  private void appendIncluded(StringBuilder html, WikiPage page, boolean tearDown) {
    if (page == null) {
      return;
    }
    if (tearDown) {
      html.append("<div class=\"teardown\">");
    }
    html.append("<details class=\"agentnesse-include\"><summary>")
      .append(page.getName())
      .append("</summary>")
      .append(includedHtml(page))
      .append("</details>");
    if (tearDown) {
      html.append("</div>");
    }
    html.append('\n');
  }

  @Override
  public String getVariable(String variable) {
    return sourcePage.getVariable(variable);
  }

  @Override
  public String getFullPath() {
    return PathParser.render(sourcePage.getFullPath());
  }

  @Override
  public ClassPath getClassPath() {
    return new ClassPath(new ClassPathBuilder().getClassPath(sourcePage), getPathSeparator());
  }

  @Override
  public String getContent() {
    return sourcePage.getData().getContent();
  }

  protected String getPathSeparator() {
    String separator = sourcePage.getVariable(PageData.PATH_SEPARATOR);
    if (separator == null)
      separator = File.pathSeparator;
    return separator;
  }

  public WikiPage getSourcePage() {
    return sourcePage;
  }

  private String getPathNameForPage(WikiPage page) {
    WikiPagePath pagePath = page.getFullPath();
    return PathParser.render(pagePath);
  }

  public String getPath() {
    return getPathNameForPage(sourcePage);
  }

  @Override
  public String getName() {
    return sourcePage.getName();
  }

  public boolean shouldIncludeScenarioLibraries() {
    // Should consider all of the decorated content to resolve those variables.
    String testSystem = sourcePage.getVariable(WikiPageIdentity.TEST_SYSTEM);
    boolean isSlim = "slim".equalsIgnoreCase(testSystem)
                      || "slimCoverage".equalsIgnoreCase(testSystem);
    String includeScenarioLibraries = sourcePage.getVariable("INCLUDE_SCENARIO_LIBRARIES");
    boolean includeScenarios = "true".equalsIgnoreCase(includeScenarioLibraries);
    boolean notIncludeScenarios = "false".equalsIgnoreCase(includeScenarioLibraries);

    return includeScenarios || (!notIncludeScenarios && isSlim);
  }

  public List<WikiPage> getScenarioLibraries() {
    if (scenarioLibraries == null) {
      scenarioLibraries = findScenarioLibraries();
    }
    return scenarioLibraries;
  }

  public WikiPage getSetUp() {
    setUp = inheritedPage(setUp, SET_UP);
    return setUp;
  }

  public WikiPage getTearDown() {
    tearDown = inheritedPage(tearDown, TEAR_DOWN);
    return tearDown;
  }

  private WikiPage inheritedPage(WikiPage cached, String pageName) {
    if (cached == null && !isSuiteSetUpOrTearDownPage()) {
      return findInheritedPage(pageName);
    }
    return cached;
  }

  protected boolean isSuiteSetUpOrTearDownPage() {
    return sourcePage.isSuiteSetupOrTearDown();
  }

  protected WikiPage findInheritedPage(String pageName) {
    return sourcePage.getPageCrawler().getClosestInheritedPage(pageName);
  }

  private List<WikiPage> findScenarioLibraries() {
    List<WikiPage> uncles;
    if (shouldIncludeScenarioLibraries()) {
      uncles = findUncles(SCENARIO_LIBRARY);
    } else {
      uncles = Collections.emptyList();
    }
    return uncles;
  }

  protected List<WikiPage> findUncles(String uncleName) {
    LinkedList<WikiPage> uncles = new LinkedList<>();
    sourcePage.getPageCrawler().traverseUncles(uncleName, uncles::addFirst);
    return uncles;
  }
}
