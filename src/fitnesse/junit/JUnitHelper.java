package fitnesse.junit;

import fitnesse.ContextConfigurator;
import fitnesse.FitNesseContext;
import fitnesse.testrunner.MultipleTestsRunner;
import fitnesse.testrunner.SuiteContentsFinder;
import fitnesse.testrunner.run.TestRun;
import fitnesse.testsystems.ConsoleExecutionLogListener;
import fitnesse.testsystems.TestSummary;
import fitnesse.testsystems.TestSystemListener;
import fitnesse.wiki.WikiPage;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JUnitHelper {

  public static final String PAGE_TYPE_SUITE="suite";
  public static final String PAGE_TYPE_TEST="test";
  private final String outputDir;
  private final String fitNesseRootPath;
  private final TestSystemListener resultsListener;
  private int port = 0;
  private boolean debugMode = true;

  public JUnitHelper(String fitNesseRootPath, String outputPath) {
    this(fitNesseRootPath, outputPath, new PrintTestListener());
  }

  public JUnitHelper(String fitNesseDir, String outputDir,
                     TestSystemListener resultsListener) {
    this.fitNesseRootPath = fitNesseDir;
    this.outputDir = outputDir;
    this.resultsListener = resultsListener;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setDebugMode(boolean enabled) {
    debugMode = enabled;
  }

  public void assertTestPasses(String testName) throws Exception {
    assertPasses(testName, PAGE_TYPE_TEST, null);
  }

  public void assertSuitePasses(String suiteName) throws Exception {
    assertPasses(suiteName, PAGE_TYPE_SUITE, null);
  }

  public void assertSuitePasses(String suiteName, String suiteFilter) throws Exception {
    assertPasses(suiteName, PAGE_TYPE_SUITE, suiteFilter);
  }

  public void assertSuitePasses(String suiteName, String suiteFilter, String excludeSuiteFilter) throws Exception {
    assertPasses(suiteName, PAGE_TYPE_SUITE, suiteFilter, excludeSuiteFilter);
  }

  public void assertPasses(String pageName, String pageType, String suiteFilter) throws Exception {
    assertPasses(pageName, pageType, suiteFilter, null);
  }

  public void assertPasses(String pageName, String pageType, String suiteFilter, String excludeSuiteFilter) throws Exception {
    FitNesseContext context = ContextConfigurator
      .systemDefaults()
      .withRootPath(fitNesseRootPath)
      .withPort(port)
      .makeFitNesseContext();

    fitnesse.results.JsonResultsFormatter results = new fitnesse.results.JsonResultsFormatter(new StringBuilder(), false, null);
    List<WikiPage> pages = initChildren(pageName, suiteFilter, excludeSuiteFilter, context);
    TestRun run = createTestRun(context, pages);
    MultipleTestsRunner testRunner = createTestRunner(run, context, debugMode);
    testRunner.addTestSystemListener(results);
    testRunner.addTestSystemListener(resultsListener);
    testRunner.addExecutionLogListener(results);
    testRunner.addExecutionLogListener(new ConsoleExecutionLogListener());

    testRunner.executeTestPages();
    results.finish();
    fitnesse.results.JunitXmlWriter.write(results.document(), new File(outputDir, pageName + ".xml").toPath());
    TestSummary summary = fitnesse.results.RunDocument.countsOf(
      results.document().getJSONObject("run").getJSONObject("counts"));

    assertEquals("wrong", 0, summary.getWrong());
    assertEquals("exceptions", 0, summary.getExceptions());
    assertTrue(msgAtLeastOneTest(pageName, summary), summary.getRight() > 0);
  }

  private List<WikiPage> initChildren(String suiteName, String suiteFilter, String excludeSuiteFilter, FitNesseContext context) {
    WikiPage suiteRoot = getSuiteRootPage(suiteName, context, Collections.emptyMap());
    if (!suiteRoot.getData().hasAttribute("Suite")) {
      return Arrays.asList(suiteRoot);
    }
    return new SuiteContentsFinder(suiteRoot, new fitnesse.testrunner.SuiteFilter(suiteFilter, excludeSuiteFilter), context.getRootPage()).getAllPagesToRunForThisSuite();
  }

  static WikiPage getSuiteRootPage(String suiteName, FitNesseContext context, Map<String, String> customProperties) {
    return context.getPage(suiteName, customProperties);
  }

  static MultipleTestsRunner createTestRunner(TestRun run, FitNesseContext context, boolean debugMode) {
    MultipleTestsRunner runner = new MultipleTestsRunner(run, context.testSystemFactory);
    runner.setRunInProcess(debugMode);
    return runner;
  }

  static TestRun createTestRun(FitNesseContext context, List<WikiPage> pages) {
    return context.testRunFactoryRegistry.createRun(pages);
  }

  static String msgAtLeastOneTest(String pageName, TestSummary summary) {
    return MessageFormat.format("at least one test executed in {0}\n{1}",
      pageName, summary.toString());
  }
}
