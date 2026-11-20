package agnesse.testsystems.slim.property;

import java.util.List;
import java.util.Random;

/**
 * A generator named by one cell. {@link #draw} reads a {@link Random} opened
 * from a {@link Seed}. {@link #simpler} offers strictly smaller values for
 * the shrinker, most aggressive first.
 */
public abstract sealed class Generator
    permits IntGen, BoolGen, StringGen, ListGen, OneOfGen, DecimalGen, PatternGen, AllOfGen,
    MomentGen, PeriodGen, NowGen, Limit, SpanGen, PartitionGen, MonotonicGen, RecordGen {

  /** Most cases an enumeration may run. A larger domain is refused. */
  public static final int ENUMERATION_LIMIT = 256;

  public Value draw(Random random) {
    return draw(random, Row.EMPTY);
  }

  /** A draw that can read values already chosen for earlier columns. */
  public abstract Value draw(Random random, Row row);

  List<Value> simpler(Value value) {
    return simpler(value, Row.EMPTY);
  }

  abstract List<Value> simpler(Value value, Row row);

  /** True when {@code value} still satisfies this generator in {@code row}. */
  public boolean holds(Value value, Row row) {
    return true;
  }

  void link(Scope scope) {
  }

  abstract String family();

  /** How many distinct cells this generator can draw, or -1 when that is not known. */
  int variety() {
    return -1;
  }

  /** Domain order. Numbers and times are not ordered as text. */
  int compare(Value left, Value right) {
    return left.cell().compareTo(right.cell());
  }

  /** The cases of an enumerated generator, or null when this generator is sampled. */
  public List<Value> domain() {
    return null;
  }

  /** True when a draw depends on the property clock. */
  public boolean timed() {
    return false;
  }
}
