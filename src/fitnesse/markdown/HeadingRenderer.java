package fitnesse.markdown;

import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
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
    attributes.put("id", anchor(plainText(heading)));
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
    String slug = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    if (slug.isEmpty()) {
      slug = "section";
    }
    String id = slug;
    int suffix = 2;
    while (!usedIds.add(id)) {
      id = slug + "-" + suffix;
      suffix++;
    }
    return id;
  }

  private static String plainText(Node node) {
    StringBuilder text = new StringBuilder();
    collect(node, text);
    return text.toString();
  }

  private static void collect(Node node, StringBuilder text) {
    if (node instanceof Text) {
      text.append(((Text) node).getLiteral());
    }
    Node child = node.getFirstChild();
    while (child != null) {
      collect(child, text);
      child = child.getNext();
    }
  }
}
