package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Recursive descent for the v1 cell language. One spec, no trailing text. */
final class SpecParser {
  private final String text;
  private int at;

  SpecParser(String text) {
    this.text = text;
  }

  Generator parse() {
    Generator generator = spec();
    skip();
    if (at != text.length()) {
      throw error("trailing text");
    }
    return generator;
  }

  private Generator spec() {
    skip();
    if (take("list")) {
      return list();
    }
    if (take("one")) {
      return oneOf();
    }
    if (take("boolean")) {
      return new BoolGen();
    }
    if (take("string")) {
      return string();
    }
    if (take("int")) {
      return integerSpec();
    }
    throw error("expected int, boolean, string, list of, or one of");
  }

  private Generator integerSpec() {
    IntFilter filter = IntFilter.ANY;
    if (filterNext()) {
      filter = readFilter();
    }
    Span span = takeSymbol(":") ? readSpan() : Span.INTS;
    return checked(span, filter);
  }

  private IntGen checked(Span span, IntFilter filter) {
    try {
      return new IntGen(span, filter);
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private boolean filterNext() {
    skip();
    if (at >= text.length()) {
      return false;
    }
    char c = text.charAt(at);
    return c == '!' || c == '>' || c == '<';
  }

  private IntFilter readFilter() {
    return new IntFilter(filterKind(), integer());
  }

  private IntFilter.Kind filterKind() {
    if (takeSymbol("!=")) {
      return IntFilter.Kind.NE;
    }
    if (takeSymbol(">=")) {
      return IntFilter.Kind.GE;
    }
    if (takeSymbol("<=")) {
      return IntFilter.Kind.LE;
    }
    if (takeSymbol(">")) {
      return IntFilter.Kind.GT;
    }
    if (takeSymbol("<")) {
      return IntFilter.Kind.LT;
    }
    throw error("expected a filter");
  }

  private Generator string() {
    if (takeSymbol(":")) {
      if (!take("ascii")) {
        throw error("expected ascii");
      }
    }
    Span length = take("length") ? readSpan() : Span.TEXT;
    return new StringGen(length);
  }

  private Generator list() {
    if (!take("of")) {
      throw error("expected of");
    }
    Generator element = spec();
    Span size = take("size") ? readSpan() : Span.LISTS;
    return new ListGen(element, size);
  }

  private Generator oneOf() {
    if (!take("of")) {
      throw error("expected of");
    }
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    List<String> choices = new ArrayList<>();
    choices.add(choice());
    while (takeSymbol(",")) {
      choices.add(choice());
    }
    requireUnique(choices);
    return new OneOfGen(choices);
  }

  private String choice() {
    skip();
    int start = at;
    while (at < text.length() && text.charAt(at) != ',' && !Character.isWhitespace(text.charAt(at))) {
      at++;
    }
    if (start == at) {
      throw error("empty choice");
    }
    return text.substring(start, at);
  }

  private void requireUnique(List<String> choices) {
    Set<String> seen = new HashSet<>();
    for (String choice : choices) {
      if (!seen.add(choice)) {
        throw error("duplicate choice " + choice);
      }
    }
  }

  private Span readSpan() {
    int min = integer();
    if (!takeSymbol("..")) {
      throw error("expected ..");
    }
    try {
      return new Span(min, integer());
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private int integer() {
    skip();
    boolean negative = takeSymbol("-");
    if (at >= text.length() || !Character.isDigit(text.charAt(at))) {
      throw error("expected an integer");
    }
    int start = at;
    while (at < text.length() && Character.isDigit(text.charAt(at))) {
      at++;
    }
    return bounded(negative, text.substring(start, at));
  }

  private int bounded(boolean negative, String digits) {
    try {
      long wide = Long.parseLong(digits);
      if (negative) {
        wide = -wide;
      }
      if (wide < Integer.MIN_VALUE || wide > Integer.MAX_VALUE) {
        throw error("integer out of range");
      }
      return (int) wide;
    } catch (NumberFormatException bad) {
      throw error("expected an integer");
    }
  }

  private boolean take(String word) {
    skip();
    if (!text.startsWith(word, at)) {
      return false;
    }
    int after = at + word.length();
    if (after < text.length() && Character.isLetterOrDigit(text.charAt(after))) {
      return false;
    }
    at = after;
    return true;
  }

  private boolean takeSymbol(String symbol) {
    skip();
    if (!text.startsWith(symbol, at)) {
      return false;
    }
    at += symbol.length();
    return true;
  }

  private void skip() {
    while (at < text.length() && (text.charAt(at) == ' ' || text.charAt(at) == '\t')) {
      at++;
    }
  }

  private IllegalArgumentException error(String reason) {
    return new IllegalArgumentException(reason + " in \"" + text + "\"");
  }
}
