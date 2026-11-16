package agnesse.wiki.search;

import agnesse.components.TraversalListener;
import agnesse.markdown.FrontMatter;
import agnesse.wiki.NoPruningStrategy;
import agnesse.wiki.WikiPage;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Matches pages by the JSON front matter: type, tags, help, and prune. */
public class FrontMatterFinder implements PageFinder {
  private final TraversalListener<WikiPage> observer;
  private final String type;
  private final List<String> tags;
  private final String help;
  private final Boolean prune;

  public FrontMatterFinder(TraversalListener<WikiPage> observer, String type, String tags, String help, String prune) {
    this.observer = observer;
    this.type = blank(type) ? null : type;
    this.tags = blank(tags) ? List.of() : Arrays.stream(tags.split(",")).map(String::trim).filter(tag -> !tag.isEmpty()).toList();
    this.help = blank(help) ? null : help;
    this.prune = blank(prune) ? null : Boolean.valueOf(prune);
  }

  @Override
  public void search(WikiPage page) {
    if (type == null && tags.isEmpty() && help == null && prune == null) {
      return;
    }
    page.getPageCrawler().traverse(this::consider, new NoPruningStrategy());
  }

  private void consider(WikiPage page) {
    if (matches(FrontMatter.read(page.getData().getContent()))) {
      observer.process(page);
    }
  }

  private boolean matches(FrontMatter matter) {
    return typeMatches(matter) && helpMatches(matter) && pruneMatches(matter) && tagsMatch(matter);
  }

  private boolean typeMatches(FrontMatter matter) {
    return type == null || type.equals(matter.type());
  }

  private boolean helpMatches(FrontMatter matter) {
    if (help == null) {
      return true;
    }
    String text = matter.help();
    return text != null && text.toLowerCase(Locale.ROOT).contains(help.toLowerCase(Locale.ROOT));
  }

  private boolean pruneMatches(FrontMatter matter) {
    return prune == null || matter.prune() == prune;
  }

  private boolean tagsMatch(FrontMatter matter) {
    for (String tag : tags) {
      if (!matter.tags().contains(tag)) {
        return false;
      }
    }
    return true;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
