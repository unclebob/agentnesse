package agnesse.slim.instructions;

import agnesse.slim.Lanes;
import agnesse.slim.SlimException;

import java.util.List;

/** One instruction that runs every lane on this connection. An empty form is the probe. */
public final class LanesInstruction extends Instruction {
  public static final String INSTRUCTION = "lanes";
  private final List<Object> statement;

  public LanesInstruction(String id) {
    super(id);
    statement = List.of(id, INSTRUCTION);
  }

  private LanesInstruction(String id, List<Object> statement) {
    super(id);
    this.statement = statement;
  }

  public static LanesInstruction fromWire(String id, List<Object> words) {
    return new LanesInstruction(id, words);
  }

  public List<Object> statement() {
    return statement;
  }

  @Override
  protected InstructionResult executeInternal(InstructionExecutor executor) throws SlimException {
    if (statement.size() <= 2) {
      return new InstructionResult(getId(), Lanes.READY);
    }
    return new InstructionResult(getId(), Lanes.run(executor, statement.subList(2, statement.size())));
  }
}
