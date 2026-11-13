package fitnesse.testsystems.slim;

import fitnesse.testsystems.ClassPath;
import fitnesse.testsystems.TestExecutionException;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.TestSystemListener;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class InstructionTestSystemTest {
  @Test
  public void TablesAreParsed() {
    TestPage page = new MyTestPage();
    StringBuilder result = new StringBuilder();
    InstructionTestSystem testSystem = new InstructionTestSystem(result);
    testSystem.addTestSystemListener(new MyListener());
    try {
      testSystem.runTests(page);
      String instructions = result.toString();
      assertTrue(instructions, instructions.startsWith("TestPageFullPath|"));
      assertTrue(instructions, instructions.contains("SampleClass"));
      assertTrue(instructions, instructions.contains("SomeProperty"));
      assertTrue(instructions, instructions.contains("in%23put"));
    } catch (TestExecutionException e) {
      e.printStackTrace();
    }
  }

  static class MyTestPage implements TestPage {
    @Override
    public String getName() {
      return null;
    }

    @Override
    public String getFullPath() {
      return "TestPageFullPath";
    }

    @Override
    public String getVariable(String name) {
      return null;
    }

    @Override
    public ClassPath getClassPath() {
      return null;
    }

    @Override
    public String getContent() {
      return "| script | SampleClass |\n| --- | --- |\n| check | SomeProperty | in%put | valid |\n";
    }

    @Override
    public String getHtml() {
      return "<table><tr><td>script</td><td>SampleClass</td></tr><tr><td>check</td><td>SomeProperty</td><td>in%put\r\n</td><td>valid</td></tr></table>";
    }
  }

  static class MyListener implements TestSystemListener {  }
}
