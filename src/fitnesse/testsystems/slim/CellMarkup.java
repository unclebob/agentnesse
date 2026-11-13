package fitnesse.testsystems.slim;

import fitnesse.html.HtmlUtil;
import fitnesse.testsystems.slim.results.SlimTestResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML for one cell, drawn from the result data. This is the compared-failure
 * formatting that used to live on the HTML table.
 */
public final class CellMarkup {
  private static final String SYMBOL_ASSIGNMENT = "\\$[A-Za-z]\\w*<?->?\\[|\\$`[^`]+`<?->?\\[";
  private static final String SYMBOL_ASSIGNMENT_SUFFIX = "\\]";
  private static final Pattern HTML_PATTERN = Pattern.compile("^(?:" + SYMBOL_ASSIGNMENT + ")?" +
    HtmlUtil.HTML_CELL_CONTENT_PATTERN_TEXT + SYMBOL_ASSIGNMENT_SUFFIX + "?$",
    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern CONTAINS_TABLE_HTML_PATTERN = Pattern.compile(".*(<table.*?>.*</table>).*",
    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern SYMBOL_REPLACEMENT_PATTERN = Pattern.compile("^(" + SYMBOL_ASSIGNMENT + ")(.*)(" +
    SYMBOL_ASSIGNMENT_SUFFIX + ")$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  private CellMarkup() {
  }

  static String format(String original, SlimTestResult result) {
    if (result.getExecutionResult() == null) {
      return result.getMessage() != null ? asHtml(result.getMessage()) : null;
    }
    String message = result.hasMessage() ? asHtml(result.getMessage()) : original;
    return switch (result.getExecutionResult()) {
      case PASS -> formatPass(result, message);
      case FAIL -> formatFail(result, original, message);
      case IGNORE -> formatIgnore(result, original, message);
      case ERROR -> formatError(original, message);
    };
  }

  public static boolean qualifiesAsConvertedList(String text) {
    if (qualifiesAsSymbolReplacement(text)) {
      int contentOffset = text.indexOf('[');
      String symbolContent = text.substring(contentOffset + 1, text.length() - 1);
      return qualifiesAsConvertedList(symbolContent);
    }
    return text.startsWith("[") && text.endsWith("]");
  }

  private static String formatPass(SlimTestResult result, String message) {
    if (!result.hasExpected()) {
      return String.format("<span class=\"pass\">%s</span>", message);
    }
    if (qualifiesAsHtml(result.getActual()) || qualifiesAsHtml(result.getExpected())) {
      return String.format("<span class=\"pass\">%s</span>", asHtml(result.getExpected()));
    }
    String[] expected = parseSymbol(result.getExpected());
    return String.format("<span class=\"pass\">%s</span>",
      HtmlUtil.escapeHTML(expected[0]) + HtmlUtil.escapeHTML(expected[1]) + HtmlUtil.escapeHTML(expected[2]));
  }

  private static String formatFail(SlimTestResult result, String original, String message) {
    if (result.hasActual() && result.hasExpected()) {
      return formatComparedFailure(result);
    }
    if ((result.hasActual() || result.hasExpected()) && result.hasMessage()) {
      return String.format("[%s] <span class=\"fail\">%s</span>",
        asHtml(result.hasActual() ? result.getActual() : result.getExpected()), message);
    }
    return String.format("<span class=\"fail\">%s</span>", message);
  }

  private static String formatComparedFailure(SlimTestResult result) {
    if (qualifiesAsHtml(result.getActual()) || qualifiesAsHtml(result.getExpected())) {
      return String.format("[%s] <span class=\"fail\">expected [%s]</span>",
        asHtml(result.getActual()), asHtml(result.getExpected()));
    }
    String[] actual = parseSymbol(result.getActual());
    String[] expected = parseSymbol(result.getExpected());
    return String.format("[%s] <span class=\"fail\">expected [%s]</span>",
      HtmlUtil.escapeHTML(actual[0]) + HtmlDiffUtil.buildActual(actual[1], expected[1]) + HtmlUtil.escapeHTML(actual[2]),
      HtmlUtil.escapeHTML(expected[0]) +
        new HtmlDiffUtil.ExpectedBuilder(result.getActual(), expected[1])
          .setOpeningTag("<span class=\"diff\">").setClosingTag("</span>").build() +
        HtmlUtil.escapeHTML(expected[2]));
  }

  private static String formatIgnore(SlimTestResult result, String original, String message) {
    if (result.doesCount()) {
      return String.format("<span class=\"ignore\">%s</span>", message);
    }
    return String.format("%s <span class=\"ignore\">%s</span>", original, message);
  }

  private static String formatError(String original, String message) {
    return String.format("%s <span class=\"error\">%s</span>", original, message);
  }

  private static String asHtml(String text) {
    if (text != null && qualifiesAsHtml(text)) {
      if (qualifiesAsSymbolReplacement(text)) {
        int contentOffset = text.indexOf('[');
        return HtmlUtil.escapeHTML(text.substring(0, contentOffset)) + text.substring(contentOffset);
      }
      return text;
    }
    if (text != null && text.contains("<") && CONTAINS_TABLE_HTML_PATTERN.matcher(text).matches() && qualifiesAsConvertedList(text)) {
      return text;
    }
    return HtmlUtil.escapeHTML(text == null ? "" : text);
  }

  private static boolean qualifiesAsSymbolReplacement(String text) {
    return text.startsWith("$") && SYMBOL_REPLACEMENT_PATTERN.matcher(text).matches();
  }

  static boolean qualifiesAsHtml(String text) {
    return text != null && (text.startsWith("<") || text.startsWith("$")) && HTML_PATTERN.matcher(text).matches();
  }

  private static String[] parseSymbol(String text) {
    Matcher matcher = SYMBOL_REPLACEMENT_PATTERN.matcher(text == null ? "" : text);
    String[] symbols = new String[] {"", "", ""};
    if (matcher.matches()) {
      symbols[0] = matcher.group(1);
      symbols[1] = matcher.group(2);
      symbols[2] = matcher.group(3);
    } else {
      symbols[1] = text == null ? "" : text;
    }
    return symbols;
  }
}
