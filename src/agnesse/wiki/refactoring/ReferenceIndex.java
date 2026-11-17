package agnesse.wiki.refactoring;

import agnesse.markdown.SlimTables;
import agnesse.wiki.NoPruningStrategy;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.WikiWordReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  /** source id, reference order, target id. The id is the {@code <!-- id:NAME -->} mark, which rename does not rewrite. */
  public static List<String> targets(WikiPage root) {
    List<String> lines = new ArrayList<>();
    root.getPageCrawler().traverse(page -> collect(page, lines), new NoPruningStrategy());
    Collections.sort(lines);
    return lines;
  }

  private static void collect(WikiPage page, List<String> lines) {
    String source = idOf(page);
    int index = 0;
    for (String path : paths(page.getData().getContent())) {
      WikiPage target = resolve(page, path);
      lines.add(source + "#" + index + "=>" + (target == null ? "MISSING" : idOf(target)));
      index++;
    }
  }

  static String idOf(WikiPage page) {
    if (page.isRoot()) {
      return "root";
    }
    Matcher mark = ID.matcher(page.getData().getContent());
    return mark.find() ? mark.group(1) : page.getFullPath().toString();
  }

  static WikiPage resolve(WikiPage page, String path) {
    try {
      return new WikiWordReference(page, path).getReferencedPage();
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static final Pattern ID = Pattern.compile("<!-- id:(\\S+) -->");

  public static List<String> dangling(WikiPage root) {
    List<String> dangling = new ArrayList<>();
    root.getPageCrawler().traverse(page -> note(page, dangling), new NoPruningStrategy());
    return dangling;
  }

  private static void note(WikiPage page, List<String> dangling) {
    String content = page.getData().getContent();
    if (WikiPageUtil.rendersSlimTables(page)) {
      content = SlimTables.withoutSlimTables(content);
    }
    for (String path : paths(content)) {
      if (!resolves(page, path)) {
        dangling.add(page.getFullPath() + " -> " + path);
      }
    }
  }

  private static boolean resolves(WikiPage page, String path) {
    return resolve(page, path) != null;
  }
}
