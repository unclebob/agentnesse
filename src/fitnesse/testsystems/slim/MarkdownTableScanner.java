package fitnesse.testsystems.slim;

import fitnesse.markdown.MarkdownFences;
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
    boolean tearDown = false;
    MarkdownFences fence = new MarkdownFences();
    for (String line : prepared.split("\n", -1)) {
      if (fence.boundary(line) || fence.inside()) {
        if (inTable) {
          add(current, tearDown);
          inTable = false;
        }
        continue;
      }
      if (line.contains("agentnesse-break") || line.contains("agentnesse-teardown")) {
        if (inTable) {
          add(current, tearDown);
          inTable = false;
        }
        fence = new MarkdownFences();
        if (line.contains("agentnesse-teardown")) {
          tearDown = true;
        }
        continue;
      }
      if (isRow(line)) {
        inTable = true;
        current.append(line).append('\n');
      } else if (inTable) {
        add(current, tearDown);
        inTable = false;
      }
    }
    if (inTable) {
      add(current, tearDown);
    }
  }

  private static boolean isRow(String line) {
    String trimmed = line.trim();
    return trimmed.startsWith("|") || trimmed.startsWith("!|");
  }

  private void add(StringBuilder current, boolean tearDown) {
    MarkdownTable table = MarkdownTable.parse(current.toString());
    if (table.getRowCount() == 0 || table.getColumnCountInRow(0) == 0) {
      current.setLength(0);
      return;
    }
    table.setTearDown(tearDown);
    tables.add(table);
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
