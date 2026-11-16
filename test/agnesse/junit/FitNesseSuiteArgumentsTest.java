package agnesse.junit;

import java.io.File;

import org.junit.Test;

import agnesse.junit.FitNesseRunner.FitnesseDir;
import agnesse.junit.FitNesseRunner.OutputDir;

import static org.junit.Assert.*;
public class FitNesseSuiteArgumentsTest {

  @FitnesseDir(systemProperty = "agnesse.root.dir.parent")
  @OutputDir(systemProperty = "java.io.tmpdir", pathExtension = "fitnesse")
  private static class PropertiesConfiguredSuite {
  }

  @Test
  public void argumentsAreParsedCorrectly() throws Exception {
	System.setProperty("agnesse.root.dir.parent", ".");
    FitNesseSuite suite = new FitNesseSuite(FitNesseSuiteTest.class);
    assertEquals(".", suite.getFitNesseDir(FitNesseSuiteExampleTest.class));
    assertEquals(new File(System.getProperty("agnesse.root.dir.parent")).getAbsolutePath(), suite.getFitNesseDir(PropertiesConfiguredSuite.class));
    assertEquals("FitNesse.SuiteAcceptanceTests.SuiteSlimTests.TableTableSuite", suite.getSuiteName(FitNesseSuiteExampleTest.class));
    assertEquals("tmp",suite.getOutputDir(FitNesseSuiteExampleTest.class));
    assertEquals(new File(System.getProperty("java.io.tmpdir"),"fitnesse").getAbsolutePath(),suite.getOutputDir(PropertiesConfiguredSuite.class));
    assertNull("null filter allowed", suite.getSuiteFilter(FitNesseSuiteExampleTest.class));
    assertNull("null exclude filter allowed", suite.getExcludeSuiteFilter(FitNesseSuiteExampleTest.class));
    assertEquals("testSuite", suite.getSuiteFilter(FitNesseSuiteWithFilterExampleTest.class));
    assertEquals("excludedSuite", suite.getExcludeSuiteFilter(FitNesseSuiteWithFilterExampleTest.class));
  }
}
