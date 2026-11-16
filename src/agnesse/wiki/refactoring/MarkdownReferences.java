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
    int length = pathLength > 0 ? pathLength : WikiWords.findWikiWordLength(content.substring(index));
    if (length > 0 && !insideUrl(content, index) && WikiWords.startsAtBoundary(content, index)) {
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
    int end = content.indexOf("```", index + 3);
    if (end < 0) {
      out.append(content.substring(index));
      return content.length();
    }
    int close = end + 3;
    String fence = content.substring(index, close);
    out.append(includeFence(fence) ? rewriteInclude(fence, changeReference) : fence);
    return close;
  }

  private static boolean includeFence(String fence) {
    String inner = fence.substring(3, fence.length() - 3);
    int newline = inner.indexOf('\n');
    if (newline < 0 || !inner.substring(0, newline).trim().equals("agnesse")) {
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
    if (labelEnd < 0) {
      return index;
    }
    int destEnd = content.indexOf(')', labelEnd + 2);
    if (destEnd < 0) {
      return index;
    }
    String destination = content.substring(labelEnd + 2, destEnd);
    int cut = WikiPaths.suffixAt(destination);
    String path = cut < 0 ? destination : destination.substring(0, cut);
    String suffix = cut < 0 ? "" : destination.substring(cut);
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
    if (end < 0) {
      out.append(content.substring(start));
      return content.length();
    }
    out.append(content, start, end + closer.length());
    return end + closer.length();
  }

  private static boolean startsWith(String content, int index, String token) {
    return content.startsWith(token, index);
  }
}
