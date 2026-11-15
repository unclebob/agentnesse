package fitnesse.testsystems.slim;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResultSectionsTest {
  @Test
  public void aPassingSectionStaysCollapsed() {
    String html = ResultSections.details("SetUp", "<table class=\"agentnesse-table import\"></table>");
    assertTrue(html.contains("<details class=\"agentnesse-include\">"));
    assertFalse(html.contains(" open"));
    assertTrue(html.contains("<summary>SetUp</summary>"));
  }

  @Test
  public void aFailingSectionOpens() {
    String html = ResultSections.details("TearDown", "<td class=\"fail\">no</td>");
    assertTrue(html.contains("<details class=\"agentnesse-include\" open>"));
  }

  @Test
  public void anErrorSectionOpens() {
    String html = ResultSections.details("import", "<span class=\"error\">missing</span>");
    assertTrue(html.contains(" open"));
  }

  @Test
  public void setupAndIncludesAreSectionsAndTheTestPageIsNot() {
    assertEquals("SetUp", ResultSections.section("SetUp", "TestChild", "import"));
    assertEquals("Included", ResultSections.section("Included", "TestChild", "script"));
    assertEquals("import", ResultSections.section("TestChild", "TestChild", "import"));
    assertEquals("", ResultSections.section("TestChild", "TestChild", "script"));
  }
}
