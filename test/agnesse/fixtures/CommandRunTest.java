package agnesse.fixtures;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandRunTest {

  @Test
  public void aFailingPageSetsTheExitCodeAndARerunSkipsThePass() throws Exception {
    CommandRun run = new CommandRun();
    run.page("CliMixed", "suite", "");
    run.page("CliMixed.CliPass", "test", table("hello", "hello"));
    run.page("CliMixed.CliFail", "test", table("hello", "goodbye"));

    assertEquals(0, run.exitFor("CliMixed.CliPass?test"));
    assertTrue(run.outputContains("\"schemaVersion\": 1"));
    assertTrue(run.outputContains("\"status\": \"pass\""));

    assertEquals(1, run.exitFor("CliMixed?suite"));
    assertEquals(1, run.rerunFailures("CliMixed?suite"));
    assertFalse(run.output(), run.outputContains("CliPass"));
    assertTrue(run.outputContains("CliFail"));
  }

  private static String table(String sent, String expected) {
    return """
      | script | agnesse.fixtures.EchoFixture |
      | --- | --- | --- |
      | check | echo | %s | %s |
      """.formatted(sent, expected);
  }
}
