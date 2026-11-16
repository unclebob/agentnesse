package agnesse.junit;

import org.junit.runner.RunWith;

@RunWith(AgNesseRunner.class)
@AgNesseRunner.Suite("AgNesse.SuiteAcceptanceTests.SuiteSlimTests.TestScriptTable")
@AgNesseRunner.AgNesseDir(".")
@AgNesseRunner.OutputDir("./build/agnesse-results")
public class AgNesseRunnerTest {
}
