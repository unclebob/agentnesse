package agnesse.slim;

import agnesse.slim.instructions.InstructionExecutor;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class LanesTest {
  @Test
  public void aLaneRowCarriesItsTagItsIdAndTheStampOrder() {
    @SuppressWarnings("unchecked")
    List<List<?>> rows = (List<List<?>>) Lanes.run(executor(), List.of(List.of("t1", List.of(
        List.of("a", "call", "seat", "hello"),
        List.of("b", "call", "seat", "hello")))));
    assertEquals(List.of("t1", "a", "1", "2", "world"), rows.get(0));
    assertEquals("3", rows.get(1).get(2));
    assertEquals(List.of(), Lanes.run(executor(), List.of("not-a-group", List.of("only"), List.of("t2", "calls"))));
  }

  private static InstructionExecutor executor() {
    return new InstructionExecutor() {
      @Override public void assign(String name, Object value) { }
      @Override public void addPath(String path) { }
      @Override public void create(String instanceName, String className, Object... constructorArgs) { }
      @Override public Object callAndAssign(String symbolName, String instanceName, String methodName, Object... arguments) {
        return null;
      }
      @Override public Object call(String instanceName, String methodName, Object... arguments) {
        return "world";
      }
    };
  }
}
