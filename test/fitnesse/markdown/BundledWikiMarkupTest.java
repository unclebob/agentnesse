package fitnesse.markdown;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class BundledWikiMarkupTest {
  private static final Pattern WIKI_LINE = Pattern.compile(
    "\\s*!(note|path|define|c|meta|include|contents|style|lastmodified|\\||-|`|[1-6])\\b.*");

  @Test
  public void bundledPagesDoNotStartALineWithWikiMarkupOutsideCode() throws IOException {
    List<String> hits = new ArrayList<>();
    try (Stream<Path> files = Files.walk(Path.of("FitNesseRoot"))) {
      files.filter(path -> path.toString().endsWith(".md")).forEach(path -> collect(path, hits));
    }
    assertEquals(hits.toString(), 0, hits.size());
  }

  private static void collect(Path path, List<String> hits) {
    boolean fence = false;
    int lineNumber = 0;
    try {
      for (String line : Files.readAllLines(path)) {
        lineNumber++;
        if (line.trim().startsWith("```")) {
          fence = !fence;
          continue;
        }
        if (!fence && WIKI_LINE.matcher(line).matches()) {
          hits.add(path + ":" + lineNumber + ": " + line.trim());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
