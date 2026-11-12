package fitnesse.markdown;

import fitnesse.html.HtmlTag;
import fitnesse.wikitext.parser.TextMaker;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Set;
import java.util.function.Predicate;

final class WikiWordNodeRenderer implements NodeRenderer {
  private final HtmlWriter writer;
  private final Predicate<String> pageExists;

  WikiWordNodeRenderer(HtmlNodeRendererContext context, Predicate<String> pageExists) {
    this.writer = context.getWriter();
    this.pageExists = pageExists;
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
      int length = TextMaker.findWikiWordLength(literal.substring(index));
      if (length > 0) {
        writer.raw(link(literal.substring(index, index + length)));
        index += length;
      } else {
        int next = index + 1;
        while (next < literal.length() && TextMaker.findWikiWordLength(literal.substring(next)) == 0) {
          next++;
        }
        writer.text(literal.substring(index, next));
        index = next;
      }
    }
  }

  private String link(String word) {
    if (pageExists.test(word)) {
      HtmlTag link = new HtmlTag("a", word);
      link.addAttribute("href", word);
      return link.htmlInline();
    }
    HtmlTag link = new HtmlTag("a", "[?]");
    link.addAttribute("title", "create page");
    link.addAttribute("href", word + "?edit&amp;nonExistent=true");
    return word + link.htmlInline();
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
