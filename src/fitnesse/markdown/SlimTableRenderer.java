package fitnesse.markdown;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Map;
import java.util.Set;

final class SlimTableRenderer implements NodeRenderer {
  private final HtmlNodeRendererContext context;
  private final HtmlWriter writer;

  SlimTableRenderer(HtmlNodeRendererContext context) {
    this.context = context;
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
    writer.tag("table", Map.of("class", "agentnesse-table " + table.type()));
    renderChildren(node);
    writer.tag("/table");
    writer.line();
  }

  private void renderChildren(Node node) {
    Node child = node.getFirstChild();
    while (child != null) {
      Node next = child.getNext();
      context.render(child);
      child = next;
    }
  }
}
