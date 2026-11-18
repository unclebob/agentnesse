package agnesse.html;

/**
 * A collapsible section. The disclosure marker is the only toggle.
 * The title links to an included page when that page has a path.
 */
public final class CollapsibleSection {
  private CollapsibleSection() {
  }

  public static String html(String title, String pagePath, String body, boolean open, boolean include) {
    String classes = include ? "agnesse-section agnesse-include" : "agnesse-section";
    return "<details class=\"" + classes + "\"" + (open ? " open" : "") + ">"
      + summary(title, pagePath)
      + (body == null ? "" : body)
      + "</details>\n";
  }

  private static String summary(String title, String pagePath) {
    return "<summary><span class=\"disclosure\" aria-hidden=\"true\">&gt;</span>"
      + name(title, pagePath) + "</summary>";
  }

  private static String name(String title, String pagePath) {
    String text = escape(title);
    if (pagePath == null || pagePath.isBlank()) {
      return text;
    }
    return "<a href=\"" + escape(pagePath) + "\">" + text + "</a>";
  }

  private static String escape(String value) {
    return HtmlUtil.escapeHTML(value == null ? "" : value);
  }
}
