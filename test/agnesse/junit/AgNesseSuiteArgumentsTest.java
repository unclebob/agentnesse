package agnesse.junit;

import java.io.File;

import org.junit.Test;

import agnesse.junit.AgNesseRunner.AgNesseDir;
import agnesse.junit.AgNesseRunner.OutputDir;

import static org.junit.Assert.*;
public class AgNesseSuiteArgumentsTest {

  @AgNesseDir(systemProperty = "agnesse.root.dir.parent")
  @OutputDir(systemProperty = "java.io.tmpdir", pathExtension = "agnesse")
  private static class PropertiesConfiguredSuite {
  }

  @Test
  public void argumentsAreParsedCorrectly() throws Exception {
	System.setProperty("agnesse.root.dir.parent", ".");
    AgNesseSuite suite = new AgNesseSuite(AgNesseSuiteTest.class);
    assertEquals(".", suite.getAgNesseDir(AgNesseSuiteExampleTest.class));
    assertEquals(new File(System.getProperty("agnesse.root.dir.parent")).getAbsolutePath(), suite.getAgNesseDir(PropertiesConfiguredSuite.class));
    assertEquals("AgNesse.SuiteAcceptanceTests.SuiteSlimTests.TableTableSuite", suite.getSuiteName(AgNesseSuiteExampleTest.class));
    assertEquals("tmp",suite.getOutputDir(AgNesseSuiteExampleTest.class));
    assertEquals(new File(System.getProperty("java.io.tmpdir"),"agnesse").getAbsolutePath(),suite.getOutputDir(PropertiesConfiguredSuite.class));
    assertNull("null filter allowed", suite.getSuiteFilter(AgNesseSuiteExampleTest.class));
    assertNull("null exclude filter allowed", suite.getExcludeSuiteFilter(AgNesseSuiteExampleTest.class));
    assertEquals("testSuite", suite.getSuiteFilter(AgNesseSuiteWithFilterExampleTest.class));
    assertEquals("excludedSuite", suite.getExcludeSuiteFilter(AgNesseSuiteWithFilterExampleTest.class));
  }
}
