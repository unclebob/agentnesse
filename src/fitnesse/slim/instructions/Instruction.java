package fitnesse.slim.instructions;

import fitnesse.slim.SlimException;

public abstract class Instruction {
  public static final Instruction NOOP_INSTRUCTION = new Instruction("NOOP") {
    @Override
    protected InstructionResult executeInternal(InstructionExecutor executor) throws SlimException {
      return new InstructionResult.Void(getId());
    }
  };

  private String id;

  public Instruction(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  public final InstructionResult execute(InstructionExecutor executor) {
    
    try {
      return executeInternal(executor);
    } catch (SlimException e) {
      return new InstructionResult.Error(getId(), e);
    }
  }

  protected abstract InstructionResult executeInternal(InstructionExecutor executor) throws SlimException;

  @Override
  public String toString() {
    return "Instruction{id='" + id + "'}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Instruction)) return false;

    Instruction that = (Instruction) o;

    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
