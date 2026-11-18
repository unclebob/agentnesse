package agnesse.testsystems.slim.property;

import java.util.Random;

/**
 * The explicit source of a property's draws. A seed is a long the author can
 * read off a result and run again. It is never taken from the clock, and two
 * seeds never share a generator.
 */
public final class Seed {
  private final long value;

  public Seed(long value) {
    this.value = value;
  }

  public long value() {
    return value;
  }

  /** A new stream positioned at the start of this seed. */
  public Random random() {
    return new Random(value);
  }
}
