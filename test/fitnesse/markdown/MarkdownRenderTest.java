package fitnesse.markdown;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class MarkdownRenderTest {

  @Test
  public void aHeadingRendersAsHtml() throws Exception {
    assertGolden("heading");
    assertGolden("headings");
  }

  @Test
  public void aWikiWordBecomesALinkAndAMissingPageOffersToCreateIt() throws Exception {
    assertGolden("wiki-word");
    String linked = MarkdownRenderer.render("Open FrontPage.\n", word -> "FrontPage".equals(word));
    assertEquals("<p>Open <a href=\"FrontPage\">FrontPage</a>.</p>\n", linked);
    String code = MarkdownRenderer.render("Use `FrontPage` literally.\n");
    assertEquals("<p>Use <code>FrontPage</code> literally.</p>\n", code);
  }

  static void assertGolden(String name) throws Exception {
    Path dir = Path.of("test/fitnesse/markdown/golden");
    String markdown = Files.readString(dir.resolve(name + ".md"));
    String html = Files.readString(dir.resolve(name + ".html"));
    assertEquals(html, MarkdownRenderer.render(markdown));
  }
}
