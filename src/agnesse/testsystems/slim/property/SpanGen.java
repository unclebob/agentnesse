package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A half-open interval {@code start+length}. The cell is {@code 3+5},
 * meaning {@code [3, 8)}. A negative start is written {@code -3+5}.
 */
final class SpanGen extends Generator {
  private final Span starts;
  private final Span lengths;

  SpanGen(Span starts, Span lengths) {
    if (lengths.min < 0) {
      throw new IllegalArgumentException("length must be zero or more");
    }
    this.starts = starts;
    this.lengths = lengths;
  }

  @Override
  String family() {
    return "span";
  }

  @Override
  public Value draw(Random random, Row row) {
    return text(starts.draw(random), lengths.draw(random));
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    int[] parts = split(value.cell());
    List<Value> out = new ArrayList<>();
    for (int length : Numbers.shorter(parts[1], lengths.min)) {
      if (lengths.contains(length)) {
        out.add(text(parts[0], length));
      }
    }
    for (int start : Numbers.closerToZero(parts[0])) {
      if (starts.contains(start)) {
        out.add(text(start, parts[1]));
      }
    }
    return out;
  }

  @Override
  public boolean holds(Value value, Row row) {
    int[] parts = splitOrNull(value.cell());
    return parts != null && starts.contains(parts[0]) && lengths.contains(parts[1]);
  }

  private static TextValue text(int start, int length) {
    return new TextValue(start + "+" + length);
  }

  private static int[] split(String cell) {
    int[] parts = splitOrNull(cell);
    if (parts == null) {
      throw new IllegalArgumentException("not a span: " + cell);
    }
    return parts;
  }

  private static int[] splitOrNull(String cell) {
    int plus = cell.lastIndexOf('+');
    try {
      int start = Integer.parseInt(cell.substring(0, plus));
      int length = Integer.parseInt(cell.substring(plus).replace("+", ""));
      return new int[] {start, length};
    } catch (NumberFormatException | IndexOutOfBoundsException bad) {
      return null;
    }
  }
}
