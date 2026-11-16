package agnesse.testsystems.slim.tables;

import agnesse.testsystems.Expectation;
import agnesse.testsystems.TestResult;
import agnesse.testsystems.slim.results.SlimExceptionResult;

public interface SlimExpectation extends Expectation {

  SlimExpectation NOOP_EXPECTATION = new SlimExpectation() {
    @Override public TestResult evaluateExpectation(Object returnValues) { return null; }
    @Override public SlimExceptionResult evaluateException(SlimExceptionResult exceptionResult) { return null; }
  };

  TestResult evaluateExpectation(Object returnValues);

  SlimExceptionResult evaluateException(SlimExceptionResult exceptionResult);
}