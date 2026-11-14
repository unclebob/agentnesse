package fitnesse.testrunner;

import java.io.Closeable;

import fitnesse.testsystems.CompositeTestSystemListener;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.TestSystemListener;
import fitnesse.testsystems.slim.MarkdownTable;
import fitnesse.testsystems.slim.TableFinished;
import util.FileUtil;

public class CompositeFormatter extends CompositeTestSystemListener implements TestsRunnerListener, TableFinished, Closeable {

  @Override
  public void announceNumberTestsToRun(int testsToRun) {
    for (TestSystemListener listener : listeners())
      if (listener instanceof TestsRunnerListener)
        ((TestsRunnerListener) listener).announceNumberTestsToRun(testsToRun);
  }

  @Override
  public void unableToStartTestSystem(final String testSystemName, final Throwable cause) {
    for (TestSystemListener listener : listeners())
      if (listener instanceof TestsRunnerListener)
        ((TestsRunnerListener) listener).unableToStartTestSystem(testSystemName, cause);
  }

  @Override
  public void tableFinished(TestPage page, MarkdownTable table) {
    for (TestSystemListener listener : listeners())
      if (listener instanceof TableFinished finished)
        finished.tableFinished(page, table);
  }

  @Override
  public void close() {
    for (TestSystemListener listener : listeners())
        if (listener instanceof Closeable)
          FileUtil.close((Closeable) listener);
  }
}
