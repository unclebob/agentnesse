package fitnesse.markdown;

import fitnesse.testsystems.slim.HtmlCells;
import fitnesse.testsystems.slim.MarkdownTable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SlimTableBridgeTest {

  @Test
  public void aRenderedTableIsReadByTheSlimScannerAsFitNesseRows() {
    String html = MarkdownRenderer.render("""
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
    String html = MarkdownRenderer.render("""
      <details open>
      <summary>Dependencies</summary>

      | import |
      | --- |
      | fitnesse.slim.test |

      </details>
      """);
    assertTrue(html.contains("<details open>"));
    assertTrue(html.contains("<summary>Dependencies</summary>"));
    assertEquals("import", HtmlCells.cell(html, 0, 0, 0));
  }

  @Test
  public void aClosedDetailsBlockKeepsItsTitleAndBody() {
    String html = MarkdownRenderer.render("""
      <details>
      <summary>Notes</summary>
      plain words
      </details>
      """);
    assertTrue(html.contains("<details><summary>Notes</summary>"));
    assertTrue(html.contains("plain words"));
    assertFalse(html.contains("<details open>"));
  }

  @Test
  public void detailsWithoutASummaryStillRendersTheBody() {
    String html = MarkdownRenderer.render("""
      <details open>
      just the body
      </details>
      """);
    assertTrue(html.contains("<details open>"));
    assertTrue(html.contains("just the body"));
  }

  @Test
  public void anEmptyBodyCellStaysSoAShortRowKeepsItsWidth() {
    String html = MarkdownRenderer.render("""
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
    String html = MarkdownRenderer.render("""
      | script | with content |
      | --- | --- |
      | given page | line one<br>\\| import \\| |
      """);

    assertEquals("line one\n| import |", HtmlCells.cell(html, 0, 1, 1));
  }

  @Test
  public void emptyCellsAtTheEndOfARowAreNotSlimCells() {
    String html = MarkdownRenderer.render("""
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
    String html = MarkdownRenderer.render(
      "| decision:X | |\n| --- | --- |\n| pattern | (?s)^([^\\\\n]\\*)\\\\n.\\* |\n");
    assertEquals("(?s)^([^\\n]*)\\n.*", HtmlCells.cell(html, 0, 1, 1));
  }

  @Test
  public void anEscapedDollarStaysADollarForTheNextPage() {
    String html = MarkdownRenderer.render("| script | |\n| --- | --- |\n| check | \\${PAGE_NAME} |\n");
    assertEquals("${PAGE_NAME}", HtmlCells.cell(html, 0, 1, 1));
  }

  @Test
  public void aQuotedScenarioArgumentStaysAQuotedCell() throws Exception {
    String html = MarkdownRenderer.render("""
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
    String html = MarkdownRenderer.render("""
      | [text](Page) | WikiWord | **bold** | `code` |
      | --- | --- | --- | --- |
      | a | b | c | d |
      """);

    assertTrue(html, html.contains("<a href=\"Page\">text</a>"));
    assertTrue(html, html.contains("href=\"WikiWord?edit&nonExistent=true\""));
    assertTrue(html, html.contains("<strong>bold</strong>"));
    assertTrue(html, html.contains("<code>code</code>"));

    assertEquals("text", HtmlCells.cell(html, 0, 0, 0));
    assertEquals("WikiWord", HtmlCells.cell(html, 0, 0, 1));
    assertEquals("bold", HtmlCells.cell(html, 0, 0, 2));
    assertEquals("code", HtmlCells.cell(html, 0, 0, 3));
  }

  @Test
  public void anEscapedUnderscoreIsAnUnderscore() {
    String html = MarkdownRenderer.render("| decision:X | |\n| --- | --- |\n| name | TEST\\_SYSTEM |\n");
    assertEquals("TEST_SYSTEM", HtmlCells.cell(html, 0, 1, 1));
  }
}
