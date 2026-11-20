package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A list of ints. Without {@code reset} it is non-decreasing. With {@code reset}
 * a reading below the previous one starts again from the floor, and shrinking
 * keeps that running total.
 */
final class MonotonicGen extends Generator {
  private final Span range;
  private final Span size;
  private final boolean reset;

  MonotonicGen(Span range, Span size, boolean reset) {
    if (size.min < 0) {
      throw new IllegalArgumentException("size must be zero or more");
    }
    this.range = range;
    this.size = size;
    this.reset = reset;
  }

  @Override
  String family() {
    return reset ? "monotonic reset" : "monotonic";
  }

  @Override
  public Value draw(Random random, Row row) {
    int count = size.draw(random);
    List<Value> items = new ArrayList<>();
    int previous = range.min;
    for (int i = 0; i < count; i++) {
      int low = reset ? range.min : previous;
      int reading = new Span(low, range.max).draw(random);
      items.add(new TextValue(Integer.toString(reading)));
      previous = reading;
    }
    return new ListValue(items);
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    int[] readings = readings(value);
    if (readings == null) {
      return List.of();
    }
    String current = value.cell();
    return reset ? shrinkReset(readings, current) : shrinkPlain(readings, current);
  }

  @Override
  public boolean holds(Value value, Row row) {
    int[] readings = readings(value);
    if (readings == null || readings.length < size.min || readings.length > size.max) {
      return false;
    }
    int previous = range.min;
    for (int reading : readings) {
      if (reading < range.min || reading > range.max) {
        return false;
      }
      if (!reset && reading < previous) {
        return false;
      }
      previous = reading;
    }
    return true;
  }

  private List<Value> shrinkPlain(int[] readings, String current) {
    List<Value> out = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (int length : Numbers.shorter(readings.length, size.min)) {
      offer(out, seen, filled(range.min, length), current);
    }
    for (int i = 0; i < readings.length; i++) {
      int floor = i == 0 ? range.min : Math.max(range.min, readings[i - 1]);
      offer(out, seen, replaced(readings, i, floor), current);
      offer(out, seen, replaced(readings, i, readings[i] - 1), current);
    }
    return out;
  }

  private List<Value> shrinkReset(int[] readings, String current) {
    List<Value> out = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    long sum = Meter.total(readings, range.min);
    int[] canon = Meter.canonical(sum, range.min, range.max, size.min);
    int[] padded = Meter.fit(canon, readings.length);
    if (padded != null) {
      offer(out, seen, canon, current);
      offer(out, seen, padded, current);
    }
    if (readings.length >= 2) {
      int gap = readings[1] - readings[0];
      offer(out, seen, Meter.raise(readings, gap / 2, range.max), current);
      offer(out, seen, Meter.raise(readings, 1, range.max), current);
    }
    return out;
  }

  private void offer(List<Value> out, Set<String> seen, int[] readings, String current) {
    if (readings == null || !holds(list(readings), Row.EMPTY)) {
      return;
    }
    Value value = list(readings);
    if (value.cell().equals(current) || !seen.add(value.cell())) {
      return;
    }
    out.add(value);
  }

  private static int[] filled(int value, int length) {
    int[] readings = new int[length];
    Arrays.fill(readings, value);
    return readings;
  }

  private static int[] replaced(int[] readings, int index, int value) {
    int[] next = readings.clone();
    next[index] = value;
    return next;
  }

  private static ListValue list(int[] readings) {
    List<Value> items = new ArrayList<>();
    for (int reading : readings) {
      items.add(new TextValue(Integer.toString(reading)));
    }
    return new ListValue(items);
  }

  private static int[] readings(Value value) {
    if (!(value instanceof ListValue list)) {
      return null;
    }
    int[] readings = new int[list.items().size()];
    for (int i = 0; i < readings.length; i++) {
      try {
        readings[i] = Integer.parseInt(list.items().get(i).cell());
      } catch (NumberFormatException bad) {
        return null;
      }
    }
    return readings;
  }
}
