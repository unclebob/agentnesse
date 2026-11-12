package fitnesse.markdown;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class MarkdownRenderer {
  private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

  private MarkdownRenderer() {
  }

  public static String render(String markdown) {
    return render(markdown, word -> false);
  }

  public static String render(String markdown, Predicate<String> pageExists) {
    Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    Set<String> headingIds = new HashSet<>();
    HtmlRenderer html = HtmlRenderer.builder()
      .nodeRendererFactory(SlimTableRenderer::new)
      .extensions(EXTENSIONS)
      .nodeRendererFactory(context -> new WikiWordNodeRenderer(context, pageExists))
      .nodeRendererFactory(context -> new HeadingRenderer(context, headingIds))
      .nodeRendererFactory(AgentNesseBlockRenderer::new)
      .build();
    return html.render(parser.parse(markdown == null ? "" : markdown));
  }
}
