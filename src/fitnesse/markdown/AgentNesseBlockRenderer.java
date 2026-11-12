package fitnesse.markdown;

import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.List;
import java.util.Set;

final class AgentNesseBlockRenderer implements NodeRenderer {
  private final HtmlWriter writer;
  private final List<ContentsLink> children;

  AgentNesseBlockRenderer(HtmlNodeRendererContext context, List<ContentsLink> children) {
    this.writer = context.getWriter();
    this.children = children;
  }

  @Override
  public Set<Class<? extends Node>> getNodeTypes() {
    return Set.of(FencedCodeBlock.class);
  }

  @Override
  public void render(Node node) {
    FencedCodeBlock code = (FencedCodeBlock) node;
    if (!"agentnesse".equals(code.getInfo())) {
      renderCode(code);
      return;
    }
    try {
      AgentNesseBlock block = AgentNesseBlock.parse(code.getLiteral());
      writer.raw("contents".equals(block.command())
        ? AgentNesseBlock.contentsHtml(children)
        : block.html());
    } catch (IllegalArgumentException e) {
      writer.raw("<div class=\"agentnesse-error\">" + e.getMessage() + "</div>\n");
    }
  }

  private void renderCode(FencedCodeBlock code) {
    writer.line();
    writer.tag("pre");
    writer.tag("code");
    writer.text(code.getLiteral());
    writer.tag("/code");
    writer.tag("/pre");
    writer.line();
  }
}
