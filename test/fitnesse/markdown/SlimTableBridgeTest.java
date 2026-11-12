package fitnesse.markdown;

import fitnesse.testsystems.slim.HtmlTableScanner;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    assertEquals("", scanner.getTable(0).getCellContents(1, 0));
    assertEquals("numerator", scanner.getTable(0).getCellContents(0, 1));
    assertEquals("10", scanner.getTable(0).getCellContents(0, 2));
    assertEquals(3, scanner.getTable(0).getColumnCountInRow(0));
  }
}
