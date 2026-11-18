package agnesse.slim.instructions;

import agnesse.slim.SlimException;

import java.util.Objects;

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

  protected final String described(String instruction, String details) {
    return "{id='" + getId() + "', instruction='" + instruction + "'" + details + "}";
  }

  /**
   * Parts that distinguish this instruction from another of the same class,
   * or null when only the id matters.
   */
  protected Object[] fields() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (fields() == null) return sameId(o);
    return sameClassAndFields(o);
  }

  private boolean sameId(Object o) {
    if (!(o instanceof Instruction)) return false;
    return id.equals(((Instruction) o).id);
  }

  private boolean sameClassAndFields(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Instruction that = (Instruction) o;
    return id.equals(that.id) && sameFields(that);
  }

  private boolean sameFields(Instruction that) {
    Object[] mine = fields();
    Object[] theirs = that.fields();
    for (int i = 0; i < mine.length; i++) {
      if (!Objects.equals(mine[i], theirs[i])) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    if (fields() == null) return id.hashCode();
    return fieldHash();
  }

  private int fieldHash() {
    int result = id.hashCode();
    for (Object field : fields()) {
      result = 31 * result + Objects.hashCode(field);
    }
    return result;
  }
}
