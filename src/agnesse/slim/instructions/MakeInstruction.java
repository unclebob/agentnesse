package agnesse.slim.instructions;

import agnesse.slim.SlimException;

import java.util.Arrays;

public class MakeInstruction extends Instruction {
  public static final String INSTRUCTION = "make";
  private final String instanceName;
  private final String className;
  private final Object[] args;

  public MakeInstruction(String id, String instanceName, String className) {
    this(id, instanceName, className, new Object[]{});
  }

  public MakeInstruction(String id, String instanceName, String className, Object[] args) {
    super(id);
    this.instanceName = instanceName;
    this.className = className;
    this.args = args;
  }

  @Override
  protected InstructionResult executeInternal(InstructionExecutor executor) throws SlimException {
    executor.create(instanceName, className, args);
    return new InstructionResult.Ok(getId());
  }

  @Override
  protected Object[] fields() {
    return new Object[] {instanceName, className, args == null ? null : Arrays.asList(args)};
  }

  @Override
  public String toString() {
    String rendered = args == null ? "null" : Arrays.asList(args).toString();
    return described(INSTRUCTION,
      ", instanceName='" + instanceName + "', className='" + className + "', args=" + rendered);
  }
}
