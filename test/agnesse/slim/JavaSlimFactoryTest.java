package agnesse.slim;

import agnesse.slim.fixtureInteraction.DefaultInteraction;
import org.junit.Test;

import static agnesse.slim.JavaSlimFactory.*;
import static org.junit.Assert.assertTrue;

public class JavaSlimFactoryTest {

  @Test
  public void slimFactoryWithNoStatementTimeout() {
    SlimFactory factory = createJavaSlimFactory(createInteraction(null), null, false);
    StatementExecutorInterface executor = factory.getStatementExecutor();
    assertTrue(executor instanceof StatementExecutor);
  }

  @Test
  public void slimFactoryWithStatementTimeout() {
    SlimFactory factory = createJavaSlimFactory(optionsWithStatementTimeout());
    StatementExecutorInterface executor = factory.getStatementExecutor();
    assertTrue(executor instanceof StatementTimeoutExecutor);
  }

  @Test
  public void wholeSecondTimeoutKeepsTheVerboseFlagAndTheTimeoutExecutor() {
    SlimFactory factory = createJavaSlimFactory(createInteraction(null), 2, true);
    assertTrue(factory.isVerbose());
    assertTrue(factory.getStatementExecutor() instanceof StatementTimeoutExecutor);
  }

  private SlimService.Options optionsWithStatementTimeout() {
    return new SlimService.Options(false, 8099, new DefaultInteraction(), 1000);
  }

}
