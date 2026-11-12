package fitnesse.markdown;

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
