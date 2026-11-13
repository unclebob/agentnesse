package fitnesse.markdown;

import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

final class AgentNesseBlockRenderer implements NodeRenderer {
  private final HtmlWriter writer;
  private final List<ContentsLink> children;
  private final Function<String, String> includedHtml;

  AgentNesseBlockRenderer(HtmlNodeRendererContext context, List<ContentsLink> children,
                          Function<String, String> includedHtml) {
    this.writer = context.getWriter();
    this.children = children;
    this.includedHtml = includedHtml;
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
      writer.raw(html(block));
    } catch (IllegalArgumentException e) {
      writer.raw("<div class=\"agentnesse-error\">" + e.getMessage() + "</div>\n");
    }
  }

  private String html(AgentNesseBlock block) {
    if ("contents".equals(block.command())) {
      return AgentNesseBlock.contentsHtml(children);
    }
    if ("include".equals(block.command()) && includedHtml != null) {
      return block.includeWith(includedHtml.apply(block.argument()));
    }
    return block.html();
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
