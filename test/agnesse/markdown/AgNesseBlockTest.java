package agnesse.markdown;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AgNesseBlockTest {

  @Test
  public void theFirstWordIsTheCommand() {
    AgNesseBlock block = AgNesseBlock.parse("define MAX\n3\n");
    assertEquals("define", block.command());
    assertEquals("MAX\n3", block.argument());
  }

  @Test
  public void anUnknownCommandNamesItself() {
    try {
      AgNesseBlock.parse("nope\n");
      fail("expected an unknown command");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("nope"));
    }
  }
}
