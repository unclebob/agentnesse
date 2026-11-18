package agnesse.testsystems.slim.property;

import java.util.Random;

/**
 * One restriction on an int span. {@code !=} leaves a single hole.
 * Every other filter narrows the span to a smaller inclusive interval.
 */
final class IntFilter {
  enum Kind { ANY, NE, GT, GE, LT, LE }

  static final IntFilter ANY = new IntFilter(Kind.ANY, 0);

  final Kind kind;
  final int bound;

  IntFilter(Kind kind, int bound) {
    this.kind = kind;
    this.bound = bound;
  }

  boolean allows(int n) {
    return switch (kind) {
      case ANY -> true;
      case NE -> n != bound;
      case GT -> n > bound;
      case GE -> n >= bound;
      case LT -> n < bound;
      case LE -> n <= bound;
    };
  }

  /** False only when the span is exactly the excluded point, or the comparison misses it. */
  boolean satisfiable(Span span) {
    return switch (kind) {
      case ANY -> true;
      case NE -> span.min != bound || span.max != bound;
      case GT -> span.max > bound;
      case GE -> span.max >= bound;
      case LT -> span.min < bound;
      case LE -> span.min <= bound;
    };
  }

  int lowest(Span span) {
    return switch (kind) {
      case GT -> (int) Math.max(span.min, (long) bound + 1L);
      case GE -> Math.max(span.min, bound);
      case ANY, NE, LT, LE -> span.min;
    };
  }

  int highest(Span span) {
    return switch (kind) {
      case LT -> (int) Math.min(span.max, (long) bound - 1L);
      case LE -> Math.min(span.max, bound);
      case ANY, NE, GT, GE -> span.max;
    };
  }

  boolean hole(Span span) {
    return kind == Kind.NE && bound >= span.min && bound <= span.max;
  }

  /** The value for index {@code k} in the span with {@code bound} removed. */
  int mapHole(Span span, long k) {
    int n = (int) (span.min + k);
    if (n >= bound) {
      n = n + 1;
    }
    return n;
  }

  int draw(Random random, Span span) {
    if (hole(span)) {
      long count = (long) span.max - span.min;
      return mapHole(span, random.nextLong(count));
    }
    return new Span(lowest(span), highest(span)).draw(random);
  }
}
