package agnesse.testsystems.slim.sequence;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whether recorded thread histories match some serial order of the model.
 * Each thread keeps its own order. The search stops after {@code bound} states.
 */
public final class Linear {
  public static final int BOUND = 4096;

  public enum Outcome { PASS, FAIL, BOUND }

  private Linear() {
  }

  public static Outcome search(Plan plan, Trace prefix, List<Reading> prefixReadings,
                               List<Trace> branches, List<List<Reading>> readings) {
    return search(plan, prefix, prefixReadings, branches, readings, BOUND);
  }

  public static Outcome search(Plan plan, Trace prefix, List<Reading> prefixReadings,
                               List<Trace> branches, List<List<Reading>> readings, int bound) {
    Advance played = play(plan, Candidates.start(plan.initial), prefix, prefixReadings);
    if (played == null) {
      return Outcome.FAIL;
    }
    return walk(plan, branches, readings, played.candidates, bound);
  }

  private static Advance play(Plan plan, Candidates cast, Trace trace, List<Reading> readings) {
    Advance last = Advance.ok(cast, -1);
    for (int index = 0; index < trace.steps.size(); index++) {
      last = cast.take(plan, trace.steps.get(index), readings.get(index));
      if (!last.ok()) {
        return null;
      }
      cast = last.candidates;
    }
    return last;
  }

  private static Outcome walk(Plan plan, List<Trace> branches, List<List<Reading>> readings,
                              Candidates cast, int bound) {
    ArrayDeque<Frame> stack = new ArrayDeque<>();
    Set<String> seen = new HashSet<>();
    stack.push(new Frame(new int[branches.size()], cast));
    int visited = 0;
    while (!stack.isEmpty()) {
      if (visited >= bound) {
        return Outcome.BOUND;
      }
      Frame frame = stack.pop();
      if (!seen.add(key(frame))) {
        continue;
      }
      visited++;
      if (Trace.finished(frame.at, branches)) {
        return Outcome.PASS;
      }
      extend(stack, plan, branches, readings, frame);
    }
    return Outcome.FAIL;
  }

  private static void extend(ArrayDeque<Frame> stack, Plan plan, List<Trace> branches,
                             List<List<Reading>> readings, Frame frame) {
    for (int thread = branches.size() - 1; thread >= 0; thread--) {
      offer(stack, plan, branches, readings, frame, thread);
    }
  }

  private static void offer(ArrayDeque<Frame> stack, Plan plan, List<Trace> branches,
                            List<List<Reading>> readings, Frame frame, int thread) {
    Trace branch = branches.get(thread);
    if (frame.at[thread] >= branch.steps.size()) {
      return;
    }
    Advance advance = frame.cast.take(plan, branch.steps.get(frame.at[thread]),
        readings.get(thread).get(frame.at[thread]));
    if (!advance.ok()) {
      return;
    }
    int[] next = frame.at.clone();
    next[thread]++;
    stack.push(new Frame(next, advance.candidates));
  }

  private static String key(Frame frame) {
    StringBuilder out = new StringBuilder();
    for (int index : frame.at) {
      out.append(index).append(',');
    }
    return out.append('|').append(frame.cast.mark()).toString();
  }

  private static final class Frame {
    private final int[] at;
    private final Candidates cast;

    private Frame(int[] at, Candidates cast) {
      this.at = at;
      this.cast = cast;
    }
  }
}
