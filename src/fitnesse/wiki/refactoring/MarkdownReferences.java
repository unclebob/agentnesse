package fitnesse.wiki.refactoring;

import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiWords;

import java.util.Optional;
import java.util.function.Function;

final class MarkdownReferences {
  private MarkdownReferences() {
  }

  static String replace(WikiPage page, String content, Function<String, Optional<String>> changeReference) {
    StringBuilder out = new StringBuilder();
    int index = 0;
    while (index < content.length()) {
      if (startsWith(content, index, "```")) {
        index = copyFence(content, index, out, changeReference);
      } else if (startsWith(content, index, "<!--")) {
        index = copyThrough(content, index, "-->", out);
      } else if (content.charAt(index) == '`') {
        index = copyThrough(content, index, "`", out);
      } else {
        int length = WikiWords.findWikiWordLength(content.substring(index));
        if (length > 0 && !insideUrl(content, index)) {
          String word = content.substring(index, index + length);
          out.append(changeReference.apply(word).orElse(word));
          index += length;
        } else {
          out.append(content.charAt(index));
          index++;
        }
      }
    }
    return out.toString();
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
    if (newline < 0 || !inner.substring(0, newline).trim().equals("agentnesse")) {
      return false;
    }
    String body = inner.substring(newline + 1).stripLeading();
    return body.startsWith("include ") || body.startsWith("include\n") || body.equals("include");
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
