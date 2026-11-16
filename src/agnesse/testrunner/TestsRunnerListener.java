package agnesse.testrunner;

public interface TestsRunnerListener {

  void announceNumberTestsToRun(int testsToRun);

  void unableToStartTestSystem(String testSystemName, Throwable cause);
}
