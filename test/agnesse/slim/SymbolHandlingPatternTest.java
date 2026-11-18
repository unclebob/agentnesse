package agnesse.slim;

import agnesse.fixtures.EchoFixture;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SymbolHandlingPatternTest {
  @Test
  public void aFixtureCanOptOutOfSymbolReplacement() throws Exception {
    StatementExecutor executor = new StatementExecutor();
    executor.create("echo", EchoFixture.class.getName());
    executor.assign("who", "Ada");
    assertEquals("Ada", executor.call("echo", "echo", "$who"));

    executor.assign(StatementExecutor.SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS, "echo\\.echo");
    assertEquals("$who", executor.call("echo", "echo", "$who"));

    executor.assign("someone", "Grace");
    assertEquals("$who", executor.call("echo", "echo", "$who"));

    executor.assign(StatementExecutor.SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS, null);
    assertEquals("Ada", executor.call("echo", "echo", "$who"));

    executor.assign(StatementExecutor.SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS, "[");
    assertEquals("Ada", executor.call("echo", "echo", "$who"));
  }
}
