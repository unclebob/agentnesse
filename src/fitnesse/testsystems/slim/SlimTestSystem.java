// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.testsystems.slim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fitnesse.slim.instructions.AssignInstruction;
import fitnesse.slim.instructions.Instruction;
import fitnesse.testsystems.*;
import fitnesse.testsystems.slim.results.SlimExceptionResult;
import fitnesse.testsystems.slim.results.SlimServerRecord;
import fitnesse.testsystems.slim.tables.SlimAssertion;
import fitnesse.testsystems.slim.tables.SlimTable;
import fitnesse.wiki.PageData;

import static fitnesse.slim.SlimServer.*;

public abstract class SlimTestSystem implements TestSystem {
  private final SlimClient slimClient;
  private final CompositeTestSystemListener testSystemListener;
  private final String testSystemName;

  private SlimTestContextImpl testContext;
  private boolean stopTestCalled;
  private boolean ignoreAllTestsCalled;
  private boolean stopSuiteCalled;
  private boolean testSystemIsStopped;
  private SlimServerRecord server = SlimServerRecord.started();

  public SlimTestSystem(String testSystemName, SlimClient slimClient) {
    this.testSystemName = testSystemName;
    this.slimClient = slimClient;
    this.testSystemListener = new CompositeTestSystemListener();
  }

  public SlimTestContext getTestContext() {
    return testContext;
  }

  public SlimServerRecord server() {
    return server;
  }

  @Override
  public String getName() {
    return testSystemName;
  }

  @Override
  public boolean isSuccessfullyStarted() {
    return !testSystemIsStopped;
  }

  @Override
  public void start() throws UnableToStartException {
    try {
      slimClient.start();
    } catch (SlimVersionMismatch slimVersionMismatch) {
      stopTestSystem(slimVersionMismatch);
      return;
    } catch (IOException e) {
      stopTestSystem(e);
      throw new UnableToStartException("Could not start test system", e);
    } catch (Exception e) {
      stopTestSystem(e);
      throw e;
    }
    testSystemListener.testSystemStarted(this);
  }

  @Override
  public void kill() {
    slimClient.kill();
  }

  @Override
  public void bye() throws UnableToStopException {
    if (testSystemIsStopped) return;
    try {
      slimClient.bye();
      testSystemStopped(null);
    } catch (IOException e) {
      stopTestSystem(e);
      throw new UnableToStopException("Could not stop test system", e);
    }
  }

  @Override
  public void runTests(TestPage pageToTest) throws TestExecutionException {
    initializeTest(pageToTest);

    testStarted(pageToTest);
    try {
      processAllTablesOnPage(pageToTest);
      testComplete(pageToTest, testContext.getTestSummary());
    } catch (Exception e) {
      stopTestSystem(e);
      throw new TestExecutionException(e);
    }
  }

  @Override
  public void addTestSystemListener(TestSystemListener listener) {
    testSystemListener.addTestSystemListener(listener);
  }

  protected void initializeTest(TestPage testPage) {
    testContext = createTestContext(testPage);
    stopTestCalled = false;
    ignoreAllTestsCalled = false;
  }

  protected SlimTestContextImpl createTestContext(TestPage testPage) {
    return new SlimTestContextImpl(testPage);
  }

  protected abstract void processAllTablesOnPage(TestPage testPage) throws TestExecutionException;

  protected void processTable(SlimTable table, boolean isSuiteTearDownPage) throws TestExecutionException {
    List<SlimAssertion> assertions = table.getAssertions();
    evaluateTables(assertions, resultsFor(table, isSuiteTearDownPage, assertions));
  }

  private Map<String, Object> resultsFor(SlimTable table, boolean isSuiteTearDownPage, List<SlimAssertion> assertions) throws SlimCommunicationException {
    if (skipBecauseStopped(table)) {
      return Collections.emptyMap();
    }
    return resultsAfterStopFlags(table, isSuiteTearDownPage, assertions);
  }

  private boolean skipBecauseStopped(SlimTable table) {
    return (stopTestCalled || ignoreAllTestsCalled) && !table.isTearDown();
  }

  private Map<String, Object> resultsAfterStopFlags(SlimTable table, boolean isSuiteTearDownPage, List<SlimAssertion> assertions) throws SlimCommunicationException {
    boolean tearDownOfAlreadyStartedTest = stopTestCalled && table.isTearDown();
    if (ignoreAllTestsCalled && table.isTearDown()) {
      ignoreAllTestsCalled = false;
    }
    if (stopSuiteCalled && !isSuiteTearDownPage && !tearDownOfAlreadyStartedTest) {
      return Collections.emptyMap();
    }
    return slimClient.invokeAndGetResponse(SlimAssertion.getInstructions(assertions));
  }

