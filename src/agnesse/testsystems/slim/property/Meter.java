package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;

/** The running total of a meter that may reset, and the smallest sequence with that total. */
final class Meter {
  private Meter() {
  }

  static long total(int[] readings, int floor) {
    long sum = 0;
    int previous = floor;
    for (int reading : readings) {
      if (reading >= previous) {
        sum += (long) reading - previous;
      } else {
        sum += (long) reading - floor;
      }
      previous = reading;
    }
    return sum;
  }

  /** Shortest reading list whose running total is {@code sum}, padded out to {@code least}. */
  static int[] canonical(long sum, int floor, int ceil, int least) {
    if (ceil < floor) {
      throw new IllegalArgumentException("inverted range");
    }
    if (sum < 0) {
      throw new IllegalArgumentException("total must be zero or more");
    }
    long width = (long) ceil - floor;
    List<Integer> out = new ArrayList<>();
    if (sum == 0) {
      out.add(floor);
    } else if (width == 0) {
      throw new IllegalArgumentException("total does not fit");
    } else {
      long left = sum;
      while (left > width) {
        out.add(ceil);
        out.add(floor);
        left -= width;
      }
      out.add((int) ((long) floor + left));
    }
    while (out.size() < least) {
      out.add(out.get(out.size() - 1));
    }
    int[] readings = new int[out.size()];
    for (int i = 0; i < readings.length; i++) {
      readings[i] = out.get(i);
    }
    return readings;
  }

  static int[] fit(int[] source, int length) {
    if (source.length > length) {
      return null;
    }
    int[] out = new int[length];
    System.arraycopy(source, 0, out, 0, source.length);
    for (int i = source.length; i < length; i++) {
      out[i] = out[i - 1];
    }
    return out;
  }

  /** Move the first reading up by {@code amount} while it still stays at or under the next. */
  static int[] raise(int[] readings, int amount, int ceil) {
    if (readings.length < 2 || amount < 1) {
      return null;
    }
    if (amount > readings[1] - readings[0]) {
      return null;
    }
    if ((long) readings[0] + amount > ceil) {
      return null;
    }
    int[] next = readings.clone();
    next[0] = readings[0] + amount;
    return next;
  }
}
