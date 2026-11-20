package agnesse.testsystems.slim.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A serial prefix and one branch per thread. Each branch is a legal
 * continuation of the prefix on its own. The threads do not see each other
 * until the linearizability search.
 */
public final class Schedule {
  public static final int LEAST = 2;

  public final Trace prefix;
  public final List<Trace> branches;

  public Schedule(Trace prefix, List<Trace> branches) {
    this.prefix = prefix;
    this.branches = List.copyOf(branches);
  }

  public static Schedule draw(Plan plan, Random random, int threadsLow, int threadsHigh,
                              int prefixLow, int prefixHigh) {
    threads(threadsLow, threadsHigh);
    span(prefixLow, prefixHigh);
    int count = threadsLow + random.nextInt(threadsHigh - threadsLow + 1);
    int length = prefixLow + random.nextInt(prefixHigh - prefixLow + 1);
    Trace drawn = Course.serial(plan, random, length, Candidates.start(plan.initial));
    Candidates after = after(plan, drawn);
    List<Trace> branches = new ArrayList<>();
    for (int thread = 0; thread < count; thread++) {
      int steps = plan.stepsMin + random.nextInt(plan.stepsMax - plan.stepsMin + 1);
      branches.add(Course.serial(plan, random, steps, after));
    }
    return new Schedule(drawn, branches);
  }

  /** Every branch is a legal continuation of the prefix, and there are at least two. */
  public boolean legal(Plan plan) {
    if (branches.size() < LEAST) {
      return false;
    }
    if (!Candidates.start(plan.initial).accepts(plan, prefix)) {
      return false;
    }
    Candidates cast = after(plan, prefix);
    for (Trace branch : branches) {
      if (!cast.accepts(plan, branch)) {
        return false;
      }
    }
    return true;
  }

  Schedule replace(int branch, Trace trace) {
    List<Trace> next = new ArrayList<>(branches);
    next.set(branch, trace);
    return new Schedule(prefix, next);
  }

  static Candidates after(Plan plan, Trace prefix) {
    Candidates cast = Candidates.start(plan.initial);
    for (Step step : prefix.steps) {
      cast = cast.image(plan, step);
    }
    return cast;
  }

  private static void threads(int low, int high) {
    Course.bounds(low, high, LEAST, "threads");
  }

  private static void span(int low, int high) {
    Course.bounds(low, high, 0, "prefix");
  }
}
