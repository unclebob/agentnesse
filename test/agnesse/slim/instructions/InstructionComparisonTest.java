package agnesse.slim.instructions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class InstructionComparisonTest {
  @Test
  public void assignMatchesOnIdSymbolAndValue() {
    AssignInstruction instruction = new AssignInstruction("id", "symbol", "value");

    assertEquals(instruction, instruction);
    assertEquals(hashed("id", "symbol", "value"), instruction.hashCode());
    assertEquals(instruction.hashCode(), new AssignInstruction("id", "symbol", "value").hashCode());
    assertEquals(instruction, new AssignInstruction("id", "symbol", "value"));
    assertNotEquals(instruction, null);
    assertNotEquals(instruction, new ImportInstruction("id", "symbol"));
    assertNotEquals(instruction, new AssignInstruction("other", "symbol", "value"));
    assertNotEquals(instruction, new AssignInstruction("id", "other", "value"));
    assertNotEquals(instruction, new AssignInstruction("id", "symbol", "other"));
    assertEquals("{id='id', instruction='assign', symbolName='symbol', value='value'}", instruction.toString());
  }

  @Test
  public void importMatchesOnIdAndPath() {
    ImportInstruction instruction = new ImportInstruction("id", "agnesse.fixtures");

    assertEquals(instruction, new ImportInstruction("id", "agnesse.fixtures"));
    assertEquals(instruction.hashCode(), new ImportInstruction("id", "agnesse.fixtures").hashCode());
    assertNotEquals(instruction, new ImportInstruction("id", "other"));
    assertEquals("{id='id', instruction='import', path='agnesse.fixtures'}", instruction.toString());
  }

  @Test
  public void makeMatchesOnNameClassAndArguments() {
    MakeInstruction instruction = new MakeInstruction("id", "actor", "com.Example", new Object[] {"a", null});
    MakeInstruction same = new MakeInstruction("id", "actor", "com.Example", new Object[] {"a", null});

    assertEquals(instruction, same);
    assertEquals(instruction.hashCode(), same.hashCode());
    assertNotEquals(instruction, new MakeInstruction("id", "actor", "com.Example", new Object[] {"b"}));
    assertNotEquals(instruction, new MakeInstruction("id", "other", "com.Example", new Object[] {"a", null}));
    assertNotEquals(instruction, new MakeInstruction("id", "actor", "com.Other", new Object[] {"a", null}));
    assertEquals(
      "{id='id', instruction='make', instanceName='actor', className='com.Example', args=[a, null]}",
      instruction.toString());
    assertEquals(
      "{id='id', instruction='make', instanceName='actor', className='com.Example', args=null}",
      new MakeInstruction("id", "actor", "com.Example", null).toString());
  }

  @Test
  public void callMatchesWhetherOrNotItAssigns() {
    CallInstruction call = new CallInstruction("id", "actor", "go", new Object[] {"x"});
    CallAndAssignInstruction assign = new CallAndAssignInstruction("id", "symbol", "actor", "go", new Object[] {"x"});

    assertEquals(call, new CallInstruction("id", "actor", "go", new Object[] {"x"}));
    assertNotEquals(call, new CallInstruction("id", "actor", "go", new Object[] {"y"}));
    assertNotEquals(call, new CallInstruction("id", "actor", "stop", new Object[] {"x"}));
    CallInstruction noArgs = new CallInstruction("id", "actor", "go", null);
    assertEquals(noArgs, new CallInstruction("id", "actor", "go", null));
    assertNotEquals(call, noArgs);
    assertNotEquals(call, assign);
    assertEquals(assign, new CallAndAssignInstruction("id", "symbol", "actor", "go", new Object[] {"x"}));
    assertNotEquals(assign, new CallAndAssignInstruction("id", "other", "actor", "go", new Object[] {"x"}));
    assertEquals("{id='id', instruction='call', instanceName='actor', methodName='go', args=[x]}", call.toString());
    assertEquals(
      "{id='id', instruction='callAndAssign', symbolName='symbol', instanceName='actor', methodName='go', args=[x]}",
      assign.toString());
  }

  @Test
  public void anInstructionWithOnlyAnIdMatchesAnotherWithThatId() {
    Instruction named = new Instruction("same") {
      @Override
      protected InstructionResult executeInternal(InstructionExecutor executor) {
        return new InstructionResult.Void(getId());
      }
    };
    Instruction also = new Instruction("same") {
      @Override
      protected InstructionResult executeInternal(InstructionExecutor executor) {
        return new InstructionResult.Void(getId());
      }
    };

    assertEquals(named, also);
    assertEquals(named.hashCode(), also.hashCode());
    assertNotEquals(named, new Instruction("other") {
      @Override
      protected InstructionResult executeInternal(InstructionExecutor executor) {
        return new InstructionResult.Void(getId());
      }
    });
    assertFalse(named.equals("same"));
    assertFalse(named.equals(Instruction.NOOP_INSTRUCTION));
    assertEquals("Instruction{id='NOOP'}", Instruction.NOOP_INSTRUCTION.toString());
  }

  private static int hashed(String id, String symbol, String value) {
    int result = id.hashCode();
    result = 31 * result + symbol.hashCode();
    result = 31 * result + value.hashCode();
    return result;
  }
}
