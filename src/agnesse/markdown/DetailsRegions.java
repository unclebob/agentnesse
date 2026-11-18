package agnesse.markdown;

import java.util.function.Function;

/**
 * Standard HTML {@code <details>} / {@code <summary>} regions. CommonMark
 * treats a raw details element as an opaque HTML block, so the body is
 * rendered as Markdown and stitched back in.
 */
final class DetailsRegions {
  private DetailsRegions() {
  }

  static String render(String markdown, Function<String, String> chunk) {
    StringBuilder html = new StringBuilder();
    int cursor = 0;
    while (cursor < markdown.length()) {
      int start = nextDetails(markdown, cursor);
      if (start < 0) {
        html.append(chunk.apply(markdown.substring(cursor)));
        break;
      }
      html.append(chunk.apply(markdown.substring(cursor, start)));
      int end = endOfDetails(markdown, start);
      html.append(one(markdown.substring(start, end), chunk));
      cursor = end;
    }
    return html.toString();
  }

  private static int nextDetails(String markdown, int from) {
    int at = from;
    int found;
    while ((found = markdown.indexOf("<details", at)) >= 0) {
      if (atLineStart(markdown, found)) {
        return found;
      }
      at = found + 1;
    }
    return -1;
  }

  private static boolean atLineStart(String markdown, int index) {
    return index == 0 || markdown.charAt(index - 1) == '\n';
  }

  private static int endOfDetails(String markdown, int start) {
    int depth = 0;
    int at = start;
    while (at != markdown.length()) {
      int open = markdown.indexOf("<details", at);
      int close = markdown.indexOf("</details>", at);
      if (close == -1) {
        return markdown.length();
      }
      if (open != -1 && Math.min(open, close) == open && atLineStart(markdown, open)) {
        depth++;
        at = open + "<details".length();
      } else {
        depth--;
        at = close + "</details>".length();
        if (depth == 0) {
          return skipNewline(markdown, at);
        }
      }
    }
    return markdown.length();
  }

  private static int skipNewline(String markdown, int at) {
    if (at < markdown.length() && markdown.charAt(at) == '\n') {
      return at + 1;
    }
    return at;
  }

  private static String one(String block, Function<String, String> chunk) {
    boolean open = headerIsOpen(block);
    String title = summary(block);
    String body = body(block);
    String inner = render(body, chunk);
    return agnesse.html.CollapsibleSection.html(title, null, inner, open, false);
  }

  private static boolean headerIsOpen(String block) {
    int end = block.indexOf('>');
    if (end == -1) {
      return false;
    }
    return block.substring(0, end).matches("(?i)<details\\b[^>]*\\bopen\\b[^>]*");
  }

  private static String summary(String block) {
    int start = indexOfIgnoreCase(block, "<summary>");
    if (start == -1) {
      return "";
    }
    int end = indexOfIgnoreCase(block, "</summary>");
    try {
      return block.substring(start + "<summary>".length(), end).trim();
    } catch (StringIndexOutOfBoundsException ignored) {
      return "";
    }
  }

  private static String body(String block) {
    int summary = indexOfIgnoreCase(block, "</summary>");
    int from = summary == -1 ? afterHeader(block) : afterSummary(block, summary);
    return block.substring(from, closeAt(block));
  }

  private static int afterHeader(String block) {
    int header = block.indexOf('\n');
    if (header == -1) {
      return block.length();
    }
    return header + 1;
  }

  private static int afterSummary(String block, int summary) {
    int from = summary + "</summary>".length();
    if (from < block.length() && block.charAt(from) == '\n') {
      return from + 1;
    }
    return from;
  }

  private static int closeAt(String block) {
    int close = lastIndexOfIgnoreCase(block, "</details>");
    return close == -1 ? block.length() : close;
  }

  private static int indexOfIgnoreCase(String text, String token) {
    return text.toLowerCase().indexOf(token.toLowerCase());
  }

  private static int lastIndexOfIgnoreCase(String text, String token) {
    return text.toLowerCase().lastIndexOf(token.toLowerCase());
  }
}
