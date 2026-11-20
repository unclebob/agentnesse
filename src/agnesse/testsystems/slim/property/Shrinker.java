package agnesse.testsystems.slim.property;

import java.util.List;
import java.util.function.Predicate;

/**
 * Walks a failing value toward a smaller one that still fails.
 * Each accepted candidate costs one step. The walk stops at the cap,
 * or when every simpler candidate passes.
 */
public final class Shrinker {
  /** The simpler values of {@code current}, most aggressive first. One shrink round. */
  public List<Value> candidates(Generator generator, Value current) {
    return candidates(generator, current, Row.EMPTY);
  }

  public List<Value> candidates(Generator generator, Value current, Row row) {
    return List.copyOf(generator.simpler(current, row));
  }

  public Value shrink(Generator generator, Value start, Predicate<Value> stillFails, int stepCap) {
    if (stepCap < 0) {
      throw new IllegalArgumentException("step cap must be zero or more");
    }
    Value current = start;
    int steps = 0;
    while (steps < stepCap) {
      Value next = firstFailure(generator.simpler(current), stillFails);
      if (next == null) {
        return current;
      }
      current = next;
      steps = steps + 1;
    }
    return current;
  }

  private static Value firstFailure(List<Value> candidates, Predicate<Value> stillFails) {
    for (Value candidate : candidates) {
      if (stillFails.test(candidate)) {
        return candidate;
      }
    }
    return null;
  }
}
