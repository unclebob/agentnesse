package fitnesse.markdown;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.node.Node;
import org.commonmark.renderer.html.AttributeProvider;

import java.util.Map;

final class SlimTableAttributes implements AttributeProvider {
  @Override
  public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
    if (node instanceof TableBlock) {
      String type = SlimTableHeading.parse(firstHeader((TableBlock) node)).type();
      attributes.put("class", "agentnesse-table " + type);
    }
  }

  private static String firstHeader(TableBlock table) {
    Node head = table.getFirstChild();
    if (!(head instanceof TableHead)) {
      return "";
    }
    Node row = head.getFirstChild();
    if (!(row instanceof TableRow)) {
      return "";
    }
    Node cell = row.getFirstChild();
    return cell instanceof TableCell ? PlainText.of(cell) : "";
  }
}
