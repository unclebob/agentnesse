package fitnesse.markdown;

import fitnesse.html.HtmlTag;
import fitnesse.wiki.WikiWords;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Set;
import java.util.function.Function;

final class WikiWordNodeRenderer implements NodeRenderer {
  private final HtmlWriter writer;
  private final Function<String, String> pageTarget;

  WikiWordNodeRenderer(HtmlNodeRendererContext context, Function<String, String> pageTarget) {
    this.writer = context.getWriter();
    this.pageTarget = pageTarget;
  }

  @Override
  public Set<Class<? extends Node>> getNodeTypes() {
    return Set.of(Text.class);
  }

  @Override
  public void render(Node node) {
    String literal = ((Text) node).getLiteral();
    if (insideLink(node)) {
      writer.text(literal);
      return;
    }
    write(literal);
  }

  private void write(String literal) {
    int index = 0;
    while (index < literal.length()) {
      int length = WikiWords.findWikiWordLength(literal.substring(index));
      if (length > 0) {
        writer.raw(link(literal.substring(index, index + length)));
        index += length;
      } else {
        int next = index + 1;
        while (next < literal.length() && WikiWords.findWikiWordLength(literal.substring(next)) == 0) {
          next++;
        }
        writer.text(literal.substring(index, next));
        index = next;
      }
    }
  }

  private String link(String word) {
    String target = pageTarget.apply(word);
    if (target != null) {
      HtmlTag link = new HtmlTag("a", word);
      link.addAttribute("href", target);
      return link.htmlInline();
    }
    String shown = word.length() > 1 && "<^>".indexOf(word.charAt(0)) >= 0 ? word.substring(1) : word;
    return shown + "<a title=\"create page\" href=\"" + shown + "?edit&nonExistent=true\">[?]</a>";
  }

  private static boolean insideLink(Node node) {
    for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
      if (parent instanceof Link) {
        return true;
      }
    }
    return false;
  }
}
