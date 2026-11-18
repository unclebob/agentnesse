package agnesse.slim.instructions;

import agnesse.slim.SlimException;

public class ImportInstruction extends Instruction {
  public static final String INSTRUCTION = "import";
  private String path;

  public ImportInstruction(String id, String path) {
    super(id);
    this.path = path;
  }

  @Override
  protected InstructionResult executeInternal(InstructionExecutor executor) throws SlimException {
    executor.addPath(this.path);
    return new InstructionResult.Ok(getId());
  }

  @Override
  protected Object[] fields() {
    return new Object[] {path};
  }

  @Override
  public String toString() {
    return described(INSTRUCTION, ", path='" + path + "'");
  }
}
