package agnesse.junit;

import org.junit.Test;
import org.junit.runner.RunWith;

import agnesse.junit.AgNesseRunner.ExcludeSuiteFilter;
import agnesse.junit.AgNesseRunner.AgNesseDir;
import agnesse.junit.AgNesseSuite.Name;
import agnesse.junit.AgNesseRunner.OutputDir;
import agnesse.junit.AgNesseRunner.SuiteFilter;

@RunWith(AgNesseSuite.class)
@Name("AgNesse.SuiteAcceptanceTests.SuiteSlimTests")
@AgNesseDir(".")
@OutputDir(systemProperty = "java.io.tmpdir", pathExtension = "agnesse")
@SuiteFilter("testSuite")
@ExcludeSuiteFilter("excludedSuite")
public class AgNesseSuiteWithFilterExampleTest {
  @Test
  public void dummy(){

  }
}
