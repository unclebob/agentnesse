package agnesse.testsystems.slim;

import agnesse.testsystems.TestPage;
import agnesse.testsystems.slim.tables.ScenarioTable;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SlimTestContextScenarioTest {
  @Test
  public void replacingAScenarioDropsTheOldInputsAndKeepsTheNewOnes() {
    SlimTestContextImpl context = new SlimTestContextImpl(mock(TestPage.class));
    ScenarioTable first = scenario("a");
    ScenarioTable second = scenario("a", "b");
    when(first.canMatchParameters("go")).thenReturn(true);

    context.addScenario("s", first);
    context.getScenarioByPattern("go");
    context.addScenario("s", second);

    assertNull(context.getScenarioByPattern("go"));
    when(second.canMatchParameters("go")).thenReturn(true);
    assertSame(second, context.getScenarioByPattern("go"));
  }

  @Test
  public void anEmptyScenarioIsNotKeptForMatching() {
    SlimTestContextImpl context = new SlimTestContextImpl(mock(TestPage.class));
    context.getScenarioByPattern("go");
    context.addScenario("s", scenario());

    assertNull(context.getScenarioByPattern("go"));
  }

  private static ScenarioTable scenario(String... inputs) {
    ScenarioTable table = mock(ScenarioTable.class);
    when(table.getInputs()).thenReturn(Set.of(inputs));
    return table;
  }
}
