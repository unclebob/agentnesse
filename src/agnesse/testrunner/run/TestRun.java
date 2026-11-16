package agnesse.testrunner.run;

import agnesse.testrunner.Stoppable;
import agnesse.testsystems.TestExecutionException;
import agnesse.wiki.WikiPage;

import java.util.List;

public interface TestRun extends Stoppable {
  void executeTestPages(RunCoordinator coordinator) throws TestExecutionException;

  List<WikiPage> getPages();
}
