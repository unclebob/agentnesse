package agnesse.testsystems.slim;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResultSectionsTest {
  @Test
  public void aPassingSectionStaysCollapsed() {
    String html = ResultSections.details("SetUp", "<table></table>", false);
    assertTrue(html.contains("<details class=\"agentnesse-include\">"));
    assertFalse(html.contains(" open"));
    assertTrue(html.contains("<summary>SetUp</summary>"));
  }

  @Test
  public void aFailingSectionOpens() {
    assertTrue(ResultSections.shouldOpen(new JSONArray().put(block("fail", "no"))));
    String html = ResultSections.details("TearDown", "<td class=\"fail\">no</td>", true);
    assertTrue(html.contains("<details class=\"agentnesse-include\" open>"));
  }

  @Test
  public void anErrorSectionOpens() {
    assertTrue(ResultSections.shouldOpen(new JSONArray().put(block("error", "missing"))));
  }

  @Test
  public void htmlUsesTheCellStatus() {
    String html = ResultHtml.block(block("pass", "exception"));
    assertTrue(html.contains("class=\"pass\""));
    assertTrue(html.contains(">exception<"));
    assertFalse(html.contains("class=\"fail\""));
    assertFalse(html.contains("class=\"error\""));
  }

  @Test
  public void aPassingSectionThatMentionsExceptionStaysClosed() {
    JSONObject block = block("pass", "this page explains an exception");
    assertFalse(ResultSections.shouldOpen(new JSONArray().put(block)));
    String html = ResultSections.details("SetUp", "this page explains an exception", false);
    assertFalse(html.contains(" open"));
  }

  @Test
  public void setupAndIncludesAreSectionsAndTheTestPageIsNot() {
    assertEquals("SetUp", ResultSections.section("SetUp", "TestChild", "import"));
    assertEquals("Included", ResultSections.section("Included", "TestChild", "script"));
    assertEquals("import", ResultSections.section("TestChild", "TestChild", "import"));
    assertEquals("", ResultSections.section("TestChild", "TestChild", "script"));
  }

  private static JSONObject block(String status, String text) {
    JSONObject cell = new JSONObject()
      .put("row", 1)
      .put("column", 1)
      .put("status", status)
      .put("expected", text)
      .put("actual", JSONObject.NULL)
      .put("symbol", JSONObject.NULL)
      .put("value", JSONObject.NULL)
      .put("error", JSONObject.NULL);
    return new JSONObject()
      .put("name", "note")
      .put("status", status)
      .put("counts", new JSONObject().put("right", 1).put("wrong", 0).put("ignored", 0).put("exceptions", 0))
      .put("rows", new JSONArray().put(new JSONObject().put("cells", new JSONArray().put(cell))));
  }
}
