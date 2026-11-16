package agnesse.junit;

import org.junit.runner.RunWith;

/**
 * Tests backwards compatibility so AgNesseSuite can still be used.
 */
@Deprecated
@RunWith(AgNesseSuite.class)
@AgNesseSuite.Name("AgNesse.SuiteAcceptanceTests.SuiteSlimTests.TestScriptTable")
@AgNesseSuite.AgNesseDir(".")
@AgNesseSuite.OutputDir("./build/agnesse-results")
public class AgNesseSuiteTest {
}