  protected void evaluateTables(List<SlimAssertion> assertions, Map<String, Object> instructionResults) throws SlimCommunicationException {
    boolean ignoreTestTable = false;
    for (SlimAssertion assertion : assertions) {
      final String key = assertion.getInstruction().getId();
      Ignored ignored = applyIgnore(instructionResults.get(key), ignoreTestTable);
      ignoreTestTable = ignored.ignoreTable;
      if (isExceptionResult(ignored.value)) {
        noteException(assertion, key, (String) ignored.value);
      } else {
        noteResult(assertion, ignored.value);
      }
    }
  }

  private Ignored applyIgnore(Object returnValue, boolean ignoreTestTable) {
    if (ignoreAllTestsCalled) {
      return new Ignored("IGNORE_SCRIPT_TEST", true);
    }
    if (returnValue != null && returnValue.toString().contains(EXCEPTION_IGNORE_ALL_TESTS_TAG)) {
      ignoreAllTestsCalled = true;
      return new Ignored(returnValue, true);
    }
    if (returnValue != null && returnValue.toString().contains(EXCEPTION_IGNORE_SCRIPT_TEST_TAG)) {
      return new Ignored(returnValue, true);
    }
    if (ignoreTestTable) {
      return new Ignored("IGNORE_SCRIPT_TEST", true);
    }
    return new Ignored(returnValue, false);
  }

  private static boolean isExceptionResult(Object returnValue) {
    return returnValue instanceof String && ((String) returnValue).startsWith(EXCEPTION_TAG);
  }

  private void noteException(SlimAssertion assertion, String key, String returnValue) throws SlimCommunicationException {
    SlimExceptionResult exceptionResult = new SlimExceptionResult(key, returnValue);
    if (exceptionResult.isStopTestException()) {
      stopTestCalled = true;
      stopSuiteCalled = PageData.SUITE_SETUP_NAME.equals(testContext.getPageToTest().getName());
    }
    if (exceptionResult.isStopSuiteException()) {
      stopTestCalled = true;
      stopSuiteCalled = true;
    }
    exceptionResult = assertion.getExpectation().evaluateException(exceptionResult);
    if (exceptionResult == null) {
      return;
    }
    if (exceptionResult.isCatchException()) {
      testAssertionVerified(assertion, exceptionResult.catchTestResult());
    } else {
      testExceptionOccurred(assertion, exceptionResult);
    }
  }

  private void noteResult(SlimAssertion assertion, Object returnValue) throws SlimCommunicationException {
    TestResult testResult = assertion.getExpectation().evaluateExpectation(returnValue);
    testAssertionVerified(assertion, testResult);
    storeVariables(testResult);
  }

  private void storeVariables(TestResult testResult) throws SlimCommunicationException {
    if (testResult == null) {
      return;
    }
    Map<String, ?> variables = testResult.getVariablesToStore();
    if (variables == null || variables.isEmpty()) {
      return;
    }
    List<Instruction> instructions = new ArrayList<>(variables.size());
    int i = 0;
    for (Entry<String, ?> variable : variables.entrySet()) {
      instructions.add(new AssignInstruction("assign_" + i++, variable.getKey(), variable.getValue()));
    }
    slimClient.invokeAndGetResponse(instructions);
  }

  private static final class Ignored {
    private final Object value;
    private final boolean ignoreTable;

    private Ignored(Object value, boolean ignoreTable) {
      this.value = value;
      this.ignoreTable = ignoreTable;
    }
  }

  protected void testOutputChunk(TestPage testPage, String output) {
    testSystemListener.testOutputChunk(testPage, output);
  }

  protected void testStarted(TestPage testPage) {
    testSystemListener.testStarted(testPage);
  }

  protected void testComplete(TestPage testPage, TestSummary testSummary) {
    testSystemListener.testComplete(testPage, testSummary);
  }

  protected void stopTestSystem(Throwable e) {
    slimClient.kill();
    testSystemStopped(e);
  }

  protected void testAssertionVerified(Assertion assertion, TestResult testResult) {
    testSystemListener.testAssertionVerified(assertion, testResult);
  }

  protected void testExceptionOccurred(Assertion assertion, ExceptionResult exceptionResult) {
    testSystemListener.testExceptionOccurred(assertion, exceptionResult);
  }

  // Ensure testSystemStopped is called only once per test system. First call counts.
  protected void testSystemStopped(Throwable e) {
    if (testSystemIsStopped) return;
    testSystemIsStopped = true;
    if (e != null) {
      server = SlimServerRecord.stopped(e);
    }
    testSystemListener.testSystemStopped(this, e);
  }
}
