package agnesse.testsystems.slim.tables;

import agnesse.testsystems.ExecutionResult;
import agnesse.testsystems.TestPage;
import agnesse.testsystems.TestSummary;
import agnesse.testsystems.slim.SlimTestContext;

import java.util.Collection;
import java.util.Map;

/**
 * Counts one scenario trial without adding those checks to the page.
 * Symbols and scenarios still belong to the page.
 */
final class QuietContext implements SlimTestContext {
  private final SlimTestContext inner;
  private final TestSummary summary = new TestSummary();

  QuietContext(SlimTestContext inner) {
    this.inner = inner;
  }

  boolean failed() {
    return summary.getWrong() > 0 || summary.getExceptions() > 0;
  }

  @Override public String getSymbol(String symbolName) { return inner.getSymbol(symbolName); }

  @Override public Map<String, String> getSymbols() { return inner.getSymbols(); }

  @Override public void setSymbol(String symbolName, String value) { inner.setSymbol(symbolName, value); }

  @Override
  public void addScenario(String scenarioName, ScenarioTable scenarioTable) {
    inner.addScenario(scenarioName, scenarioTable);
  }

  @Override public ScenarioTable getScenario(String scenarioName) { return inner.getScenario(scenarioName); }

  @Override
  public ScenarioTable getScenarioByPattern(String invokingString) {
    return inner.getScenarioByPattern(invokingString);
  }

  @Override public Collection<ScenarioTable> getScenarios() { return inner.getScenarios(); }

  @Override public void incrementPassedTestsCount() { summary.add(ExecutionResult.PASS); }

  @Override public void incrementFailedTestsCount() { summary.add(ExecutionResult.FAIL); }

  @Override public void incrementErroredTestsCount() { summary.add(ExecutionResult.ERROR); }

  @Override public void incrementIgnoredTestsCount() { summary.add(ExecutionResult.IGNORE); }

  @Override public void increment(ExecutionResult result) { summary.add(result); }

  @Override public void increment(TestSummary extra) { summary.add(extra); }

  @Override public TestPage getPageToTest() { return inner.getPageToTest(); }

  @Override
  public void setCurrentScriptClass(Class<? extends ScriptTable> currentScriptClass) {
    inner.setCurrentScriptClass(currentScriptClass);
  }

  @Override public Class<? extends ScriptTable> getCurrentScriptClass() { return inner.getCurrentScriptClass(); }

  @Override public void setCurrentScriptActor(String currentScriptActor) { inner.setCurrentScriptActor(currentScriptActor); }

  @Override public String getCurrentScriptActor() { return inner.getCurrentScriptActor(); }
}
