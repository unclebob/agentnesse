package fitnesse.results;

import fitnesse.testrunner.CompositeFormatter;
import fitnesse.testrunner.MultipleTestsRunner;
import fitnesse.testrunner.TestsRunnerListener;
import fitnesse.testrunner.run.TestRun;
import fitnesse.testrunner.run.TestRunFactoryRegistry;
import fitnesse.testsystems.Descriptor;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.TestSummary;
import fitnesse.testsystems.TestSystem;
import fitnesse.testsystems.TestSystemFactory;
import fitnesse.testsystems.TestSystemListener;
import fitnesse.testsystems.slim.MarkdownTable;
import fitnesse.testsystems.slim.TableFinished;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import org.json.JSONArray;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** The runner's own listener, not a listener hung directly on Slim, receives each table. */
public class TableResultReachesTheRunnerTest {

  @Test
  public void aFormatterAddedToTheRunnerReceivesTheTable() throws Exception {
    WikiPage root = FitNesseUtil.makeTestContext().getRootPage();
    WikiPage suite = WikiPageUtil.addPage(root, PathParser.parse("Suite"), "");
    WikiPage test = WikiPageUtil.addPage(suite, PathParser.parse("One"), "| script |\n| --- |\n| check |\n");
    PageData data = test.getData();
    data.setAttribute("Test");
    test.commit(data);

    JsonResultsFormatter formatter = new JsonResultsFormatter(new StringBuilder(), false, null);
    formatter.setSuiteName("Suite");
    MultipleTestsRunner runner = new MultipleTestsRunner(run(test), new PublishingFactory());
    runner.addTestSystemListener(formatter);
    runner.executeTestPages();
    formatter.finish();

    JSONArray blocks = formatter.document().getJSONObject("run").getJSONArray("pages").getJSONObject(0).getJSONArray("blocks");
    assertFalse(blocks.isEmpty());
    assertEquals("script", blocks.getJSONObject(0).getString("name"));
    assertEquals("Suite", formatter.document().getJSONObject("run").getString("name"));
  }

  @Test
  public void aTestSystemThatCannotStartIsReported() {
    CompositeFormatter formatters = new CompositeFormatter();
    RememberingListener listener = new RememberingListener();
    formatters.addTestSystemListener(listener);
    formatters.unableToStartTestSystem("slim", new IllegalStateException("down"));
    assertEquals("slim", listener.name);
  }

  private static final class RememberingListener implements TestSystemListener, TestsRunnerListener {
    private String name;

    @Override
    public void announceNumberTestsToRun(int testsToRun) {
    }

    @Override
    public void unableToStartTestSystem(String testSystemName, Throwable cause) {
      name = testSystemName;
    }

    @Override
    public void testSystemStarted(fitnesse.testsystems.TestSystem testSystem) {
    }

    @Override
    public void testOutputChunk(TestPage testPage, String output) {
    }

    @Override
    public void testStarted(TestPage testPage) {
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
    }

    @Override
    public void testSystemStopped(fitnesse.testsystems.TestSystem testSystem, Throwable cause) {
    }

    @Override
    public void testAssertionVerified(fitnesse.testsystems.Assertion assertion, fitnesse.testsystems.TestResult testResult) {
    }

    @Override
    public void testExceptionOccurred(fitnesse.testsystems.Assertion assertion, fitnesse.testsystems.ExceptionResult exceptionResult) {
    }
  }

  private static TestRun run(WikiPage page) {
    return TestRunFactoryRegistry.DEFAULT.createRun(asList(page));
  }

  private static final class PublishingFactory implements TestSystemFactory {
    @Override
    public TestSystem create(Descriptor descriptor) {
      return new PublishingSystem();
    }
  }

  private static final class PublishingSystem implements TestSystem {
    private TestSystemListener listener;

    @Override
    public String getName() {
      return "slim";
    }

    @Override
    public void start() {
    }

    @Override
    public void bye() {
    }

    @Override
    public void kill() {
    }

    @Override
    public boolean isSuccessfullyStarted() {
      return true;
    }

    @Override
    public void addTestSystemListener(TestSystemListener listener) {
      this.listener = listener;
    }

    @Override
    public void runTests(TestPage page) {
      listener.testStarted(page);
      if (listener instanceof TableFinished finished) {
        finished.tableFinished(page, MarkdownTable.parse("| script |\n| --- |\n| check |\n"));
      }
      listener.testComplete(page, new TestSummary(1, 0, 0, 0));
    }
  }
}
