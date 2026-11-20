package agnesse.testsystems.slim.sequence;

import java.util.List;

/** The commands of one trial, in the order they run. */
public final class Trace {
  public final List<Step> steps;

  public Trace(List<Step> steps) {
    this.steps = List.copyOf(steps);
  }

  /** Every thread has run every step it was given. */
  static boolean finished(int[] at, List<Trace> branches) {
    for (int index = 0; index < at.length; index++) {
      if (at[index] < branches.get(index).steps.size()) {
        return false;
      }
    }
    return true;
  }
}
