package fitnesse.markdown;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
  }
}
