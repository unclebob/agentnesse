package agnesse.testsystems.slim.sequence;

import java.util.List;

/** The commands of one trial, in the order they run. */
public final class Trace {
  public final List<Step> steps;

  public Trace(List<Step> steps) {
    this.steps = List.copyOf(steps);
  }
}
