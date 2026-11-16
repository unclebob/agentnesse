package agnesse.junit;

import org.junit.Test;
import org.junit.runner.RunWith;

import agnesse.junit.FitNesseRunner.FitnesseDir;
import agnesse.junit.FitNesseSuite.Name;
import agnesse.junit.FitNesseRunner.OutputDir;
@RunWith(FitNesseSuite.class)
@Name("AgNesse.SuiteAcceptanceTests.SuiteSlimTests.TableTableSuite")
@FitnesseDir(".")
@OutputDir("tmp")
public class FitNesseSuiteExampleTest {

  @Test
  public void dummy(){
    
  }
}
