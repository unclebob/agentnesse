package agnesse.testsystems.slim;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MarkdownTableScannerTest {
  @Test
  public void anEmptyTableIsNotKept() {
    MarkdownTableScanner scanner = new MarkdownTableScanner("|\n");

    assertEquals(0, scanner.getTableCount());
  }

  @Test
  public void aTableWithACellIsKept() {
    MarkdownTableScanner scanner = new MarkdownTableScanner("|cell|\n");

    assertEquals(1, scanner.getTableCount());
  }
}
