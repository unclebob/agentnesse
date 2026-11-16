package agnesse.slim.protocol;

import java.util.ArrayList;
import java.util.List;

import agnesse.slim.SlimError;
import agnesse.slim.SlimException;
import agnesse.slim.instructions.AssignInstruction;
import agnesse.slim.instructions.CallAndAssignInstruction;
import agnesse.slim.instructions.CallInstruction;
import agnesse.slim.instructions.ImportInstruction;
import agnesse.slim.instructions.Instruction;
import agnesse.slim.instructions.InstructionExecutor;
import agnesse.slim.instructions.MakeInstruction;

import static java.util.Arrays.asList;

public class SlimListBuilder {

  private final double slimVersion;

  public SlimListBuilder(double slimVersion) {
    this.slimVersion = slimVersion;
  }

  private interface ToListExecutor extends InstructionExecutor {

  }

  public List<Object> toList(List<Instruction> instructions) {
    final List<Object> statementsAsList = new ArrayList<>(instructions.size());
    for (final Instruction instruction : instructions) {
      ToListExecutor executor = new ToListExecutor() {

        private List<Object> mergeAsList(Object[] a, Object[] b) {
          List<Object> l = new ArrayList<>(a.length + b.length);
          l.addAll(asList(a));
          l.addAll(asList(b));
          return l;
        }

        @Override
        public void addPath(String path) throws SlimException {
          statementsAsList.add(asList(instruction.getId(), ImportInstruction.INSTRUCTION, path));
        }

        @Override
        public Object callAndAssign(String symbolName, String instanceName, String methodsName, Object... arguments) throws SlimException {
          Object[] list = new Object[]{instruction.getId(), CallAndAssignInstruction.INSTRUCTION, symbolName, instanceName, methodsName};
          statementsAsList.add(mergeAsList(list, arguments));

          return null;
        }

        @Override
        public Object call(String instanceName, String methodName, Object... arguments) throws SlimException {
          Object[] list = new Object[]{instruction.getId(), CallInstruction.INSTRUCTION, instanceName, methodName};
          statementsAsList.add(mergeAsList(list, arguments));
          return null;
        }

        @Override
        public void create(String instanceName, String className, Object... constructorArgs) throws SlimException {
          Object[] list = new Object[]{instruction.getId(), MakeInstruction.INSTRUCTION, instanceName, className};
          statementsAsList.add(mergeAsList(list, constructorArgs));
        }

        @Override
        public void assign(String symbolName, Object value) {
          if (slimVersion < 0.4) {
            throw new SlimError("The assign instruction is available as of SLIM protocol version 0.4");
          }
          Object[] list = new Object[]{instruction.getId(), AssignInstruction.INSTRUCTION, symbolName, value};
          statementsAsList.add(asList(list));
        }
      };

      instruction.execute(executor);
    }
    return statementsAsList;
  }

}
