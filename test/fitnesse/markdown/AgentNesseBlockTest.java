package fitnesse.markdown;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AgentNesseBlockTest {

  @Test
  public void theFirstWordIsTheCommand() {
    AgentNesseBlock block = AgentNesseBlock.parse("define MAX\n3\n");
    assertEquals("define", block.command());
    assertEquals("MAX\n3", block.argument());
  }

  @Test
  public void anUnknownCommandNamesItself() {
    try {
      AgentNesseBlock.parse("nope\n");
      fail("expected an unknown command");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("nope"));
    }
  }
}
