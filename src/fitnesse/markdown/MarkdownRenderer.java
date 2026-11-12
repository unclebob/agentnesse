package fitnesse.markdown;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;

public final class MarkdownRenderer {
  private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

  private MarkdownRenderer() {
  }

  public static String render(String markdown) {
    Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    HtmlRenderer html = HtmlRenderer.builder().extensions(EXTENSIONS).build();
    return html.render(parser.parse(markdown == null ? "" : markdown));
  }
}
