package fitnesse.markdown;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class MarkdownRenderTest {

  @Test
  public void aHeadingRendersAsHtml() throws Exception {
    assertGolden("heading");
  }

  static void assertGolden(String name) throws Exception {
    Path dir = Path.of("test/fitnesse/markdown/golden");
    String markdown = Files.readString(dir.resolve(name + ".md"));
    String html = Files.readString(dir.resolve(name + ".html"));
    assertEquals(html, MarkdownRenderer.render(markdown));
  }
}
