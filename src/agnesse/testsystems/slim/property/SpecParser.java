package agnesse.testsystems.slim.property;

import java.math.BigDecimal;
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
    if (take("pattern")) {
      return pattern();
    }
    if (take("decimal")) {
      return decimal(false);
    }
    if (take("money")) {
      return decimal(true);
    }
    if (take("int")) {
      return integerSpec();
    }
    throw error("expected int, boolean, string, list of, one of, decimal, money, or pattern");
  }

  private Generator pattern() {
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    List<PatternGen.Piece> pieces = new ArrayList<>();
    skip();
    if (at >= text.length()) {
      throw error("expected a pattern");
    }
    while (at < text.length() && !sizeAhead()) {
      skip();
      if (at >= text.length() || sizeAhead()) {
        break;
      }
      pieces.add(patternPiece());
    }
    try {
      return new PatternGen(pieces);
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  /** {@code size} followed by a number belongs to the list that wraps this pattern. */
  private boolean sizeAhead() {
    int mark = at;
    boolean found = take("size") && numberAhead();
    at = mark;
    return found;
  }

  private boolean numberAhead() {
    skip();
    int mark = at;
    if (at < text.length() && text.charAt(at) == '-') {
      at++;
    }
    boolean digits = at < text.length() && Character.isDigit(text.charAt(at));
    at = mark;
    return digits;
  }

  private PatternGen.Piece patternPiece() {
    if (text.charAt(at) == '[') {
      return characterClass();
    }
    int start = at;
    while (at < text.length() && text.charAt(at) != '[' && !Character.isWhitespace(text.charAt(at))) {
      at++;
    }
    if (start == at) {
      throw error("expected a pattern");
    }
    return PatternGen.Piece.literal(text.substring(start, at));
  }

  private PatternGen.Piece characterClass() {
    int close = text.indexOf(']', at);
    if (close < 0) {
      throw error("expected ]");
    }
    String name = text.substring(at, close + 1);
    at = close + 1;
    if (!takeSymbol("{")) {
      throw error("expected {");
    }
    int count = integer();
    if (!takeSymbol("}")) {
      throw error("expected }");
    }
    return PatternGen.Piece.clazz(name, count);
  }

  private Generator decimal(boolean money) {
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    BigDecimal low = fixed();
    if (!takeSymbol("..")) {
      throw error("expected ..");
    }
    BigDecimal high = fixed();
    int scale = money ? 2 : scale();
    try {
      return new DecimalGen(low, high, scale);
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private int scale() {
    if (!take("scale")) {
      throw error("expected scale");
    }
    return integer();
  }

  private BigDecimal fixed() {
    skip();
    int start = at;
    if (at < text.length() && text.charAt(at) == '-') {
      at++;
    }
    if (readDigits() == 0) {
      throw error("expected a decimal");
    }
    fraction();
    try {
      return new BigDecimal(text.substring(start, at));
    } catch (NumberFormatException bad) {
      throw error("expected a decimal");
    }
  }

  private void fraction() {
    if (at >= text.length() || text.charAt(at) != '.') {
      return;
    }
    if (at + 1 < text.length() && text.charAt(at + 1) == '.') {
      return;
    }
    at++;
    if (readDigits() == 0) {
      throw error("expected a decimal");
    }
  }

  private int readDigits() {
    int count = 0;
    while (at < text.length() && Character.isDigit(text.charAt(at))) {
      at++;
      count++;
    }
    return count;
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
    boolean adversarial = false;
    if (takeSymbol(":")) {
      if (take("adversarial")) {
        adversarial = true;
      } else if (!take("ascii")) {
        throw error("expected ascii or adversarial");
      }
    }
    Span length = take("length") ? readSpan() : Span.TEXT;
    return new StringGen(length, adversarial);
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
    List<Integer> weights = new ArrayList<>();
    choices.add(choice());
    boolean weighted = weightNext();
    if (weighted) {
      weights.add(weight());
    }
    while (takeSymbol(",")) {
      choices.add(choice());
      if (weightNext() != weighted) {
        throw error("weight");
      }
      if (weighted) {
        weights.add(weight());
      }
    }
    requireUnique(choices);
    return weighted ? new OneOfGen(choices, weights) : new OneOfGen(choices);
  }

  private boolean weightNext() {
    skip();
    return at < text.length() && Character.isDigit(text.charAt(at));
  }

  private int weight() {
    int value = integer();
    if (value < 1) {
      throw error("weight must be at least 1");
    }
    return value;
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
