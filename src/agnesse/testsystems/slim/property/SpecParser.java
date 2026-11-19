package agnesse.testsystems.slim.property;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Recursive descent for the v1 cell language. One spec, no trailing text. */
final class SpecParser {
  private final String text;
  private final Clock clock;
  private int at;

  SpecParser(String text, Clock clock) {
    this.text = text;
    this.clock = clock;
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
    if (take("all")) {
      return allOf();
    }
    if (take("one")) {
      return oneOf();
    }
    return scalar();
  }

  private Generator scalar() {
    Generator timed = timed();
    if (timed != null) {
      return timed;
    }
    return numbered();
  }

  private Generator timed() {
    if (take("date")) {
      return moment(MomentGen.Kind.DATE);
    }
    if (take("time")) {
      return moment(MomentGen.Kind.TIME);
    }
    if (take("datetime")) {
      return moment(MomentGen.Kind.DATETIME);
    }
    if (take("period")) {
      return period();
    }
    if (take("now")) {
      return nowSpec();
    }
    return null;
  }

  private Generator numbered() {
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
    throw error("expected int, boolean, string, list of, one of, decimal, money, pattern, all of, date, time, datetime, period, or now");
  }

  private Generator moment(MomentGen.Kind kind) {
    ZoneId zone = resolve(optionalZone());
    if (!takeSymbol(":")) {
      return MomentGen.open(kind, zone, clock().now());
    }
    long low = bound(kind, zone);
    if (!takeSymbol("..")) {
      throw error("expected ..");
    }
    return closed(kind, zone, low, bound(kind, zone));
  }

  private Generator closed(MomentGen.Kind kind, ZoneId zone, long low, long high) {
    try {
      return MomentGen.of(kind, zone, low, high, clock().now());
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private long bound(MomentGen.Kind kind, ZoneId zone) {
    try {
      return Moments.bound(kind, boundToken(), zone, clock().now());
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private String boundToken() {
    skip();
    if (take("now")) {
      return "now" + durationTail();
    }
    return rawToken();
  }

  private String durationTail() {
    StringBuilder out = new StringBuilder();
    while (signAhead()) {
      out.append(text.charAt(at));
      at++;
      appendPiece(out);
    }
    return out.toString();
  }

  private boolean signAhead() {
    skip();
    if (at >= text.length()) {
      return false;
    }
    char sign = text.charAt(at);
    return sign == '+' || sign == '-';
  }

  private void appendPiece(StringBuilder out) {
    int mark = at;
    while (at < text.length() && pieceChar(text.charAt(at))) {
      out.append(text.charAt(at));
      at++;
    }
    if (at == mark) {
      throw error("expected a duration");
    }
  }

  private boolean pieceChar(char c) {
    return Character.isDigit(c) || c == 'd' || c == 'h' || c == 'm' || c == 's';
  }

  private String rawToken() {
    skip();
    int start = at;
    while (at < text.length() && !Character.isWhitespace(text.charAt(at)) && !text.startsWith("..", at)) {
      at++;
    }
    if (start == at) {
      throw error("expected a bound");
    }
    return text.substring(start, at);
  }

  private Generator period() {
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    PeriodGen.Unit unit = unit();
    return new PeriodGen(unit, resolve(optionalZone()));
  }

  private PeriodGen.Unit unit() {
    if (take("day")) {
      return PeriodGen.Unit.DAY;
    }
    if (take("week")) {
      return PeriodGen.Unit.WEEK;
    }
    if (take("month")) {
      return PeriodGen.Unit.MONTH;
    }
    throw error("expected day, week, or month");
  }

  private Generator nowSpec() {
    resolve(null);
    return new NowGen(clock());
  }

  private ZoneId optionalZone() {
    if (!take("zone")) {
      return null;
    }
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    try {
      return ZoneId.of(rawToken());
    } catch (DateTimeException e) {
      throw error("zone is not a zone id");
    }
  }

  private ZoneId resolve(ZoneId override) {
    if (override != null) {
      return override;
    }
    if (clock == null || clock.zone() == null) {
      throw error("zone is not configured");
    }
    return clock.zone();
  }

  private Clock clock() {
    if (clock == null) {
      throw error("zone is not configured");
    }
    return clock;
  }

  private Generator allOf() {
    if (!take("of")) {
      throw error("expected of");
    }
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    List<List<String>> groups = new ArrayList<>();
    groups.add(group());
    while (take("x")) {
      groups.add(group());
    }
    try {
      return new AllOfGen(groups);
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private List<String> group() {
    List<String> choices = new ArrayList<>();
    choices.add(choice());
    while (takeSymbol(",")) {
      choices.add(choice());
    }
    return choices;
  }

  private Generator pattern() {
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    List<PatternGen.Piece> pieces = new ArrayList<>();
    while (!sizeAhead()) {
      skip();
      if (at >= text.length()) {
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
    if (close == -1) {
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
    return new BigDecimal(text.substring(start, at));
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
    if (element.domain() != null) {
      throw error("all of is enumerated, not sampled");
    }
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
    return integer();
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
