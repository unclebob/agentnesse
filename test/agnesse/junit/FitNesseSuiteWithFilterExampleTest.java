package agnesse.junit;

import org.junit.Test;
import org.junit.runner.RunWith;

import agnesse.junit.FitNesseRunner.ExcludeSuiteFilter;
import agnesse.junit.FitNesseRunner.FitnesseDir;
import agnesse.junit.FitNesseSuite.Name;
import agnesse.junit.FitNesseRunner.OutputDir;
import agnesse.junit.FitNesseRunner.SuiteFilter;

@RunWith(FitNesseSuite.class)
@Name("AgNesse.SuiteAcceptanceTests.SuiteSlimTests")
@FitnesseDir(".")
@OutputDir(systemProperty = "java.io.tmpdir", pathExtension = "fitnesse")
@SuiteFilter("testSuite")
@ExcludeSuiteFilter("excludedSuite")
public class FitNesseSuiteWithFilterExampleTest {
  @Test
  public void dummy(){

  }
}
