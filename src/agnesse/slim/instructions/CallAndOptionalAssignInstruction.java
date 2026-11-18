package agnesse.slim.instructions;

import agnesse.slim.SlimException;

import java.util.Arrays;
import java.util.Optional;

abstract class CallAndOptionalAssignInstruction extends Instruction {
  private final String instructionName;
  private Optional<String> symbolName;
  private String instanceName;
  private String methodName;
  private Object[] args;

  protected CallAndOptionalAssignInstruction(String instructionName, String id, Optional<String> symbolName, String instanceName, String methodName, Object[] args) {
    super(id);
    this.instructionName = instructionName;
    this.symbolName = symbolName;
    this.instanceName = instanceName;
    this.methodName = methodName;
    this.args = args;
  }

  @Override
  protected InstructionResult executeInternal(InstructionExecutor executor) throws SlimException {
    Object result;
    if (symbolName.isPresent()) {
      result = executor.callAndAssign(symbolName.get(), instanceName, methodName, args);
    } else {
      result = executor.call(this.instanceName, this.methodName, this.args);
    }
    return new InstructionResult(getId(), result);
  }

  @Override
  protected Object[] fields() {
    return new Object[] {symbolName, instanceName, methodName, args == null ? null : Arrays.asList(args)};
  }

  @Override
  public String toString() {
    return described(instructionName, details());
  }

  private String details() {
    String symbol = symbolName.map(name -> ", symbolName='" + name + "'").orElse("");
    return symbol
      + ", instanceName='" + instanceName + "'"
      + ", methodName='" + methodName + "'"
      + ", args=" + Arrays.toString(args);
  }
}
