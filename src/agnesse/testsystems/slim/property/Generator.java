package agnesse.testsystems.slim.property;

import java.util.List;
import java.util.Random;

/**
 * A generator named by one cell. {@link #draw} reads a {@link Random} opened
 * from a {@link Seed}. {@link #simpler} offers strictly smaller values for
 * the shrinker, most aggressive first.
 */
public abstract sealed class Generator
    permits IntGen, BoolGen, StringGen, ListGen, OneOfGen, DecimalGen, PatternGen {

  public abstract Value draw(Random random);

  abstract List<Value> simpler(Value value);
}
