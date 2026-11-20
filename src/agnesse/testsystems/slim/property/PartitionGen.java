package agnesse.testsystems.slim.property;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * N non-negative parts that add up to a total. The scale is the scale of the
 * written total, so {@code 10.00} stays at hundredths. The cell is a Slim list.
 */
final class PartitionGen extends Generator {
  private final long total;
  private final int scale;
  private final int parts;

  PartitionGen(BigDecimal total, int parts) {
    if (parts < 1 || parts > 8) {
      throw new IllegalArgumentException("into is 1 through 8");
    }
    if (total.signum() < 0) {
      throw new IllegalArgumentException("total must be zero or more");
    }
    this.scale = total.scale();
    if (scale > DecimalGen.MAX_SCALE) {
      throw new IllegalArgumentException("scale must be 0 through " + DecimalGen.MAX_SCALE);
    }
    this.total = units(total);
    this.parts = parts;
  }

  @Override
  String family() {
    return "partition";
  }

  @Override
  public Value draw(Random random, Row row) {
    long[] cuts = new long[parts - 1];
    for (int i = 0; i < cuts.length; i++) {
      cuts[i] = random.nextLong(total + 1);
    }
    Arrays.sort(cuts);
    long[] amounts = new long[parts];
    long previous = 0;
    for (int i = 0; i < cuts.length; i++) {
      amounts[i] = cuts[i] - previous;
      previous = cuts[i];
    }
    amounts[parts - 1] = total - previous;
    return list(amounts);
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    long[] amounts = amounts(value);
    List<Value> out = new ArrayList<>();
    for (int i = 1; i < parts; i++) {
      offer(out, amounts, i, amounts[i]);
      offer(out, amounts, i, amounts[i] / 2);
      offer(out, amounts, i, 1);
    }
    return out;
  }

  @Override
  public boolean holds(Value value, Row row) {
    if (!(value instanceof ListValue list) || list.items().size() != parts) {
      return false;
    }
    long sum = 0;
    for (Value item : list.items()) {
      long part = read(item.cell());
      if (part < 0) {
        return false;
      }
      sum += part;
    }
    return sum == total;
  }

  private void offer(List<Value> out, long[] amounts, int index, long moved) {
    if (moved <= 0 || moved > amounts[index]) {
      return;
    }
    long[] next = amounts.clone();
    next[0] += moved;
    next[index] -= moved;
    out.add(list(next));
  }

  private ListValue list(long[] amounts) {
    List<Value> cells = new ArrayList<>();
    for (long amount : amounts) {
      cells.add(new TextValue(BigDecimal.valueOf(amount, scale).toPlainString()));
    }
    return new ListValue(cells);
  }

  private long[] amounts(Value value) {
    ListValue list = (ListValue) value;
    long[] amounts = new long[parts];
    for (int i = 0; i < parts; i++) {
      amounts[i] = read(list.items().get(i).cell());
    }
    return amounts;
  }

  private long read(String cell) {
    try {
      BigDecimal parsed = new BigDecimal(cell);
      if (parsed.scale() > scale) {
        return -1;
      }
      return parsed.movePointRight(scale).longValueExact();
    } catch (NumberFormatException | ArithmeticException bad) {
      return -1;
    }
  }

  private static long units(BigDecimal total) {
    try {
      return total.movePointRight(total.scale()).longValueExact();
    } catch (ArithmeticException bad) {
      throw new IllegalArgumentException("total is too wide");
    }
  }
}
