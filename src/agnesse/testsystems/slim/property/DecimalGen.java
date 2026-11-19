package agnesse.testsystems.slim.property;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A fixed-point decimal. Values are whole counts of {@code 10^-scale}, never
 * binary fractions. Money is this generator with scale 2.
 */
final class DecimalGen extends Generator {
  static final int MAX_SCALE = 8;

  private final long low;
  private final long high;
  private final int scale;

  DecimalGen(BigDecimal min, BigDecimal max, int scale) {
    if (scale < 0 || scale > MAX_SCALE) {
      throw new IllegalArgumentException("scale must be 0 through " + MAX_SCALE);
    }
    this.scale = scale;
    this.low = bound(min);
    this.high = bound(max);
    if (low > high) {
      throw new IllegalArgumentException("inverted range " + min.toPlainString() + ".." + max.toPlainString());
    }
    width();
  }

  int scale() {
    return scale;
  }

  @Override
  public Value draw(Random random) {
    return text(low + random.nextLong(width()));
  }

  @Override
  List<Value> simpler(Value value) {
    long current = unscaled(value.cell());
    List<Value> out = new ArrayList<>();
    offer(out, current, 0L);
    for (int places = 0; places < scale; places++) {
      offer(out, current, rounder(current, scale - places));
    }
    offer(out, current, current / 2);
    offer(out, current, step(current));
    return out;
  }

  private void offer(List<Value> out, long current, long candidate) {
    if (!nearerZero(candidate, current) || candidate < low || candidate > high || listed(out, candidate)) {
      return;
    }
    out.add(text(candidate));
  }

  private boolean listed(List<Value> out, long candidate) {
    String rendered = format(candidate);
    for (Value value : out) {
      if (value.cell().equals(rendered)) {
        return true;
      }
    }
    return false;
  }

  private TextValue text(long unscaled) {
    return new TextValue(format(unscaled));
  }

  private String format(long unscaled) {
    return BigDecimal.valueOf(unscaled, scale).toPlainString();
  }

  private long unscaled(String cell) {
    try {
      BigDecimal parsed = new BigDecimal(cell);
      if (parsed.scale() > scale) {
        throw bad(cell);
      }
      return parsed.movePointRight(scale).longValueExact();
    } catch (NumberFormatException | ArithmeticException e) {
      throw bad(cell);
    }
  }

  private long bound(BigDecimal value) {
    if (value.scale() > scale) {
      throw new IllegalArgumentException("bound has more decimal places than the scale");
    }
    try {
      return value.movePointRight(scale).longValueExact();
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("range is too wide");
    }
  }

  private long width() {
    try {
      return Math.addExact(Math.subtractExact(high, low), 1);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("range is too wide");
    }
  }

  private static long rounder(long current, int drop) {
    long factor = 1;
    for (int i = 0; i < drop; i++) {
      factor *= 10L;
    }
    return current / factor * factor;
  }

  private static long step(long current) {
    if (current == Long.MIN_VALUE) {
      return Long.MIN_VALUE + 1;
    }
    return current - Long.signum(current);
  }

  private static boolean nearerZero(long candidate, long current) {
    if (current < 0) {
      return candidate > current && candidate <= 0;
    }
    return candidate < current && candidate >= 0;
  }

  private static IllegalArgumentException bad(String cell) {
    return new IllegalArgumentException("not a decimal: " + cell);
  }
}
