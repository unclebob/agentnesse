package fitnesse.markdown;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class MarkdownRenderer {
  private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

  private MarkdownRenderer() {
  }

  public static String render(String markdown) {
    return render(markdown, word -> false, List.of());
  }

  public static String render(String markdown, Predicate<String> pageExists) {
    return render(markdown, pageExists, List.of());
  }

  public static String render(String markdown, Predicate<String> pageExists, List<ContentsLink> children) {
    return render(markdown, pageExists, children, null, null);
  }

  public static String render(String markdown, Function<String, String> pageTarget, List<ContentsLink> children,
                              Function<String, String> includedHtml) {
    return render(markdown, word -> pageTarget.apply(word) != null, children, includedHtml, pageTarget);
  }

  private static String render(String markdown, Predicate<String> pageExists, List<ContentsLink> children,
                              Function<String, String> includedHtml, Function<String, String> pageTarget) {
    Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    Set<String> headingIds = new HashSet<>();
    HtmlRenderer html = HtmlRenderer.builder()
      .nodeRendererFactory(SlimTableRenderer::new)
      .extensions(EXTENSIONS)
      .nodeRendererFactory(context -> new WikiWordNodeRenderer(context,
        pageTarget != null ? pageTarget : word -> pageExists.test(word) ? word : null))
      .nodeRendererFactory(context -> new HeadingRenderer(context, headingIds))
      .nodeRendererFactory(context -> new AgentNesseBlockRenderer(context, children, includedHtml))
      .build();
    return html.render(parser.parse(markdown == null ? "" : markdown));
  }
}
