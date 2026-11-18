package agnesse.markdown;

import agnesse.wiki.WikiPaths;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Renders a Markdown link to a wiki page as the page's full path, keeping a query or anchor. */
final class PageLinkRenderer implements NodeRenderer {
  private final HtmlNodeRendererContext context;
  private final HtmlWriter writer;
  private final Function<String, String> pageTarget;

  PageLinkRenderer(HtmlNodeRendererContext context, Function<String, String> pageTarget) {
    this.context = context;
    this.writer = context.getWriter();
    this.pageTarget = pageTarget;
  }

  @Override public Set<Class<? extends Node>> getNodeTypes() { return Set.of(Link.class); }

  @Override
  public void render(Node node) {
    Link link = (Link) node;
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("href", href(link.getDestination()));
    writer.tag("a", attributes);
    NodeChildren.render(link, context);
    writer.tag("/a");
  }

  private String href(String destination) {
    if (destination == null || pageTarget == null) {
      return destination == null ? "" : destination;
    }
    int cut = WikiPaths.suffixAt(destination);
    String path = cut < 0 ? destination : destination.substring(0, cut);
    if (!WikiPaths.isPage(path)) {
      return destination;
    }
    String target = pageTarget.apply(path);
    if (target == null) {
      return destination;
    }
    return target + destination.substring(path.length());
  }
}
