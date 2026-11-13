package fitnesse.testsystems.slim;

import fitnesse.markdown.AgentNesseBlock;
import fitnesse.markdown.MarkdownFences;
import fitnesse.markdown.MarkdownPage;
import fitnesse.testrunner.TestPageWithSuiteSetUpAndTearDown;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPagePath;

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
    if (test instanceof TestPageWithSuiteSetUpAndTearDown suite) {
      sources.addPages(suite.getSuiteSetUps(), context, false, seen);
    }
    sources.addPages(test.getScenarioLibraries(), context, false, seen);
    sources.addPage(test.getSetUp(), context, false, seen);
    sources.addPage(context, context, false, seen);
    sources.addPage(test.getTearDown(), context, true, seen);
    if (test instanceof TestPageWithSuiteSetUpAndTearDown suite) {
      sources.addPages(suite.getSuiteTearDowns(), context, true, seen);
    }
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

  private void addPages(List<WikiPage> pages, WikiPage context, boolean tearDown, Set<String> seen) {
    if (pages == null) {
      return;
    }
    for (WikiPage page : pages) {
      addPage(page, context, tearDown, seen);
    }
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
    for (String line : page.expandedForSlim(context).split("\n", -1)) {
      boolean wasInside = fence.inside();
      if (fence.boundary(line)) {
        if (!wasInside) {
          flush(buffer, tearDown);
          info = fenceInfo(line);
          fenceBody.setLength(0);
        } else {
          includeOrSkip(page, info, fenceBody.toString(), context, tearDown, seen);
          fenceBody.setLength(0);
        }
        continue;
      }
      if (fence.inside()) {
        fenceBody.append(line).append('\n');
      } else {
        buffer.append(line).append('\n');
      }
    }
    flush(buffer, tearDown);
    if (fence.inside()) {
      items.add(Item.error("Unclosed code fence on " + page.getName()));
    }
  }

  private void includeOrSkip(MarkdownPage page, String info, String body, WikiPage context,
                              boolean tearDown, Set<String> seen) {
    if (!info.startsWith("agentnesse")) {
      return;
    }
    try {
      AgentNesseBlock block = AgentNesseBlock.parse(body);
      if ("alias".equals(block.command())) {
        rememberAlias(block.argument());
        return;
      }
      if ("include".equals(block.command())) {
        include(page, block.argument(), context, tearDown, seen);
      }
    } catch (IllegalArgumentException ignored) {
      // A define or path fence is not a document of tables.
    }
  }

  private void include(MarkdownPage page, String argument, WikiPage context, boolean tearDown, Set<String> seen) {
    String name = argument == null ? "" : argument.trim();
    int split = name.indexOf('\n');
    if (split >= 0) {
      name = name.substring(0, split).trim();
    }
    int space = name.indexOf(' ');
    if (space >= 0) {
      name = name.substring(0, space).trim();
    }
    WikiPagePath path = PathParser.parse(name);
    if (path == null) {
      return;
    }
    WikiPage included = page.getPageCrawler().getSiblingPage(path);
    if (included instanceof MarkdownPage markdown && included != page) {
      addPage(markdown, context, tearDown, seen);
    }
  }

  private void rememberAlias(String argument) {
    if (argument == null) {
      return;
    }
    int split = argument.indexOf('\n');
    if (split < 0) {
      return;
    }
    String alias = argument.substring(0, split).trim();
    String target = argument.substring(split + 1).trim();
    if (!alias.isEmpty() && !target.isEmpty()) {
      items.add(Item.alias(alias, target));
    }
  }

  private void flush(StringBuilder buffer, boolean tearDown) {
    if (buffer.length() == 0) {
      return;
    }
    for (MarkdownTable table : new MarkdownTableScanner(buffer.toString())) {
      table.setTearDown(tearDown);
      items.add(Item.table(table));
    }
    buffer.setLength(0);
  }

  private static String fenceInfo(String line) {
    String trimmed = line.trim();
    int ticks = 0;
    while (ticks < trimmed.length() && trimmed.charAt(ticks) == '`') {
      ticks++;
    }
    return trimmed.substring(ticks).trim();
  }
}
