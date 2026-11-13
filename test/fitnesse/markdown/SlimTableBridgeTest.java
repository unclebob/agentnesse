package fitnesse.markdown;

import fitnesse.testsystems.slim.HtmlTableScanner;
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

    HtmlTableScanner scanner = new HtmlTableScanner(html);
    assertEquals(1, scanner.getTableCount());
    assertEquals("decision:Division", scanner.getTable(0).getCellContents(0, 0));
    assertEquals(1, scanner.getTable(0).getColumnCountInRow(0));
    assertEquals("numerator", scanner.getTable(0).getCellContents(0, 1));
    assertEquals("10", scanner.getTable(0).getCellContents(0, 2));
    assertEquals(3, scanner.getTable(0).getColumnCountInRow(1));
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
    assertEquals("import", new HtmlTableScanner(html).getTable(0).getCellContents(0, 0));
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
    HtmlTableScanner scanner = new HtmlTableScanner(html);
    assertEquals(1, scanner.getTable(0).getColumnCountInRow(0));
    assertEquals(4, scanner.getTable(0).getColumnCountInRow(2));
    assertEquals("no", scanner.getTable(0).getCellContents(3, 2));
  }

  @Test
  public void aCellKeepsAPipeAndANewlineThatThePageUnderTestNeeds() {
    String html = MarkdownRenderer.render("""
      | script | with content |
      | --- | --- |
      | given page | line one<br>\\| import \\| |
      """);

    HtmlTableScanner scanner = new HtmlTableScanner(html);
    assertEquals("line one\n| import |", scanner.getTable(0).getCellContents(1, 1));
  }

  @Test
  public void emptyCellsAtTheEndOfARowAreNotSlimCells() {
    String html = MarkdownRenderer.render("""
      | script | echo fixture | | |
      | --- | --- | --- | --- |
      | check | echo | Hello | |
      """);
    HtmlTableScanner scanner = new HtmlTableScanner(html);
    assertEquals(2, scanner.getTable(0).getColumnCountInRow(0));
    assertEquals(3, scanner.getTable(0).getColumnCountInRow(1));
    assertEquals("Hello", scanner.getTable(0).getCellContents(2, 1));
  }

  @Test
  public void aCellKeepsABackslash() {
    String html = MarkdownRenderer.render(
      "| decision:X | |\n| --- | --- |\n| pattern | (?s)^([^\\\\n]\\*)\\\\n.\\* |\n");
    assertEquals("(?s)^([^\\n]*)\\n.*", new HtmlTableScanner(html).getTable(0).getCellContents(1, 1));
  }

  @Test
  public void anEscapedDollarStaysADollarForTheNextPage() {
    String html = MarkdownRenderer.render("| script | |\n| --- | --- |\n| check | \\${PAGE_NAME} |\n");
    assertEquals("${PAGE_NAME}", new HtmlTableScanner(html).getTable(0).getCellContents(1, 1));
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

    HtmlTableScanner scanner = new HtmlTableScanner(html);
    assertEquals("text", scanner.getTable(0).getCellContents(0, 0));
    assertEquals("WikiWord", scanner.getTable(0).getCellContents(1, 0));
    assertEquals("bold", scanner.getTable(0).getCellContents(2, 0));
    assertEquals("code", scanner.getTable(0).getCellContents(3, 0));
  }

  @Test
  public void anEscapedUnderscoreIsAnUnderscore() {
    String html = MarkdownRenderer.render("| decision:X | |\n| --- | --- |\n| name | TEST\\_SYSTEM |\n");
    assertEquals("TEST_SYSTEM", new HtmlTableScanner(html).getTable(0).getCellContents(1, 1));
  }
}
