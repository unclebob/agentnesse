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
    MomentGen, PeriodGen, NowGen {

  /** Most cases an enumeration may run. A larger domain is refused. */
  public static final int ENUMERATION_LIMIT = 256;

  public abstract Value draw(Random random);

  abstract List<Value> simpler(Value value);

  /** The cases of an enumerated generator, or null when this generator is sampled. */
  public List<Value> domain() {
    return null;
  }

  /** True when a draw depends on the property clock. */
  public boolean timed() {
    return false;
  }
}
