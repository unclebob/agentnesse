package fitnesse.wiki.refactoring;

import fitnesse.wiki.NoPruningStrategy;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiWordReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Every page path a Markdown page points at. */
public final class ReferenceIndex {
  private ReferenceIndex() {
  }

  public static List<String> paths(String content) {
    List<String> found = new ArrayList<>();
    MarkdownReferences.replace(null, content, reference -> {
      found.add(reference);
      return Optional.empty();
    });
    return found;
  }

  public static List<String> dangling(WikiPage root) {
    List<String> dangling = new ArrayList<>();
    root.getPageCrawler().traverse(page -> note(page, dangling), new NoPruningStrategy());
    return dangling;
  }

  private static void note(WikiPage page, List<String> dangling) {
    for (String path : paths(page.getData().getContent())) {
      if (!resolves(page, path)) {
        dangling.add(page.getFullPath() + " -> " + path);
      }
    }
  }

  private static boolean resolves(WikiPage page, String path) {
    try {
      return new WikiWordReference(page, path).getReferencedPage() != null;
    } catch (RuntimeException ignored) {
      return false;
    }
  }
}
