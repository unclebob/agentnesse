package fitnesse.markdown;

import org.commonmark.node.Node;
import org.commonmark.renderer.html.HtmlNodeRendererContext;

final class NodeChildren {
  private NodeChildren() {
  }

  static void render(Node node, HtmlNodeRendererContext context) {
    Node child = node.getFirstChild();
    while (child != null) {
      Node next = child.getNext();
      context.render(child);
      child = next;
    }
  }
}
