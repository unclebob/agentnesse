package agnesse.junit;

import org.junit.Test;
import org.junit.runner.RunWith;

import agnesse.junit.AgNesseRunner.AgNesseDir;
import agnesse.junit.AgNesseSuite.Name;
import agnesse.junit.AgNesseRunner.OutputDir;
@RunWith(AgNesseSuite.class)
@Name("AgNesse.SuiteAcceptanceTests.SuiteSlimTests.TableTableSuite")
@AgNesseDir(".")
@OutputDir("tmp")
public class AgNesseSuiteExampleTest {

  @Test
  public void dummy(){
    
  }
}
