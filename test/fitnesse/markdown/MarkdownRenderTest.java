package fitnesse.markdown;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MarkdownRenderTest {

  @Test
  public void aHeadingRendersAsHtml() throws Exception {
    assertGolden("heading");
    assertGolden("headings");
    assertGolden("slug");
  }

  @Test
  public void eachAgentNesseCommandHasItsOwnClass() throws Exception {
    for (String command : new String[] {"define", "scenarios", "path", "contents", "alias"}) {
      assertGolden(command);
    }
    assertGolden("include");
    for (String removed : new String[] {"setup", "teardown"}) {
      String html = MarkdownRenderer.render("```agentnesse\n" + removed + "\nx\n```\n");
      assertTrue(html, html.contains("unknown agentnesse command"));
      assertTrue(html, html.contains(removed));
    }
    String bad = MarkdownRenderer.render("```agentnesse\nnope\n```\n");
    assertTrue(bad.contains("agentnesse-error"));
    assertTrue(bad.contains("nope"));
  }

  @Test
  public void aSlimTableKeepsItsNameAndGainsATypeClass() throws Exception {
    assertGolden("decision-table");
    assertGolden("bare-decision");
    assertGolden("script-table");
    assertGolden("query-table");
    assertGolden("unknown-table");
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
