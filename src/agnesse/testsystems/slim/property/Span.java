package agnesse.testsystems.slim.property;

import java.util.Random;

/** An inclusive integer span. Ints, string lengths, and list sizes all use one. */
final class Span {
  static final Span INTS = new Span(-10_000, 10_000);
  static final Span TEXT = new Span(0, 20);
  static final Span LISTS = new Span(0, 8);

  final int min;
  final int max;

  Span(int min, int max) {
    if (min > max) {
      throw new IllegalArgumentException("inverted range " + min + ".." + max);
    }
    this.min = min;
    this.max = max;
  }

  boolean contains(int n) {
    return n >= min && n <= max;
  }

  int draw(Random random) {
    return (int) random.nextLong(min, (long) max + 1L);
  }
}
