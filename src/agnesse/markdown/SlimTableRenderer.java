package agnesse.markdown;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class SlimTableRenderer implements NodeRenderer {
  private final HtmlNodeRendererContext context;
  private final HtmlWriter writer;
  private final boolean testPage;
  private final RenderState state;

  SlimTableRenderer(HtmlNodeRendererContext context, boolean testPage, RenderState state) {
    this.context = context;
    this.writer = context.getWriter();
    this.testPage = testPage;
    this.state = state;
  }

  @Override
  public Set<Class<? extends Node>> getNodeTypes() {
    return Set.of(TableBlock.class);
  }

  @Override
  public void render(Node node) {
    TableBlock block = (TableBlock) node;
    SlimMarkdownTable table = SlimMarkdownTable.from(block);
    String first = table.rowCount() == 0 || table.row(1).isEmpty() ? "" : table.row(1).get(0);
    writer.line();
    if (!SlimTables.slim(testPage, first)) {
      writePlain(block);
      return;
    }
    if (table.error() != null) {
      writer.raw("<table class=\"agnesse-table error\">\n<tr>\n<td><span class=\"error\">");
      writer.text(table.error());
      writer.raw("</span></td>\n</tr>\n</table>\n");
      return;
    }
    writeRows(block, table.type());
  }

  private void writePlain(TableBlock block) {
    writer.raw("<table>\n");
    Node section = block.getFirstChild();
    while (section != null) {
      if (section instanceof TableHead || section instanceof TableBody) {
        writePlainSection(section, section instanceof TableHead ? "th" : "td");
      }
      section = section.getNext();
    }
    writer.raw("</table>\n");
  }

  private void writePlainSection(Node section, String cellTag) {
    Node row = section.getFirstChild();
    while (row != null) {
      if (row instanceof TableRow) {
        writer.raw("<tr>");
        Node cell = row.getFirstChild();
        while (cell != null) {
          if (cell instanceof TableCell) {
            writer.raw("<" + cellTag + ">");
            renderChildren(cell);
            writer.raw("</" + cellTag + ">");
          }
          cell = cell.getNext();
        }
        writer.raw("</tr>\n");
      }
      row = row.getNext();
    }
  }

  private void writeRows(TableBlock block, String type) {
    writer.raw("<table class=\"agnesse-table " + type + "\">\n");
    Node section = block.getFirstChild();
    while (section != null) {
      if (section instanceof TableHead || section instanceof TableBody) {
        writeSection(section);
      }
      section = section.getNext();
    }
    writer.raw("</table>\n");
  }

  private void writeSection(Node section) {
    Node row = section.getFirstChild();
    while (row != null) {
      if (row instanceof TableRow) {
        writeRow((TableRow) row);
      }
      row = row.getNext();
    }
  }

  private void writeRow(TableRow row) {
    List<TableCell> cells = withoutTrailingEmpty(cells(row));
    if (cells.isEmpty()) {
      return;
    }
    writer.raw("<tr>");
    for (TableCell cell : cells) {
      writer.raw("<td data-slim=\"" + attribute(PlainText.of(cell).trim()) + "\">");
      state.literalCells = true;
      renderChildren(cell);
      state.literalCells = false;
      writer.raw("</td>");
    }
    writer.raw("</tr>\n");
  }

  private void renderChildren(Node node) {
    Node child = node.getFirstChild();
    while (child != null) {
      context.render(child);
      child = child.getNext();
    }
  }

  private static List<TableCell> cells(TableRow row) {
    List<TableCell> cells = new ArrayList<>();
    Node cell = row.getFirstChild();
    while (cell != null) {
      if (cell instanceof TableCell) {
        cells.add((TableCell) cell);
      }
      cell = cell.getNext();
    }
    return cells;
  }

  private static List<TableCell> withoutTrailingEmpty(List<TableCell> row) {
    int last = row.size() - 1;
    while (last > 0 && PlainText.of(row.get(last)).trim().isEmpty()) {
      last--;
    }
    return new ArrayList<>(row.subList(0, last + 1));
  }

  private static String attribute(String text) {
    return text.replace("&", "&amp;").replace("\"", "&quot;").replace("\n", "&#10;");
  }
}
