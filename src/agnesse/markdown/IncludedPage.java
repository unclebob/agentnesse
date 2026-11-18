package agnesse.markdown;

/** The body of an included page, and the full path when the page exists. */
final class IncludedPage {
  private final String path;
  private final String html;

  IncludedPage(String path, String html) {
    this.path = path == null ? "" : path;
    this.html = html == null ? "" : html;
  }

  static IncludedPage missing() {
    return new IncludedPage("", "");
  }

  String path() {
    return path;
  }

  String html() {
    return html;
  }
}
