package agnesse.markdown;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SlimTableHeadingTest {

  @Test
  public void aPrefixSelectsTheTableType() {
    SlimTableHeading heading = SlimTableHeading.parse("script:Login");
    assertEquals("script", heading.type());
    assertEquals("Login", heading.name());
  }

  @Test
  public void aBareNameIsADecisionTable() {
    SlimTableHeading heading = SlimTableHeading.parse("Division");
    assertEquals("decision", heading.type());
    assertEquals("Division", heading.name());
  }

  @Test
  public void theSlimAliasesShareOneClass() {
    assertEquals("decision", SlimTableHeading.parse("dt:Division").type());
    assertEquals("query", SlimTableHeading.parse("query:Employees").type());
    assertEquals("ordered-query", SlimTableHeading.parse("ordered query:Employees").type());
    assertEquals("import", SlimTableHeading.parse("import").type());
    assertEquals("library", SlimTableHeading.parse("library").type());
    assertEquals("scenario", SlimTableHeading.parse("scenario:Pay").type());
    assertEquals("property", SlimTableHeading.parse("property:ListReversal").type());
    assertEquals("ListReversal", SlimTableHeading.parse("property:ListReversal").name());
    assertEquals("sequence", SlimTableHeading.parse("sequence:ItemLife").type());
    assertEquals("ItemLife", SlimTableHeading.parse("sequence:ItemLife").name());
  }

  @Test
  public void aCommentIsNotATable() {
    assertFalse(SlimTableHeading.comment(null));
    assertFalse(SlimTableHeading.comment("commentary"));
    assertFalse(SlimTableHeading.comment(":comment"));
    assertTrue(SlimTableHeading.comment("comment"));
    assertTrue(SlimTableHeading.comment("Comment"));
    assertTrue(SlimTableHeading.comment("comment: hidden"));
    assertTrue(SlimTableHeading.comment(" comment : hidden"));
  }

  @Test
  public void aLeadingColonIsAnUnknownType() {
    SlimTableHeading heading = SlimTableHeading.parse(":Division");
    assertEquals("error", heading.type());
    assertEquals("unknown table type \"\"", heading.error());
  }
}
