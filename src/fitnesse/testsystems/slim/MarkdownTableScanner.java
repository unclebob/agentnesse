package fitnesse.testsystems.slim;

import fitnesse.markdown.TableSeparators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The GFM tables in a page, in order. A table is a run of lines that start
 * with {@code |}.
 */
public final class MarkdownTableScanner implements TableScanner<MarkdownTable> {
  private final List<MarkdownTable> tables = new ArrayList<>();

  public MarkdownTableScanner(String markdown) {
    String prepared = TableSeparators.supply(markdown == null ? "" : markdown);
    StringBuilder current = new StringBuilder();
    boolean inTable = false;
    for (String line : prepared.split("\n", -1)) {
      if (isRow(line)) {
        inTable = true;
        current.append(line).append('\n');
      } else if (inTable) {
        add(current);
        inTable = false;
      }
    }
    if (inTable) {
      add(current);
    }
  }

  private static boolean isRow(String line) {
    String trimmed = line.trim();
    return trimmed.startsWith("|") || trimmed.startsWith("!|");
  }

  private void add(StringBuilder current) {
    tables.add(MarkdownTable.parse(current.toString()));
    current.setLength(0);
  }

  @Override
  public int getTableCount() {
    return tables.size();
  }

  @Override
  public MarkdownTable getTable(int index) {
    return tables.get(index);
  }

  @Override
  public Iterator<MarkdownTable> iterator() {
    return tables.iterator();
  }

  public String toHtml() {
    StringBuilder html = new StringBuilder();
    for (MarkdownTable table : tables) {
      html.append(table.toHtml());
    }
    return html.toString();
  }
}
