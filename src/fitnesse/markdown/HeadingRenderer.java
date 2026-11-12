package fitnesse.markdown;

import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class HeadingRenderer implements NodeRenderer {
  private final HtmlNodeRendererContext context;
  private final HtmlWriter writer;
  private final Set<String> usedIds;

  HeadingRenderer(HtmlNodeRendererContext context, Set<String> usedIds) {
    this.context = context;
    this.writer = context.getWriter();
    this.usedIds = usedIds;
  }

  @Override
  public Set<Class<? extends Node>> getNodeTypes() {
    return Set.of(Heading.class);
  }

  @Override
  public void render(Node node) {
    Heading heading = (Heading) node;
    String tag = "h" + heading.getLevel();
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("id", anchor(PlainText.of(heading)));
    writer.line();
    writer.tag(tag, attributes);
    renderChildren(heading);
    writer.tag("/" + tag);
    writer.line();
  }

  private void renderChildren(Heading heading) {
    Node child = heading.getFirstChild();
    while (child != null) {
      Node next = child.getNext();
      context.render(child);
      child = next;
    }
  }

  private String anchor(String text) {
    String slug = text.toLowerCase(Locale.ROOT)
      .replaceAll("[^\\p{L}\\p{N} -]", "")
      .replace(" ", "-");
    String id = slug;
    int suffix = 1;
    while (!usedIds.add(id)) {
      id = slug + "-" + suffix;
      suffix++;
    }
    return id;
  }
}
