package fitnesse.markdown;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * A GFM table read as a FitNesse Slim table. The header is row 1: its first
 * cell is {@code type:Name} and the rest are constructor arguments. The first
 * body row is row 2.
 */
public final class SlimMarkdownTable {
  private final List<List<String>> rows;
  private final String type;
  private final String name;
  private final String error;

  private SlimMarkdownTable(List<List<String>> rows, String type, String name, String error) {
    this.rows = rows;
    this.type = type;
    this.name = name;
    this.error = error;
  }

  public static SlimMarkdownTable parse(String markdown) {
    Parser parser = Parser.builder().extensions(List.of(TablesExtension.create())).build();
    return from(firstTable(parser.parse(markdown == null ? "" : markdown)));
  }

  public static SlimMarkdownTable from(TableBlock table) {
    List<List<String>> rows = new ArrayList<>();
    if (table != null) {
      collect(table, rows);
    }
    SlimTableHeading heading = SlimTableHeading.parse(rows.isEmpty() ? "" : rows.get(0).get(0));
    return new SlimMarkdownTable(rows, heading.type(), heading.name(), heading.error());
  }

  public String type() {
    return type;
  }

  public String name() {
    return name;
  }

  public String error() {
    return error;
  }

  public List<String> row(int fitNesseRow) {
    return rows.get(fitNesseRow - 1);
  }

  public int rowCount() {
    return rows.size();
  }

  public List<String> constructorArguments() {
    List<String> header = rows.get(0);
    return header.subList(1, header.size());
  }

  private static TableBlock firstTable(Node node) {
    if (node instanceof TableBlock) {
      return (TableBlock) node;
    }
    Node child = node.getFirstChild();
    while (child != null) {
      TableBlock found = firstTable(child);
      if (found != null) {
        return found;
      }
      child = child.getNext();
    }
    return null;
  }

  private static void collect(TableBlock table, List<List<String>> rows) {
    Node section = table.getFirstChild();
    while (section != null) {
      if (section instanceof TableHead || section instanceof TableBody) {
        collectRows(section, rows);
      }
      section = section.getNext();
    }
  }

  private static void collectRows(Node section, List<List<String>> rows) {
    Node row = section.getFirstChild();
    while (row != null) {
      if (row instanceof TableRow) {
        rows.add(cells((TableRow) row));
      }
      row = row.getNext();
    }
  }

  private static List<String> cells(TableRow row) {
    List<String> cells = new ArrayList<>();
    Node cell = row.getFirstChild();
    while (cell != null) {
      if (cell instanceof TableCell) {
        cells.add(PlainText.of(cell).trim());
      }
      cell = cell.getNext();
    }
    return List.copyOf(cells);
  }
}
