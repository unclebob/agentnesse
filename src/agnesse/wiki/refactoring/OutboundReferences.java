package agnesse.wiki.refactoring;

import agnesse.wiki.NoPruningStrategy;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Relative references on a page that is about to move are resolved from its
 * old parent. Absolute paths keep pointing at the same page after the move.
 */
public final class OutboundReferences {
  private OutboundReferences() {
  }

  public static void absolutize(WikiPage subtree) {
    Set<WikiPage> inside = new HashSet<>();
    subtree.getPageCrawler().traverse(inside::add, new NoPruningStrategy());
    for (WikiPage page : Set.copyOf(inside)) {
      rewrite(page, inside);
    }
  }

  private static void rewrite(WikiPage page, Set<WikiPage> inside) {
    String content = page.getData().getContent();
    String updated = MarkdownReferences.replace(page, content, reference -> absolute(page, reference, inside));
    if (!updated.equals(content)) {
      PageData data = page.getData();
      data.setContent(updated);
      page.commit(data);
    }
  }

  private static Optional<String> absolute(WikiPage page, String reference, Set<WikiPage> inside) {
    if (reference.startsWith(".")) {
      return Optional.empty();
    }
    WikiPage target = ReferenceIndex.resolve(page, reference);
    if (target == null || inside.contains(target)) {
      return Optional.empty();
    }
    WikiPagePath path = target.getFullPath();
    path.makeAbsolute();
    return Optional.of(PathParser.render(path));
  }
}
