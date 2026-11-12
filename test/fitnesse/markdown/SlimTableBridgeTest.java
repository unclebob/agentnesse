package fitnesse.markdown;

import fitnesse.testsystems.slim.HtmlTableScanner;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
  public void aTableInsideACollapsibleIsStillASlimTable() {
    String html = MarkdownRenderer.render("""
      ```agentnesse
      collapsible open Dependencies
      | import |
      | --- |
      | fitnesse.slim.test |
      ```
      """);
    assertTrue(html.contains("<details open>"));
    assertEquals("import", new HtmlTableScanner(html).getTable(0).getCellContents(0, 0));
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
      | given page | line one{{n}}\\| import \\| |
      """);

    HtmlTableScanner scanner = new HtmlTableScanner(html);
    assertEquals("line one\n| import |", scanner.getTable(0).getCellContents(1, 1));
  }

  @Test
  public void aCellKeepsABackslash() {
    String html = MarkdownRenderer.render(
      "| decision:X | |\n| --- | --- |\n| pattern | (?s)^([^\\\\n]\\*)\\\\n.\\* |\n");
    assertEquals("(?s)^([^\\n]*)\\n.*", new HtmlTableScanner(html).getTable(0).getCellContents(1, 1));
  }

  @Test
  public void anEscapedUnderscoreIsAnUnderscore() {
    String html = MarkdownRenderer.render("| decision:X | |\n| --- | --- |\n| name | TEST\\_SYSTEM |\n");
    assertEquals("TEST_SYSTEM", new HtmlTableScanner(html).getTable(0).getCellContents(1, 1));
  }
}
