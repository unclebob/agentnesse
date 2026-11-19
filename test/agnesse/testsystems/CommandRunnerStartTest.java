package agnesse.testsystems;

import org.junit.Test;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

public class CommandRunnerStartTest {
  @Test
  public void aFinishedCommandIsJoinedWithoutWaiting() throws Exception {
    Logger.getLogger(CommandRunner.class.getName()).setLevel(Level.FINE);
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    CommandRunner runner = new CommandRunner(new String[] {java, "-version"}, null, new CompositeExecutionLogListener());

    runner.asynchronousStart();
    runner.join();

    assertTrue(runner.isDead());
  }

  @Test
  public void anInterruptedWaitStopsAtOnce() throws Exception {
    CommandRunner runner = new CommandRunner(new String[] {"/bin/sleep", "30"}, null, new CompositeExecutionLogListener(), 2);
    runner.asynchronousStart();
    Thread.currentThread().interrupt();

    runner.join();

    assertTrue(Thread.currentThread().isInterrupted());
    runner.kill();
    Thread.interrupted();
  }
}
