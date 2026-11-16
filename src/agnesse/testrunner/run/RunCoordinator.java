package agnesse.testrunner.run;

import agnesse.testrunner.WikiPageIdentity;
import agnesse.testsystems.TestPage;
import agnesse.testsystems.TestSystem;
import agnesse.testsystems.slim.TestingInterruptedException;

import java.util.List;

public interface RunCoordinator {
  boolean isNotStopped();

  void announceTotalTestsToRun(int toRun);

  TestSystem startTestSystem(WikiPageIdentity identity, List<TestPage> testPages);

  int announceTestStarted();

  void waitForNoTestsInProgress() throws TestingInterruptedException;

  void reportException(Exception e);
}
