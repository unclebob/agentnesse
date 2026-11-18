package agnesse.markdown;

import agnesse.testsystems.slim.HtmlCells;
import agnesse.testsystems.slim.MarkdownTable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SlimTableBridgeTest {

  @Test
  public void aRenderedTableIsReadByTheSlimScannerAsAgNesseRows() {
    String html = MarkdownRenderer.render(true, """
      | decision:Division | | |
      | --- | --- | --- |
      | numerator | denominator | quotient? |
      | 10 | 2 | 5.0 |
      """);

    assertEquals(1, HtmlCells.tables(html));
    assertEquals("decision:Division", HtmlCells.cell(html, 0, 0, 0));
    assertEquals(1, HtmlCells.columns(html, 0, 0));
    assertEquals("numerator", HtmlCells.cell(html, 0, 1, 0));
    assertEquals("10", HtmlCells.cell(html, 0, 2, 0));
    assertEquals(3, HtmlCells.columns(html, 0, 1));
  }

  @Test
  public void aTableInsideDetailsIsStillASlimTable() {
    String html = MarkdownRenderer.render(true, """
      <details open>
      <summary>Dependencies</summary>

      | import |
      | --- |
      | agnesse.slim.test |

      </details>
      """);
    assertTrue(html.contains("<details class=\"agnesse-section\" open>"));
    assertTrue(html.contains("<button type=\"button\" class=\"disclosure\" aria-expanded=\"true\">&gt;</button>Dependencies"));
    assertEquals("import", HtmlCells.cell(html, 0, 0, 0));
  }

  @Test
  public void aDetailsTagInsideALineIsLeftAlone() {
    String html = MarkdownRenderer.render("see <details> later\n");

    assertFalse(html, html.contains("agnesse-section"));
    assertTrue(html, html.contains("<details>"));
  }

  @Test
  public void aDetailsTagWithoutAClosingAngleStaysClosed() {
    String html = MarkdownRenderer.render("<details\nplain words\n");

    assertTrue(html, html.contains("<details class=\"agnesse-section\">"));
    assertFalse(html, html.contains(" open"));
    assertTrue(html, html.contains("plain words"));
  }

  @Test
  public void anUnresolvedIncludeKeepsEscapedTextUnderTheName() {
    String html = MarkdownRenderer.render("""
      ```agnesse
      include SetUp
      hello <there>
      ```
      """);

    assertTrue(html, html.contains("<button type=\"button\" class=\"disclosure\" aria-expanded=\"false\">&gt;</button>SetUp"));
    assertFalse(html, html.contains("<a "));
    assertTrue(html, html.contains("hello &lt;there&gt;"));
  }

  @Test
  public void aDetailsTitleStaysPlainTextBesideTheDisclosureMarker() {
    String html = MarkdownRenderer.render("""
      <details>
      <summary>Notes</summary>
      plain words
      </details>
      """);

    assertTrue(html, html.contains("<button type=\"button\" class=\"disclosure\" aria-expanded=\"false\">&gt;</button>Notes"));
    assertTrue(html, html.contains("plain words"));
    assertFalse(html, html.contains("<a "));
    assertFalse(html, html.contains("<details open>"));
  }

  @Test
  public void aDetailsTitleIsEscapedAndIsNotALink() {
    String html = MarkdownRenderer.render("""
      <details open>
      <summary>A<B & C</summary>
      kept
      </details>
      """);

    assertTrue(html, html.contains("<button type=\"button\" class=\"disclosure\" aria-expanded=\"true\">&gt;</button>A&lt;B &amp; C"));
    assertTrue(html, html.contains("<details class=\"agnesse-section\" open>"));
    assertFalse(html, html.contains("<a "));
    assertTrue(html, html.contains("kept"));
  }

  @Test
  public void aClosedDetailsBlockKeepsItsTitleAndBody() {
    String html = MarkdownRenderer.render(true, """
      <details>
      <summary>Notes</summary>
      plain words
      </details>
      """);
    assertTrue(html.contains("<button type=\"button\" class=\"disclosure\" aria-expanded=\"false\">&gt;</button>Notes"));
    assertTrue(html.contains("plain words"));
    assertFalse(html.contains("<details open>"));
  }

  @Test
  public void detailsWithoutASummaryStillRendersTheBody() {
    String html = MarkdownRenderer.render(true, """
      <details open>
      just the body
      </details>
      """);
    assertTrue(html.contains("<details class=\"agnesse-section\" open>"));
    assertTrue(html.contains("just the body"));
  }

  @Test
  public void anEmptyBodyCellStaysSoAShortRowKeepsItsWidth() {
    String html = MarkdownRenderer.render(true, """
      | baseline:buy milk | | | |
      | --- | --- | --- | --- |
      | cash? | card? | pints? | go? |
      | | | | no |
      """);
    assertEquals(1, HtmlCells.columns(html, 0, 0));
    assertEquals(4, HtmlCells.columns(html, 0, 2));
    assertEquals("no", HtmlCells.cell(html, 0, 2, 3));
  }

  @Test
  public void aCellKeepsAPipeAndANewlineThatThePageUnderTestNeeds() {
    String html = MarkdownRenderer.render(true, """
      | script | with content |
      | --- | --- |
      | given page | line one<br>\\| import \\| |
      """);

    assertEquals("line one\n| import |", HtmlCells.cell(html, 0, 1, 1));
  }

  @Test
  public void emptyCellsAtTheEndOfARowAreNotSlimCells() {
    String html = MarkdownRenderer.render(true, """
      | script | echo fixture | | |
      | --- | --- | --- | --- |
      | check | echo | Hello | |
      """);
    assertEquals(2, HtmlCells.columns(html, 0, 0));
    assertEquals(3, HtmlCells.columns(html, 0, 1));
    assertEquals("Hello", HtmlCells.cell(html, 0, 1, 2));
  }

  @Test
  public void aCellKeepsABackslash() {
    String html = MarkdownRenderer.render(true, 
      "| decision:X | |\n| --- | --- |\n| pattern | (?s)^([^\\\\n]\\*)\\\\n.\\* |\n");
    assertEquals("(?s)^([^\\n]*)\\n.*", HtmlCells.cell(html, 0, 1, 1));
  }

  @Test
  public void anEscapedDollarStaysADollarForTheNextPage() {
    String html = MarkdownRenderer.render(true, "| script | |\n| --- | --- |\n| check | \\${PAGE_NAME} |\n");
    assertEquals("${PAGE_NAME}", HtmlCells.cell(html, 0, 1, 1));
  }

  @Test
  public void aQuotedScenarioArgumentStaysAQuotedCell() throws Exception {
    String html = MarkdownRenderer.render(true, """
      | scenario | page | source | should not contain | text |
      | --- | --- | --- | --- | --- |
      | reject | content contains | @text |
      """);
    MarkdownTable table = MarkdownTable.parse("""
      | scenario | page | source | should not contain | text |
      | reject | content contains | @text |
      """);
    var copy = (MarkdownTable) table.asTemplate(content -> content.replace("@text", "\"brother content\""));
    assertEquals("\"brother content\"", copy.getCellContents(2, 1));
  }

  @Test
  public void inlineMarkdownInACellRendersAndSlimReadsThePlainText() {
    String html = MarkdownRenderer.render(true, """
      | [text](Page) | WikiWord | **bold** | `code` |
      | --- | --- | --- | --- |
      | a | b | c | d |
      """);

    assertTrue(html, html.contains("<a href=\"Page\">text</a>"));
    assertFalse(html, html.contains("WikiWord?edit"));
    assertTrue(html, html.contains("<strong>bold</strong>"));
    assertTrue(html, html.contains("<code>code</code>"));

    assertEquals("text", HtmlCells.cell(html, 0, 0, 0));
    assertEquals("WikiWord", HtmlCells.cell(html, 0, 0, 1));
    assertEquals("bold", HtmlCells.cell(html, 0, 0, 2));
    assertEquals("code", HtmlCells.cell(html, 0, 0, 3));
  }

  @Test
  public void anEscapedUnderscoreIsAnUnderscore() {
    String html = MarkdownRenderer.render(true, "| decision:X | |\n| --- | --- |\n| name | TEST\\_SYSTEM |\n");
    assertEquals("TEST_SYSTEM", HtmlCells.cell(html, 0, 1, 1));
  }
}
