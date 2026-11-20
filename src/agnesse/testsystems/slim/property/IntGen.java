package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class IntGen extends Generator {
  private final Span span;
  private final IntFilter filter;

  IntGen(Span span, IntFilter filter) {
    if (!filter.satisfiable(span)) {
      throw new IllegalArgumentException(
          "filter excludes every int in " + span.min + ".." + span.max);
    }
    this.span = span;
    this.filter = filter;
  }

  int lowest() {
    return filter.lowest(span);
  }

  int highest() {
    return filter.highest(span);
  }

  IntGen narrowed(int low, int high) {
    return new IntGen(new Span(low, high), IntFilter.ANY);
  }

  @Override
  String family() {
    return "int";
  }

  @Override
  int variety() {
    long count = (long) highest() - lowest() + 1L;
    if (filter.hole(span)) {
      count -= 1;
    }
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  @Override
  int compare(Value left, Value right) {
    return Integer.compare(Integer.parseInt(left.cell()), Integer.parseInt(right.cell()));
  }

  @Override
  public Value draw(Random random, Row row) {
    return text(filter.draw(random, span));
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    int n = Integer.parseInt(value.cell());
    List<Value> out = new ArrayList<>();
    for (int candidate : Numbers.closerToZero(n)) {
      if (span.contains(candidate) && filter.allows(candidate)) {
        out.add(text(candidate));
      }
    }
    return out;
  }

  private static TextValue text(int n) {
    return new TextValue(Integer.toString(n));
  }
}
