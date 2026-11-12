package fitnesse.markdown;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class SlimTableRenderer implements NodeRenderer {
  private final HtmlWriter writer;

  SlimTableRenderer(HtmlNodeRendererContext context) {
    this.writer = context.getWriter();
  }

  @Override
  public Set<Class<? extends Node>> getNodeTypes() {
    return Set.of(TableBlock.class);
  }

  @Override
  public void render(Node node) {
    SlimMarkdownTable table = SlimMarkdownTable.from((TableBlock) node);
    writer.line();
    if (table.error() != null) {
      writer.raw("<table class=\"agentnesse-table error\">\n<tr>\n<td><span class=\"error\">");
      writer.text(table.error());
      writer.raw("</span></td>\n</tr>\n</table>\n");
      return;
    }
    writeRows(table);
  }

  private void writeRows(SlimMarkdownTable table) {
    writer.raw("<table class=\"agentnesse-table " + table.type() + "\">\n");
    for (int row = 1; row <= table.rowCount(); row++) {
      List<String> cells = renderedCells(table.row(row), row == 1);
      if (cells.isEmpty()) {
        continue;
      }
      writer.raw("<tr>");
      for (String cell : cells) {
        writer.raw("<td>");
        writer.text(cellWithNewlines(cell));
        writer.raw("</td>");
      }
      writer.raw("</tr>\n");
    }
    writer.raw("</table>\n");
  }

  private static String cellWithNewlines(String cell) {
    return cell.replace("{{n}}", "\n");
  }

  private static List<String> renderedCells(List<String> row, boolean header) {
    int last = row.size() - 1;
    while (last > 0 && dropped(row.get(last), header)) {
      last--;
    }
    List<String> cells = new ArrayList<>();
    for (int i = 0; i <= last; i++) {
      if (!"{{pad}}".equals(row.get(i))) {
        cells.add(row.get(i));
      }
    }
    return cells;
  }

  private static boolean dropped(String cell, boolean header) {
    return "{{pad}}".equals(cell) || (header && cell.isEmpty());
  }
}
