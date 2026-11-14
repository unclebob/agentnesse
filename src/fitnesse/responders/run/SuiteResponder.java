// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders.run;

import fitnesse.FitNesseContext;
import fitnesse.html.template.HtmlPage;
import fitnesse.html.template.PageTitle;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.reporting.BaseFormatter;
import fitnesse.reporting.Formatter;
import fitnesse.reporting.InteractiveFormatter;
import fitnesse.reporting.SuiteHtmlFormatter;
import fitnesse.results.FailedPages;
import fitnesse.results.JsonResultsFormatter;
import fitnesse.responders.ChunkingResponder;
import fitnesse.responders.WikiPageActions;
import fitnesse.testrunner.MultipleTestsRunner;
import fitnesse.testrunner.RunningTestingTracker;
import fitnesse.testrunner.SuiteContentsFinder;
import fitnesse.testrunner.SuiteFilter;
import fitnesse.testrunner.run.TestRun;
import fitnesse.testsystems.ConsoleExecutionLogListener;
import fitnesse.testsystems.ExecutionLogListener;
import fitnesse.testsystems.TestExecutionException;
import fitnesse.wiki.PageCrawler;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PageType;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPagePath;
import fitnesse.wiki.WikiPageUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SuiteResponder extends ChunkingResponder {
  private static final Logger LOG = Logger.getLogger(SuiteResponder.class.getName());

  private static final String NOT_FILTER_ARG = "excludeSuiteFilter";
  private static final String AND_FILTER_ARG = "runTestsMatchingAllTags";
  private static final String OR_FILTER_ARG_1 = "runTestsMatchingAnyTag";
  private static final String OR_FILTER_ARG_2 = "suiteFilter";

  static final RunningTestingTracker runningTestingTracker = new RunningTestingTracker();

  static final String UNSUPPORTED_RESULT_FORMAT_MESSAGE =
    "Result format %s is not supported. Use format=json.\n";

  private Formatter rerunFormatter;

  private PageData data;
  private String testRunId;
  private BaseFormatter mainFormatter;
  private volatile boolean isClosed = false;

  private boolean debug = false;
  private boolean remoteDebug = false;
  private int exitCode;

  private Request request;

  public SuiteResponder() {
    super();
  }

  private boolean isInteractive() {
    return mainFormatter instanceof InteractiveFormatter;
  }

  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    SimpleResponse rejected = unsupportedFormatResponse(request.getInput("format"));
    if (rejected != null) {
      return rejected;
    }
    this.request = request;
    Response result = super.makeResponse(context, request);
    if (result != response){
        return result;
    }
    testRunId = runningTestingTracker.generateNextTicket();
    response.addHeader("X-Agentnesse-Test-Id", testRunId);
    return response;
  }

  @Override
  protected void doSending() throws Exception {
    debug |= request.hasInput("debug");
    remoteDebug |= request.hasInput("remote_debug");
    data = page.getData();

    createMainFormatter();
    rerunFormatter = createRerunFormatter();

    if (isInteractive()) {
      makeHtml().render(response.getWriter(), request);
    } else {
      doExecuteTests();
    }

    closeHtmlResponse(exitCode);
  }

  public void doExecuteTests() {
    try {
      performExecution();
    } catch (Exception e) {
      LOG.log(Level.INFO, "Test system terminated with exception", e);
    }

    exitCode = mainFormatter.getErrorCount();
  }

  private HtmlPage makeHtml() {
    PageCrawler pageCrawler = page.getPageCrawler();
    WikiPagePath fullPath = pageCrawler.getFullPath();
    String fullPathName = PathParser.render(fullPath);
    HtmlPage htmlPage = context.pageFactory.newPage();
    htmlPage.setTitle(getTitle() + ": " + fullPathName);
    htmlPage.setPageTitle(new PageTitle(getTitle(), fullPath, data.getAttribute(PageData.PropertySUITES)));
    htmlPage.setNavTemplate("testNav.vm");
    htmlPage.put("actions", new WikiPageActions(page));
    htmlPage.setMainTemplate(mainTemplate());
    htmlPage.put("testExecutor", new TestExecutor());
    htmlPage.setFooterTemplate("wikiFooter.vm");
    htmlPage.put("headerContent", new WikiPageHeaderRenderer());
    htmlPage.put("footerContent", new WikiPageFooterRenderer());
    htmlPage.setErrorNavTemplate("errorNavigator");
    htmlPage.put("multipleTestsRun", isMultipleTestsRun());
    if (rerunFormatter != null) {
      htmlPage.put("rerunPage", getRerunPageName());
    }

    return htmlPage;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public class WikiPageHeaderRenderer {

    public String render() {
      return WikiPageUtil.getHeaderPageHtml(page);
    }

  }

  public class WikiPageFooterRenderer {

    public String render() {
        return WikiPageUtil.getFooterPageHtml(page);
    }

  }

  public class TestExecutor {
    public void execute() {
        doExecuteTests();
    }
  }

  private boolean isMultipleTestsRun() {
    return PageType.fromWikiPage(page) == PageType.SUITE;
  }

  protected void addFormatters(MultipleTestsRunner runner) {
    runner.addTestSystemListener(mainFormatter);
    if (rerunFormatter != null) {
      runner.addTestSystemListener(rerunFormatter);
    }
    runner.addExecutionLogListener(new ConsoleExecutionLogListener());
    if (mainFormatter instanceof ExecutionLogListener) {
      runner.addExecutionLogListener((ExecutionLogListener) mainFormatter);
    }
    for (Formatter formatter : context.formatterFactory.createFormatters()) {
      runner.addTestSystemListener(formatter);
    }
    if (context.testSystemListener != null) {
      runner.addTestSystemListener(context.testSystemListener);
    }
  }

  static SimpleResponse unsupportedFormatResponse(String format) throws java.io.UnsupportedEncodingException {
    if (format == null || "json".equalsIgnoreCase(format) || "html".equalsIgnoreCase(format)) {
      return null;
    }
    SimpleResponse rejected = new SimpleResponse(400);
    rejected.setContentType("text/plain");
    rejected.setContent(String.format(UNSUPPORTED_RESULT_FORMAT_MESSAGE, format.toLowerCase()));
    return rejected;
  }

  private void createMainFormatter() {
    if (response.isJsonFormat()) {
      boolean stream = request == null || !request.hasInput(fitnesse.http.Request.NOCHUNK);
      java.io.File failures = FailedPages.defaultFile(context.getRootPagePath());
      JsonResultsFormatter json = new JsonResultsFormatter(response.getWriter(), stream, failures);
      json.setRunId(testRunId);
      json.setSuiteName(fitnesse.wiki.PathParser.render(page.getFullPath()));
      mainFormatter = json;
    } else {
      mainFormatter = newHtmlFormatter();
    }
  }

  protected Formatter createRerunFormatter() {
    return null;
  }

  protected File getRerunPageFile() {
    return new File(context.getRootPagePath(), getRerunPageName() + ".md");
  }

  protected String getRerunPageName() {
    PageCrawler pageCrawler = page.getPageCrawler();
    WikiPagePath fullPath = pageCrawler.getFullPath();
    String fullPathName = PathParser.render(fullPath);
    if (fullPathName.startsWith("RerunLastFailures_")) {
      String newFullPathName = fullPathName.replace(".", "-");
      return newFullPathName;
    } else {
      String rerunPageName = "RerunLastFailures_" + fullPathName.replace(".", "-");
      String rerunSuiteFilter = getRerunSuiteFilterName();
      if (rerunSuiteFilter != null) {
        rerunPageName += "_suiteFilter_" + rerunSuiteFilter;
      }
      return rerunPageName;
    }
  }

  private String getRerunSuiteFilterName() {
    if (request == null) {
      return null;
    }
    String orFilterString = getOrFilterString(request);
    if (StringUtils.isBlank(orFilterString)) {
      return null;
    }
    String normalized = orFilterString.trim().replaceAll("[^A-Za-z0-9]+", "-");
    normalized = normalized.replaceAll("^-+|-+$", "");
    return StringUtils.isBlank(normalized) ? null : normalized;
  }

  protected String getTitle() {
    return "Test Results";
  }

  protected String mainTemplate() {
    return "testPage";
  }

  protected BaseFormatter newHtmlFormatter() {
    return new SuiteHtmlFormatter(page, isMultipleTestsRun(), response.getWriter());
  }

  protected void performExecution() throws TestExecutionException {
    MultipleTestsRunner runner = newMultipleTestsRunner(getPagesToRun());
    runningTestingTracker.addStartedProcess(testRunId, runner);
    if (isInteractive()) {
      ((InteractiveFormatter) mainFormatter).setTrackingId(testRunId);
    }
    try {
      runner.executeTestPages();
      if (mainFormatter instanceof JsonResultsFormatter json) {
        try {
          json.finish();
        } catch (IOException e) {
          throw new TestExecutionException(e);
        }
      }
    } finally {
      runningTestingTracker.removeEndedProcess(testRunId);
    }
  }

  protected List<WikiPage> getPagesToRun() {
    SuiteFilter filter = createSuiteFilter(request, page.getFullPath().toString());
    SuiteContentsFinder suiteTestFinder = new SuiteContentsFinder(page, filter, root);
    List<WikiPage> pages = suiteTestFinder.getAllPagesToRunForThisSuite();
    return onlyFailedPages(pages);
  }

  private List<WikiPage> onlyFailedPages(List<WikiPage> pages) {
    if (request == null || request.getInput("rerunFailures") == null) {
      return pages;
    }
    try {
      java.util.Set<String> only = new java.util.HashSet<>(
        FailedPages.read(java.nio.file.Path.of(request.getInput("rerunFailures"))));
      pages.removeIf(candidate -> !only.contains(PathParser.render(candidate.getFullPath())));
      return pages;
    } catch (java.io.IOException e) {
      throw new IllegalArgumentException("Cannot read rerun failures: " + e.getMessage());
    }
  }

  protected MultipleTestsRunner newMultipleTestsRunner(List<WikiPage> pages) {
    // Add test url inputs to context's variableSource.

    TestRun run = createRunProvider(pages);
    MultipleTestsRunner runner = new MultipleTestsRunner(run, context.testSystemFactory);

    runner.setRunInProcess(debug);
    runner.setEnableRemoteDebug(remoteDebug);
    addFormatters(runner);

    return runner;
  }

  protected TestRun createRunProvider(List<WikiPage> pages) {
    return context.testRunFactoryRegistry.createRun(pages);
  }

  public void addToResponse(String output) {
    if (!isClosed()) {
      try {
        response.add(output);
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Unable to send output", e);
      }
    }
  }

  synchronized boolean isClosed() {
    return isClosed;
  }

  synchronized void setClosed() {
    isClosed = true;
  }

  void closeHtmlResponse(int exitCode) throws IOException {
    if (!isClosed()) {
      setClosed();
      response.addTrailingHeader("Exit-Code", String.valueOf(exitCode));
      response.close();
    }
  }

  public Response getResponse() {
    return response;
  }

  public static SuiteFilter createSuiteFilter(Request request, String suitePath) {
    return new SuiteFilter(getOrTagFilter(request),
            getNotSuiteFilter(request),
            getAndTagFilters(request),
            getSuiteFirstTest(request, suitePath));
  }

  private static String getOrTagFilter(Request request) {
    return request != null ? getOrFilterString(request) : null;
  }

  private static String getOrFilterString(Request request) {
    //request already confirmed not-null
    String orFilterString = null;
    if(request.getInput(OR_FILTER_ARG_1) != null){
      orFilterString = request.getInput(OR_FILTER_ARG_1);
    } else {
      orFilterString = request.getInput(OR_FILTER_ARG_2);
    }
    return orFilterString;
  }

  private static String getNotSuiteFilter(Request request) {
    return request != null ? request.getInput(NOT_FILTER_ARG) : null;
  }

  private static String getAndTagFilters(Request request) {
    return request != null ? request.getInput(AND_FILTER_ARG) : null;
  }

  private static String getSuiteFirstTest(Request request, String suiteName) {
    String startTest = null;
    if (request != null) {
      startTest = request.getInput("firstTest");
    }

    if (startTest != null) {
      if (startTest.indexOf(suiteName) != 0) {
        startTest = suiteName + "." + startTest;
      }
    }

    return startTest;
  }

}
