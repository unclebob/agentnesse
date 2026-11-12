package fitnesse.markdown;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.node.Node;
import org.commonmark.renderer.html.AttributeProvider;

import java.util.Map;

final class SlimTableAttributes implements AttributeProvider {
  @Override
  public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
    if (node instanceof TableBlock) {
      SlimMarkdownTable table = SlimMarkdownTable.from((TableBlock) node);
      attributes.put("class", "agentnesse-table " + table.type());
    }
  }
}
