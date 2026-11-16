package agnesse.testrunner;

import java.io.Closeable;

import agnesse.testsystems.CompositeTestSystemListener;
import agnesse.testsystems.TestPage;
import agnesse.testsystems.TestSystemListener;
import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.TableFinished;
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
