package fitnesse.markdown;

import org.commonmark.node.HardLineBreak;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Node;
import org.commonmark.node.Text;

final class PlainText {
  private PlainText() {
  }

  static String of(Node node) {
    StringBuilder text = new StringBuilder();
    collect(node, text);
    return text.toString();
  }

  private static boolean breakTag(Node node) {
    if (!(node instanceof HtmlInline)) {
      return false;
    }
    String html = ((HtmlInline) node).getLiteral();
    return html != null && html.matches("(?i)<br\\s*/?>");
  }

  private static void collect(Node node, StringBuilder text) {
    if (node instanceof Text) {
      text.append(((Text) node).getLiteral());
    } else if (node instanceof HardLineBreak || breakTag(node)) {
      text.append('\n');
    }
    Node child = node.getFirstChild();
    while (child != null) {
      collect(child, text);
      child = child.getNext();
    }
  }
}
