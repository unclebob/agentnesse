package fitnesse.wiki;

/** The page portion of a Markdown link, before a query or anchor. */
public final class WikiPaths {
  private WikiPaths() {
  }

  public static int suffixAt(String destination) {
    return firstOf(destination, '#', '?');
  }

  public static int firstOf(String text, char left, char right) {
    int a = text.indexOf(left);
    int b = text.indexOf(right);
    if (a < 0) {
      return b;
    }
    return b < 0 ? a : Math.min(a, b);
  }

  public static boolean isPage(String path) {
    if (path.isEmpty() || remote(path)) {
      return false;
    }
    char first = path.charAt(0);
    return "<>^.".indexOf(first) >= 0 || WikiWords.findWikiWordLength(path) == path.length();
  }

  private static boolean remote(String path) {
    return path.contains("://") || path.indexOf(' ') >= 0 || path.indexOf('/') >= 0;
  }
}
