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
  private int records;

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
    if (take("record")) {
      return record();
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
    Generator plain = plain();
    if (plain != null) {
      return plain;
    }
    return compound();
  }

  private Generator plain() {
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
    return null;
  }

  private Generator compound() {
    if (take("int")) {
      return integerSpec();
    }
    if (take("span")) {
      return spanSpec();
    }
    if (take("partition")) {
      return partition();
    }
    if (take("monotonic")) {
      return monotonic();
    }
    throw error("expected int, boolean, string, list of, one of, decimal, money, pattern, all of, date, time, datetime, period, now, span, partition, monotonic, or record");
  }

  private Generator moment(MomentGen.Kind kind) {
    Relation relation = relation();
    ZoneId zone = resolve(optionalZone());
    MomentGen moment = ranged(kind, zone);
    return relation == null ? moment : new Limit(moment, relation);
  }

  private MomentGen ranged(MomentGen.Kind kind, ZoneId zone) {
    if (!takeSymbol(":")) {
      return MomentGen.open(kind, zone, clock().now());
    }
    long low = bound(kind, zone);
    if (!takeSymbol("..")) {
      throw error("expected ..");
    }
    return closed(kind, zone, low, bound(kind, zone));
  }

  private Relation relation() {
    if (take("after")) {
      return new Relation(ident(), true);
    }
    if (take("before")) {
      return new Relation(ident(), false);
    }
    return null;
  }

  private String ident() {
    return name("column");
  }

  private String name(String kind) {
    skip();
    if (at >= text.length() || !Character.isLetter(text.charAt(at))) {
      throw error("expected a " + kind);
    }
    int start = at;
    at++;
    while (at < text.length() && identChar(text.charAt(at))) {
      at++;
    }
    return text.substring(start, at);
  }

  private static boolean identChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }

  private MomentGen closed(MomentGen.Kind kind, ZoneId zone, long low, long high) {
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
    return charAhead("+-");
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
    while (at < text.length() && tokenChar(text.charAt(at)) && !text.startsWith("..", at)) {
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
    Relation relation = relation();
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
      DecimalGen decimal = new DecimalGen(low, high, scale);
      return relation == null ? decimal : new Limit(decimal, relation);
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
    Relation relation = relation();
    IntFilter filter = IntFilter.ANY;
    if (relation == null && filterNext()) {
      filter = readFilter();
    }
    Span span = takeSymbol(":") ? readSpan() : Span.INTS;
    IntGen ints = checked(span, filter);
    return relation == null ? ints : new Limit(ints, relation);
  }

  private IntGen checked(Span span, IntFilter filter) {
    try {
      return new IntGen(span, filter);
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private boolean filterNext() {
    return charAhead("!><");
  }

  private boolean charAhead(String marks) {
    skip();
    if (at >= text.length()) {
      return false;
    }
    return marks.indexOf(text.charAt(at)) >= 0;
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
    Flags flags = flags();
    try {
      return new ListGen(element, size, flags.unique, flags.sorted);
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private Flags flags() {
    boolean unique = false;
    boolean sorted = false;
    while (!(unique && sorted)) {
      if (!unique && take("unique")) {
        unique = true;
      } else if (!sorted && take("sorted")) {
        sorted = true;
      } else {
        break;
      }
    }
    return new Flags(unique, sorted);
  }

  private record Flags(boolean unique, boolean sorted) {
  }

  private Generator spanSpec() {
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    Span starts = readSpan();
    if (!take("length")) {
      throw error("expected length");
    }
    try {
      return new SpanGen(starts, readSpan());
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private Generator partition() {
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    BigDecimal total = fixed();
    if (!take("into")) {
      throw error("expected into");
    }
    try {
      return new PartitionGen(total, integer());
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private Generator monotonic() {
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    Span range = readSpan();
    Span size = take("size") ? readSpan() : Span.LISTS;
    try {
      return new MonotonicGen(range, size, take("reset"));
    } catch (IllegalArgumentException bad) {
      throw error(bad.getMessage());
    }
  }

  private Generator record() {
    if (!takeSymbol(":")) {
      throw error("expected :");
    }
    boolean grouped = takeSymbol("(");
    if (records > 0 && !grouped) {
      throw error("expected (");
    }
    records++;
    try {
      List<String> names = new ArrayList<>();
      List<Generator> fields = new ArrayList<>();
      field(names, fields);
      while (takeSymbol(";")) {
        field(names, fields);
      }
      if (grouped && !takeSymbol(")")) {
        throw error("expected )");
      }
      return new RecordGen(names, fields);
    } finally {
      records--;
    }
  }

  private void field(List<String> names, List<Generator> fields) {
    String name = name("field");
    if (names.contains(name)) {
      throw error(name + " is already a field");
    }
    Generator generator = spec();
    if (generator.domain() != null) {
      throw error("all of is enumerated, not sampled");
    }
    names.add(name);
    fields.add(generator);
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
    while (at < text.length() && choiceChar(text.charAt(at))) {
      at++;
    }
    if (start == at) {
      throw error("empty choice");
    }
    return text.substring(start, at);
  }

  private boolean choiceChar(char c) {
    if (c == ',' || c == ';' || Character.isWhitespace(c)) {
      return false;
    }
    return c != ')' || records == 0;
  }

  private boolean tokenChar(char c) {
    if (Character.isWhitespace(c) || c == ';') {
      return false;
    }
    return c != ')' || records == 0;
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
