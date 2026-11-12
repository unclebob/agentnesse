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
      List<String> cells = withoutTrailingEmpty(table.row(row));
      if (cells.isEmpty()) {
        continue;
      }
      writer.raw("<tr>");
      for (String cell : cells) {
        writer.raw("<td>");
        writer.raw(cellHtml(cell));
        writer.raw("</td>");
      }
      writer.raw("</tr>\n");
    }
    writer.raw("</table>\n");
  }

  private String cellHtml(String cell) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < cell.length(); i++) {
      int tag = tagEnd(cell, i);
      if (tag > i) {
        out.append(cell, i, tag);
        i = tag - 1;
        continue;
      }
      char current = cell.charAt(i);
      if (current == '&') {
        out.append("&amp;");
      } else if (current == '<') {
        out.append("&lt;");
      } else if (current == '>') {
        out.append("&gt;");
      } else {
        out.append(current);
      }
    }
    return out.toString();
  }

  private static int tagEnd(String cell, int start) {
    if (cell.charAt(start) != '<') {
      return -1;
    }
    int end = cell.indexOf('>', start);
    if (end < 0) {
      return -1;
    }
    String tag = cell.substring(start + 1, end).toLowerCase();
    if (tag.startsWith("/")) {
      tag = tag.substring(1);
    }
    int nameEnd = 0;
    while (nameEnd < tag.length() && Character.isLetter(tag.charAt(nameEnd))) {
      nameEnd++;
    }
    String name = tag.substring(0, nameEnd);
    if (name.equals("span") || name.equals("a") || name.equals("table") || name.equals("tr") || name.equals("td")
      || name.equals("pre") || name.equals("div")) {
      return end + 1;
    }
    return -1;
  }

  private static List<String> withoutTrailingEmpty(List<String> row) {
    int last = row.size() - 1;
    while (last > 0 && row.get(last).isEmpty()) {
      last--;
    }
    return new ArrayList<>(row.subList(0, last + 1));
  }
}
