// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.testsystems.slim;

import java.util.Collection;
import java.util.Map;

import agnesse.testsystems.ExecutionResult;
import agnesse.testsystems.TestPage;
import agnesse.testsystems.TestSummary;
import agnesse.testsystems.slim.tables.ScenarioTable;
import agnesse.testsystems.slim.tables.ScriptTable;

public interface SlimTestContext {
  String getSymbol(String symbolName);

  Map<String, String> getSymbols();

  void setSymbol(String symbolName, String value);

  void addScenario(String scenarioName, ScenarioTable scenarioTable);

  ScenarioTable getScenario(String scenarioName);

  ScenarioTable getScenarioByPattern(String invokingString);

  Collection<ScenarioTable> getScenarios();

  void incrementPassedTestsCount();

  void incrementFailedTestsCount();

  void incrementErroredTestsCount();

  void incrementIgnoredTestsCount();

  void increment(ExecutionResult testSummary);

  void increment(TestSummary testSummary);

  TestPage getPageToTest();

  void setCurrentScriptClass(Class<? extends ScriptTable> currentScriptClass);

  Class<? extends ScriptTable> getCurrentScriptClass();

  void setCurrentScriptActor(String currentScriptActor);

  String getCurrentScriptActor();
}
