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

  @Test
  public void aBlankCommandIsRejected() {
    assertRejected(null);
    assertRejected("");
    assertRejected("\ninclude SetUp");
    assertRejected("   \ninclude SetUp");
  }

  @Test
  public void theCommandIsTheFirstWordAndTheRestStaysVerbatim() {
    AgNesseBlock spaced = AgNesseBlock.parse("define  MAX\n3");
    assertEquals("define", spaced.command());
    assertEquals("MAX\n3", spaced.argument());

    AgNesseBlock only = AgNesseBlock.parse("path");
    assertEquals("path", only.command());
    assertEquals("", only.argument());
  }

  @Test
  public void anIncludeNamesThePageAndKeepsTheSectionClosed() {
    AgNesseBlock block = AgNesseBlock.parse("include SetUp");
    assertEquals(""
      + "<details class=\"agnesse-section agnesse-include\">"
      + "<summary><span class=\"disclosure\" aria-hidden=\"true\">&gt;</span>"
      + "<a href=\"Parent.SetUp\">SetUp</a></summary>from-include</details>\n",
      block.includeWith("from-include", "Parent.SetUp"));
  }

  @Test
  public void anUnresolvedIncludeEscapesTheLinesUnderTheName() {
    assertEquals(""
      + "<details class=\"agnesse-section agnesse-include\">"
      + "<summary><span class=\"disclosure\" aria-hidden=\"true\">&gt;</span>SetUp</summary>"
      + "hello &lt;there&gt;</details>\n",
      AgNesseBlock.parse("include SetUp\nhello <there>").html());

    assertEquals(""
      + "<details class=\"agnesse-section agnesse-include\">"
      + "<summary><span class=\"disclosure\" aria-hidden=\"true\">&gt;</span></summary>"
      + "hello</details>\n",
      AgNesseBlock.parse("include\n\nhello").html());
  }

  private static void assertRejected(String body) {
    try {
      AgNesseBlock.parse(body);
      fail("expected a missing command");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("command"));
    }
  }
}
