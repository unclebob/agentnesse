package agnesse.testsystems.slim;

import agnesse.slim.SlimVersion;
import agnesse.slim.instructions.Instruction;
import agnesse.slim.protocol.SlimListBuilder;
import agnesse.slim.protocol.SlimSerializer;
import agnesse.testsystems.TestExecutionException;
import agnesse.testsystems.TestPage;
import agnesse.testsystems.TestSummary;
import agnesse.testsystems.TestSystem;
import agnesse.testsystems.TestSystemListener;
import agnesse.testsystems.UnableToStartException;
import agnesse.testsystems.UnableToStopException;
import agnesse.testsystems.slim.tables.SlimAssertion;
import agnesse.testsystems.slim.tables.SlimTable;
import agnesse.testsystems.slim.tables.SlimTableFactory;

import java.util.List;

public class InstructionTestSystem implements TestSystem {

  public InstructionTestSystem(StringBuilder result) {
    this.result = result;
  }

  @Override
  public String getName() {
    return "slim";
  }

  @Override
  public void start() throws UnableToStartException { }

  @Override
  public void bye() throws UnableToStopException { }

  @Override
  public void kill() { }

  @Override
  public void runTests(TestPage page) throws TestExecutionException {
    List<SlimTable> tables = SlimPage.Make(page, new SlimTestContextImpl(page), new SlimTableFactory()).getTables();
    for (SlimTable table: tables) {
      List<Instruction> instructions = SlimAssertion.getInstructions(table.getAssertions());
      String serial = SlimSerializer.serialize(new SlimListBuilder(Double.parseDouble(SlimVersion.VERSION)).toList(instructions));
      result.append(page.getFullPath()).append("|").append(encode(serial)).append(System.lineSeparator());
    }
    listener.testComplete(page, new TestSummary());
  }

  @Override
  public boolean isSuccessfullyStarted() {
    return true;
  }

  @Override public void addTestSystemListener(TestSystemListener listener) { this.listener = listener; }

  private String encode(String input) {
    return input
      .replace("%", "%23")
      .replace("\r", "%0A")
      .replace("\n", "%0D");
  }

  private final StringBuilder result;

  private TestSystemListener listener;
}
