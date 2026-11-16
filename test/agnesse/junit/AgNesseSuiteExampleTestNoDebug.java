package agnesse.junit;

import org.junit.runner.RunWith;

@RunWith(AgNesseRunner.class)
@AgNesseRunner.Suite("AgNesse.SuiteAcceptanceTests.SuiteSlimTests.TestSystemSlimSuite")
@AgNesseRunner.AgNesseDir(".")
@AgNesseRunner.OutputDir(systemProperty = "java.io.tmpdir", pathExtension = "agnesse")
@AgNesseRunner.ExcludeSuiteFilter("noJunit")

public class AgNesseSuiteExampleTestNoDebug {
}
