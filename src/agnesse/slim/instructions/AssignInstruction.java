package agnesse.slim.instructions;

import agnesse.slim.SlimException;

public class AssignInstruction extends Instruction {
  public static final String INSTRUCTION = "assign";
  private String symbolName;
  private Object value;

  public AssignInstruction(String id, String symbolName, Object value) {
    super(id);
    this.symbolName = symbolName;
    this.value = value;
  }

  @Override
  protected InstructionResult executeInternal(InstructionExecutor executor) throws SlimException {
    executor.assign(symbolName, value);
    return new InstructionResult.Ok(getId());
  }

  @Override
  protected Object[] fields() {
    return new Object[] {symbolName, value};
  }

  @Override
  public String toString() {
    return described(INSTRUCTION, ", symbolName='" + symbolName + "', value='" + value + "'");
  }
}
