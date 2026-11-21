package agnesse.wiki.refactoring;

import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPaths;
import agnesse.wiki.WikiWords;

import java.util.Optional;
import java.util.function.Function;

final class MarkdownReferences {
  private MarkdownReferences() {
  }

  static String replace(WikiPage page, String content, Function<String, Optional<String>> changeReference) {
    StringBuilder out = new StringBuilder();
    int index = 0;
    while (index < content.length()) {
      index = advance(content, index, out, changeReference);
    }
    return out.toString();
  }

  private static int advance(String content, int index, StringBuilder out,
                             Function<String, Optional<String>> changeReference) {
    if (startsWith(content, index, "```")) {
      return copyFence(content, index, out, changeReference);
    }
    if (startsWith(content, index, "<!--")) {
      return copyThrough(content, index, "-->", out);
    }
    if (content.charAt(index) == '`') {
      return copyThrough(content, index, "`", out);
    }
    if (content.charAt(index) == '[') {
      return bracket(content, index, out, changeReference);
    }
    return word(content, index, out, changeReference);
  }

  private static int bracket(String content, int index, StringBuilder out,
                             Function<String, Optional<String>> changeReference) {
    int next = link(content, index, out, changeReference);
    if (next == index) {
      out.append(content.charAt(index));
      return index + 1;
    }
    return next;
  }

  private static int word(String content, int index, StringBuilder out,
                          Function<String, Optional<String>> changeReference) {
    int pathLength = leadingPath(content, index);
    int length = pathLength != 0 ? pathLength : WikiWords.findWikiWordLength(content.substring(index));
    if (length != 0 && !insideUrl(content, index) && WikiWords.startsAtBoundary(content, index)) {
      String token = content.substring(index, index + length);
      out.append(changeReference.apply(token).orElse(token));
      return index + length;
    }
    out.append(content.charAt(index));
    return index + 1;
  }

  private static int leadingPath(String content, int index) {
    if (!pathSigil(content, index)) {
      return 0;
    }
    return scanPath(content, index + 1) - index;
  }

  private static boolean pathSigil(String content, int index) {
    char first = content.charAt(index);
    return "<>^.".indexOf(first) >= 0 && index + 1 < content.length() && Character.isUpperCase(content.charAt(index + 1));
  }

  private static int scanPath(String content, int end) {
    while (end < content.length() && pathChar(content.charAt(end))) {
      end++;
    }
    while (content.charAt(end - 1) == '.') {
      end--;
    }
    return end;
  }

  private static boolean pathChar(char c) {
    return Character.isLetterOrDigit(c) || c == '.';
  }

  private static boolean insideUrl(String content, int index) {
    if (index == 0) {
      return false;
    }
    char previous = content.charAt(index - 1);
    return previous == '/' || previous == ':';
  }

  private static int copyFence(String content, int index, StringBuilder out,
                               Function<String, Optional<String>> changeReference) {
    int opener = tickRun(content, index);
    int close = closingFence(content, index + opener, opener);
    if (missing(close)) {
      out.append(content.substring(index));
      return content.length();
    }
    int end = close + tickRun(content, close);
    String fence = content.substring(index, end);
    out.append(opener == 3 && includeFence(fence) ? rewriteInclude(fence, changeReference) : fence);
    return end;
  }

  private static int tickRun(String content, int index) {
    int cursor = index;
    while (cursor != content.length() && content.charAt(cursor) == '`') {
      cursor++;
    }
    return cursor - index;
  }

  /** A closer is its own line of at least {@code opener} backticks and nothing else. */
  private static int closingFence(String content, int from, int opener) {
    int index = from;
    while (index < content.length()) {
      int lineEnd = content.indexOf('\n', index);
      if (missing(lineEnd)) {
        lineEnd = content.length();
      }
      int lineStart = content.lastIndexOf('\n', index - 1) + 1;
      if (lineStart >= from && closes(content.substring(lineStart, lineEnd), opener)) {
        return lineStart + content.substring(lineStart, lineEnd).indexOf('`');
      }
      if (lineEnd == content.length()) {
        return -1;
      }
      index = lineEnd + 1;
    }
    return -1;
  }

  private static boolean closes(String line, int opener) {
    String trimmed = line.trim();
    if (trimmed.length() < opener) {
      return false;
    }
    int i = 0;
    while (i != trimmed.length()) {
      if (trimmed.charAt(i) != '`') {
        return false;
      }
      i++;
    }
    return true;
  }

  private static boolean includeFence(String fence) {
    String inner = fence.substring(3, fence.length() - 3);
    int newline = inner.indexOf('\n');
    if (missing(newline) || !inner.substring(0, newline).trim().equals("agnesse")) {
      return false;
    }
    String body = inner.substring(newline + 1).stripLeading();
    return command(body, "include") || command(body, "scenarios");
  }

  private static boolean command(String body, String name) {
    return body.equals(name) || body.startsWith(name + " ") || body.startsWith(name + "\n");
  }

  private static int link(String content, int index, StringBuilder out,
                          Function<String, Optional<String>> changeReference) {
    int labelEnd = content.indexOf("](", index);
    if (missing(labelEnd)) {
      return index;
    }
    int destEnd = content.indexOf(')', labelEnd + 2);
    if (missing(destEnd)) {
      return index;
    }
    String destination = content.substring(labelEnd + 2, destEnd);
    int cut = WikiPaths.suffixAt(destination);
    String path = missing(cut) ? destination : destination.substring(0, cut);
    String suffix = missing(cut) ? "" : destination.substring(cut);
    if (!WikiPaths.isPage(path)) {
      return index;
    }
    out.append(content, index, labelEnd + 2);
    out.append(changeReference.apply(path).orElse(path)).append(suffix).append(')');
    return destEnd + 1;
  }

  private static String rewriteInclude(String fence, Function<String, Optional<String>> changeReference) {
    String inner = fence.substring(3, fence.length() - 3);
    int newline = inner.indexOf('\n');
    String body = inner.substring(newline + 1);
    return "```" + inner.substring(0, newline + 1) + replace(null, body, changeReference) + "```";
  }

  private static int copyThrough(String content, int index, String closer, StringBuilder out) {
    int start = index;
    int from = index + (closer.equals("`") ? 1 : closer.length());
    int end = content.indexOf(closer, from);
    if (missing(end)) {
      out.append(content.substring(start));
      return content.length();
    }
    out.append(content, start, end + closer.length());
    return end + closer.length();
  }

  private static boolean startsWith(String content, int index, String token) {
    return content.startsWith(token, index);
  }

  private static boolean missing(int at) {
    return at == -1;
  }
}
