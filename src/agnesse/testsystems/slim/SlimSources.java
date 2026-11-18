package agnesse.testsystems.slim;

import agnesse.markdown.AgNesseBlock;
import agnesse.markdown.MarkdownFences;
import agnesse.markdown.MarkdownPage;
import agnesse.testrunner.WikiTestPage;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Slim documents of one test, each parsed on its own. An unclosed fence
 * in one document is an error on that document. It does not hide the tables
 * of the next document, so TearDown still runs.
 */
public final class SlimSources {
  public static final class Item {
    final MarkdownTable table;
    final String alias;
    final String target;
    final String error;

    private Item(MarkdownTable table, String alias, String target, String error) {
      this.table = table;
      this.alias = alias;
      this.target = target;
      this.error = error;
    }

    static Item table(MarkdownTable table) {
      return new Item(table, null, null, null);
    }

    static Item alias(String alias, String target) {
      return new Item(null, alias, target, null);
    }

    static Item error(String error) {
      return new Item(null, null, null, error);
    }

    public String alias() {
      return alias;
    }

    public String target() {
      return target;
    }
  }

  private final List<Item> items = new ArrayList<>();

  private SlimSources() {
  }

  public static SlimSources collect(WikiTestPage test) {
    SlimSources sources = new SlimSources();
    WikiPage context = test.getSourcePage();
    Set<String> seen = new HashSet<>();
    for (WikiPage library : test.getScenarioLibraries()) {
      sources.addPage(library, context, false, seen);
    }
    sources.addPage(test.getSetUp(), context, false, seen);
    sources.addPage(context, context, false, seen);
    sources.addPage(test.getTearDown(), context, true, seen);
    return sources;
  }

  public List<Item> items() {
    return items;
  }

  public List<MarkdownTable> tables() {
    return items.stream().map(item -> item.table).filter(table -> table != null).toList();
  }

  public List<String> errors() {
    return items.stream().map(item -> item.error).filter(error -> error != null).toList();
  }

  private void addPage(WikiPage page, WikiPage context, boolean tearDown, Set<String> seen) {
    if (!(page instanceof MarkdownPage markdown)) {
      return;
    }
    String id = PathParser.render(page.getFullPath());
    if (!seen.add(id)) {
      return;
    }
    try {
      read(markdown, context, tearDown, seen);
    } finally {
      seen.remove(id);
    }
  }

  private void read(MarkdownPage page, WikiPage context, boolean tearDown, Set<String> seen) {
    StringBuilder buffer = new StringBuilder();
    StringBuilder fenceBody = new StringBuilder();
    String info = "";
    MarkdownFences fence = new MarkdownFences();
    for (String line : page.expandedForSlim(context).split("\n")) {
      boolean wasInside = fence.inside();
      if (fence.boundary(line)) {
        if (!wasInside) {
          flush(buffer, tearDown, page.getName(), PathParser.render(page.getFullPath()));
          info = fenceInfo(line);
          fenceBody = new StringBuilder();
        } else {
          includeOrSkip(page, info, fenceBody.toString(), context, tearDown, seen);
        }
        continue;
      }
      if (fence.inside()) {
        fenceBody.append(line).append('\n');
      } else {
        buffer.append(line).append('\n');
      }
    }
    flush(buffer, tearDown, page.getName(), PathParser.render(page.getFullPath()));
    if (fence.inside()) {
      items.add(Item.error("Unclosed code fence on " + page.getName()));
    }
  }

  private void includeOrSkip(MarkdownPage page, String info, String body, WikiPage context,
                              boolean tearDown, Set<String> seen) {
    if (!info.startsWith("agnesse")) {
      return;
    }
    try {
      AgNesseBlock block = AgNesseBlock.parse(body);
      if ("alias".equals(block.command())) {
        rememberAlias(block.argument());
        return;
      }
      if ("include".equals(block.command()) || "scenarios".equals(block.command())) {
        include(page, block.argument(), context, tearDown, seen);
      }
    } catch (IllegalArgumentException ignored) {
      // An unknown command is not a document of tables.
    }
  }

  private void include(MarkdownPage page, String argument, WikiPage context, boolean tearDown, Set<String> seen) {
    WikiPagePath path = PathParser.parse(argument.trim().split("\\s+")[0]);
    if (path == null) {
      return;
    }
    WikiPage included = page.getPageCrawler().getSiblingPage(path);
    if (included instanceof MarkdownPage markdown) {
      addPage(markdown, context, tearDown, seen);
    }
  }

  private void rememberAlias(String argument) {
    String[] parts = argument.split("\n", 2);
    if (parts.length < 2) {
      return;
    }
    String alias = parts[0].trim();
    String target = parts[1].trim();
    if (alias.isEmpty() || target.isEmpty()) {
      return;
    }
    items.add(Item.alias(alias, target));
  }

  private void flush(StringBuilder buffer, boolean tearDown, String origin, String originPath) {
    for (MarkdownTable table : new MarkdownTableScanner(buffer.toString())) {
      table.setTearDown(tearDown);
      table.setOrigin(origin);
      table.setOriginPath(originPath);
      items.add(Item.table(table));
    }
    buffer.setLength(0);
  }

  private static String fenceInfo(String line) {
    return line.trim().replaceFirst("^`+", "").trim();
  }
}
