package agnesse.testsystems.slim.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Shrinks one failing schedule. Threads go first, then the prefix, then
 * commands inside a branch, and only then arguments. At most {@link Course#CAP}
 * reductions are kept. A caller decides which proposal still fails. Because a
 * race may not show on every run, {@link #anyFail} repeats one proposal.
 */
public final class Fold {
  public static final int RERUNS = 3;

  private static final int THREADS = 0;
  private static final int PREFIX = 1;
  private static final int BRANCH = 2;
  private static final int ARGS = 3;
  private static final int DONE = 4;

  private final Plan plan;
  private Schedule schedule;
  private int accepted;
  private int phase;
  private int width = Schedule.LEAST;
  private int prefixLength = 1;
  private boolean prefixEmptyTried;
  private int branch;
  private int branchLength = 1;
  private int place;
  private int argStep;
  private int argIndex;
  private int issuedPlace;
  private int issuedStep;
  private int issuedArg;
  private boolean capped;

  public Fold(Plan plan, Schedule schedule) {
    this.plan = plan;
    this.schedule = schedule;
  }

  public Schedule schedule() {
    return schedule;
  }

  public boolean capped() {
    return capped;
  }

  /** True when any of {@code times} runs fails. Zero runs does not fail. */
  public static boolean anyFail(int times, BooleanSupplier run) {
    for (int i = 0; i < times; i++) {
      if (run.getAsBoolean()) {
        return true;
      }
    }
    return false;
  }

  public List<Schedule> next() {
    if (accepted >= Course.CAP) {
      capped = true;
      return List.of();
    }
    while (phase < DONE) {
      List<Schedule> batch = batch();
      if (batch != null) {
        return batch;
      }
      phase++;
    }
    return List.of();
  }

  public void accept(Schedule next) {
    schedule = next;
    accepted++;
    if (phase == THREADS) {
      width = Schedule.LEAST;
      return;
    }
    if (phase == PREFIX) {
      prefixLength = 1;
      prefixEmptyTried = false;
      return;
    }
    if (phase == BRANCH) {
      branch = 0;
      branchLength = 1;
      return;
    }
    place = issuedPlace;
    argStep = issuedStep;
    argIndex = issuedArg;
  }

  private List<Schedule> batch() {
    if (phase == THREADS) {
      return threads();
    }
    if (phase == PREFIX) {
      return prefixes();
    }
    if (phase == BRANCH) {
      return commands();
    }
    return arguments();
  }

  private List<Schedule> threads() {
    int count = schedule.branches.size();
    while (width < count) {
      List<Schedule> found = subsets(width);
      width++;
      if (!found.isEmpty()) {
        return found;
      }
    }
    return null;
  }

  private List<Schedule> subsets(int keep) {
    List<Schedule> out = new ArrayList<>();
    for (List<Integer> indexes : Course.combinations(schedule.branches.size(), keep)) {
      List<Trace> kept = new ArrayList<>();
      for (int index : indexes) {
        kept.add(schedule.branches.get(index));
      }
      Schedule candidate = new Schedule(schedule.prefix, kept);
      if (candidate.legal(plan)) {
        out.add(candidate);
      }
    }
    return out;
  }

  private List<Schedule> prefixes() {
    if (!prefixEmptyTried) {
      prefixEmptyTried = true;
      if (!schedule.prefix.steps.isEmpty()) {
        Schedule empty = new Schedule(new Trace(List.of()), schedule.branches);
        if (empty.legal(plan)) {
          return List.of(empty);
        }
      }
    }
    while (prefixLength < schedule.prefix.steps.size()) {
      int length = prefixLength;
      prefixLength++;
      List<Schedule> found = prefixCuts(length);
      if (!found.isEmpty()) {
        return found;
      }
    }
    return null;
  }

  private List<Schedule> prefixCuts(int length) {
    List<Schedule> out = new ArrayList<>();
    for (Trace cut : Course.shorter(plan, schedule.prefix, length)) {
      Schedule candidate = new Schedule(cut, schedule.branches);
      if (candidate.legal(plan)) {
        out.add(candidate);
      }
    }
    return out;
  }

  private List<Schedule> commands() {
    while (branch < schedule.branches.size()) {
      Trace trace = schedule.branches.get(branch);
      while (branchLength < trace.steps.size()) {
        List<Schedule> found = branchCuts(branch, branchLength);
        branchLength++;
        if (!found.isEmpty()) {
          return found;
        }
      }
      branch++;
      branchLength = 1;
    }
    return null;
  }

  private List<Schedule> branchCuts(int which, int length) {
    Candidates after = Schedule.after(plan, schedule.prefix);
    List<Schedule> out = new ArrayList<>();
    for (Trace cut : Course.shorter(plan, schedule.branches.get(which), length, after)) {
      out.add(schedule.replace(which, cut));
    }
    return out;
  }

  private List<Schedule> arguments() {
    while (place <= schedule.branches.size()) {
      List<Schedule> found = onePlace();
      if (found != null) {
        return found;
      }
      place++;
      argStep = 0;
      argIndex = 0;
    }
    return null;
  }

  private List<Schedule> onePlace() {
    Trace trace = place == 0 ? schedule.prefix : schedule.branches.get(place - 1);
    while (argStep < trace.steps.size()) {
      List<Schedule> found = oneArg(trace);
      if (found == null) {
        argStep++;
        argIndex = 0;
      } else if (!found.isEmpty()) {
        return found;
      }
    }
    return null;
  }

  /** {@code null} when this step has no further arguments. Empty when none are simpler. */
  private List<Schedule> oneArg(Trace trace) {
    if (argIndex >= trace.steps.get(argStep).size()) {
      return null;
    }
    int step = argStep;
    int arg = argIndex;
    argIndex++;
    Candidates start = place == 0
        ? Candidates.start(plan.initial)
        : Schedule.after(plan, schedule.prefix);
    List<Trace> simpler = Course.simpler(plan, trace, step, arg, start);
    if (simpler.isEmpty()) {
      return List.of();
    }
    issuedPlace = place;
    issuedStep = step;
    issuedArg = arg;
    return placed(simpler);
  }

  private List<Schedule> placed(List<Trace> traces) {
    List<Schedule> out = new ArrayList<>();
    for (Trace trace : traces) {
      out.add(place == 0 ? new Schedule(trace, schedule.branches) : schedule.replace(place - 1, trace));
    }
    return out;
  }
}
