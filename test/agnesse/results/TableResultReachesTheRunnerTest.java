package agnesse.results;

import agnesse.testrunner.CompositeFormatter;
import agnesse.testrunner.MultipleTestsRunner;
import agnesse.testrunner.TestsRunnerListener;
import agnesse.testrunner.run.TestRun;
import agnesse.testrunner.run.TestRunFactoryRegistry;
import agnesse.testsystems.Descriptor;
import agnesse.testsystems.TestPage;
import agnesse.testsystems.TestSummary;
import agnesse.testsystems.TestSystem;
import agnesse.testsystems.TestSystemFactory;
import agnesse.testsystems.TestSystemListener;
import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.TableFinished;
import agnesse.testutil.FitNesseUtil;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
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
    public void testSystemStarted(agnesse.testsystems.TestSystem testSystem) {
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
    public void testSystemStopped(agnesse.testsystems.TestSystem testSystem, Throwable cause) {
    }

    @Override
    public void testAssertionVerified(agnesse.testsystems.Assertion assertion, agnesse.testsystems.TestResult testResult) {
    }

    @Override
    public void testExceptionOccurred(agnesse.testsystems.Assertion assertion, agnesse.testsystems.ExceptionResult exceptionResult) {
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
