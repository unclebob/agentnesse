package agnesse.testsystems.slim.sequence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs a serial prefix and then the branches, one wave at a time, and shrinks
 * the first schedule no serial order can explain. A wave is one call per
 * thread that still has a step. The caller decides whether those calls overlap.
 */
public final class Race {
  private Race() {
  }

  public static List<Schedule> draw(Plan plan, Random random, int trials, int threadsLow, int threadsHigh,
                                    int prefixLow, int prefixHigh) {
    List<Schedule> drawn = new ArrayList<>();
    for (int trial = 0; trial < trials; trial++) {
      drawn.add(Schedule.draw(plan, random, threadsLow, threadsHigh, prefixLow, prefixHigh));
    }
    return drawn;
  }

  public static Result check(Plan plan, List<Schedule> schedules, Desk desk, String fixture, String[] arguments,
                             Waves.Overlap overlap) {
    Ids ids = new Ids();
    for (int index = 0; index < schedules.size(); index++) {
      Schedule schedule = schedules.get(index);
      History history = play(plan, schedule, desk, fixture, arguments, overlap, ids);
      String why = explain(plan, schedule, history, Linear.BOUND);
      if (why != null) {
        return shrink(plan, schedule, index, schedules.size(), history, why, desk, fixture, arguments, overlap, ids);
      }
    }
    return Result.pass(schedules.size());
  }

  static String explain(Plan plan, Schedule schedule, History history, int bound) {
    if (!history.ok()) {
      return history.broken;
    }
    Linear.Outcome outcome = Linear.search(plan, schedule.prefix, history.prefix,
        schedule.branches, history.branches, bound);
    if (outcome == Linear.Outcome.PASS) {
      return null;
    }
    return reason(outcome, bound) + "; saw " + saw(plan, schedule, history);
  }

  public static String drew(Plan plan, Schedule schedule) {
    StringJoiner joiner = new StringJoiner(", ");
    joiner.add("prefix [" + names(plan, schedule.prefix) + "]");
    for (int index = 0; index < schedule.branches.size(); index++) {
      joiner.add("t" + (index + 1) + " [" + names(plan, schedule.branches.get(index)) + "]");
    }
    return joiner.toString();
  }

  static History play(Plan plan, Schedule schedule, Desk desk, String fixture, String[] arguments,
                      Waves.Overlap overlap, Ids ids) {
    String instance = ids.next();
    try {
      desk.make(instance, fixture, arguments);
      List<Reading> prefix = prefix(plan, schedule, desk, instance);
      return History.ok(prefix, waves(plan, schedule, desk, instance, overlap));
    } catch (RuntimeException e) {
      return History.broke(e.getMessage());
    }
  }

  private static Result shrink(Plan plan, Schedule schedule, int index, int trials, History history, String why,
                               Desk desk, String fixture, String[] arguments, Waves.Overlap overlap, Ids ids) {
    Fold fold = new Fold(plan, schedule);
    Schedule best = schedule;
    History bestHistory = history;
    String bestWhy = why;
    boolean shrunk = false;
    while (true) {
      List<Schedule> batch = fold.next();
      if (batch.isEmpty()) {
        return Result.fail(index + 1, trials, schedule, best, bestHistory, bestWhy, shrunk, fold.capped());
      }
      Attempt kept = firstFailure(plan, batch, desk, fixture, arguments, overlap, ids);
      if (kept != null) {
        fold.accept(kept.schedule);
        best = kept.schedule;
        bestHistory = kept.history;
        bestWhy = kept.why;
        shrunk = true;
      }
    }
  }

  private static Attempt firstFailure(Plan plan, List<Schedule> batch, Desk desk, String fixture,
                                      String[] arguments, Waves.Overlap overlap, Ids ids) {
    for (Schedule candidate : batch) {
      Attempt failed = rerun(plan, candidate, desk, fixture, arguments, overlap, ids);
      if (failed != null) {
        return failed;
      }
    }
    return null;
  }

  /** Tries the candidate {@link Fold#RERUNS} times. The first failure is enough. */
  private static Attempt rerun(Plan plan, Schedule candidate, Desk desk, String fixture, String[] arguments,
                               Waves.Overlap overlap, Ids ids) {
    Attempt failed = null;
    for (int run = 0; run < Fold.RERUNS && failed == null; run++) {
      failed = once(plan, candidate, desk, fixture, arguments, overlap, ids);
    }
    return failed;
  }

  private static Attempt once(Plan plan, Schedule candidate, Desk desk, String fixture, String[] arguments,
                              Waves.Overlap overlap, Ids ids) {
    History history = play(plan, candidate, desk, fixture, arguments, overlap, ids);
    String why = explain(plan, candidate, history, Linear.BOUND);
    if (why == null) {
      return null;
    }
    return new Attempt(candidate, history, why);
  }

  private static List<Reading> prefix(Plan plan, Schedule schedule, Desk desk, String instance) {
    List<Reading> readings = new ArrayList<>();
    for (Step step : schedule.prefix.steps) {
      readings.add(one(plan, desk, 0, instance, step));
    }
    return readings;
  }

  private static List<List<Reading>> waves(Plan plan, Schedule schedule, Desk desk, String instance,
                                           Waves.Overlap overlap) {
    List<List<Reading>> branches = new ArrayList<>();
    for (int lane = 0; lane < schedule.branches.size(); lane++) {
      branches.add(new ArrayList<>());
    }
    int[] at = new int[schedule.branches.size()];
    while (!Trace.finished(at, schedule.branches)) {
      wave(plan, schedule, desk, instance, overlap, branches, at);
    }
    return branches;
  }

  private static void wave(Plan plan, Schedule schedule, Desk desk, String instance, Waves.Overlap overlap,
                           List<List<Reading>> branches, int[] at) {
    List<Job> jobs = jobs(schedule, at);
    Reading[] got = new Reading[jobs.size()];
    AtomicReference<RuntimeException> failed = new AtomicReference<>();
    overlap.run(tasks(plan, desk, instance, jobs, got, failed));
    if (failed.get() != null) {
      throw failed.get();
    }
    keep(jobs, got, branches, at);
  }

  private static List<Runnable> tasks(Plan plan, Desk desk, String instance, List<Job> jobs, Reading[] got,
                                      AtomicReference<RuntimeException> failed) {
    List<Runnable> tasks = new ArrayList<>();
    for (int index = 0; index < jobs.size(); index++) {
      int at = index;
      tasks.add(() -> take(plan, desk, instance, jobs.get(at), got, at, failed));
    }
    return tasks;
  }

  private static void take(Plan plan, Desk desk, String instance, Job job, Reading[] got, int index,
                           AtomicReference<RuntimeException> failed) {
    try {
      got[index] = one(plan, desk, job.lane, instance, job.step);
    } catch (RuntimeException e) {
      failed.compareAndSet(null, e);
    }
  }

  private static void keep(List<Job> jobs, Reading[] got, List<List<Reading>> branches, int[] at) {
    for (int index = 0; index < jobs.size(); index++) {
      Job job = jobs.get(index);
      branches.get(job.lane).add(got[index]);
      at[job.lane]++;
    }
  }

  private static Reading one(Plan plan, Desk desk, int lane, String instance, Step step) {
    Command command = plan.commands.get(step.command);
    String returned = required(desk.call(lane, instance, command.name, command.call(step.values())));
    Map<String, String> queries = new LinkedHashMap<>();
    for (String query : queries(plan, step)) {
      queries.put(query, required(desk.call(lane, instance, query, List.of())));
    }
    return new Reading(returned, queries);
  }

  static List<String> queries(Plan plan, Step step) {
    List<String> names = new ArrayList<>();
    String name = plan.commands.get(step.command).name;
    for (Command command : plan.commands) {
      if (command.name.equals(name)) {
        command.collectQueries(names);
      }
    }
    for (Guard guard : plan.guards) {
      if (guard.query != null && !names.contains(guard.query)) {
        names.add(guard.query);
      }
    }
    return names;
  }

  private static String required(String value) {
    if (value == null) {
      throw new CallBroke("no result");
    }
    return value;
  }

  private static List<Job> jobs(Schedule schedule, int[] at) {
    List<Job> jobs = new ArrayList<>();
    for (int lane = 0; lane < at.length; lane++) {
      Trace branch = schedule.branches.get(lane);
      if (at[lane] < branch.steps.size()) {
        jobs.add(new Job(lane, branch.steps.get(at[lane])));
      }
    }
    return jobs;
  }

  private static String reason(Linear.Outcome outcome, int bound) {
    if (outcome == Linear.Outcome.BOUND) {
      return "linearizability search hit its bound of " + bound;
    }
    return "no serial order";
  }

  private static String saw(Plan plan, Schedule schedule, History history) {
    StringJoiner joiner = new StringJoiner(", ");
    addSaw(joiner, "prefix", schedule.prefix, history.prefix);
    for (int index = 0; index < schedule.branches.size(); index++) {
      addSaw(joiner, "t" + (index + 1), schedule.branches.get(index), history.branches.get(index));
    }
    return joiner.toString();
  }

  private static void addSaw(StringJoiner joiner, String label, Trace trace, List<Reading> readings) {
    if (trace.steps.isEmpty()) {
      return;
    }
    StringJoiner values = new StringJoiner(",");
    int count = Math.min(trace.steps.size(), readings.size());
    for (int index = 0; index < count; index++) {
      values.add(readings.get(index).returned());
    }
    joiner.add(label + " " + values);
  }

  private static String names(Plan plan, Trace trace) {
    StringJoiner joiner = new StringJoiner(" ");
    for (Step step : trace.steps) {
      joiner.add(plan.commands.get(step.command).name);
    }
    return joiner.toString();
  }

  static final class Ids {
    private int n;

    String next() {
      n++;
      return "race" + n;
    }
  }

  private static final class Job {
    private final int lane;
    private final Step step;

    private Job(int lane, Step step) {
      this.lane = lane;
      this.step = step;
    }
  }

  private static final class Attempt {
    private final Schedule schedule;
    private final History history;
    private final String why;

    private Attempt(Schedule schedule, History history, String why) {
      this.schedule = schedule;
      this.history = history;
      this.why = why;
    }
  }

  public static final class Result {
    public final boolean passed;
    public final boolean broken;
    public final int trials;
    public final int trial;
    public final Schedule drawn;
    public final Schedule schedule;
    public final History history;
    public final String why;
    public final boolean shrunk;
    public final boolean capped;

    private Result(boolean passed, boolean broken, int trials, int trial, Schedule drawn, Schedule schedule,
                   History history, String why, boolean shrunk, boolean capped) {
      this.passed = passed;
      this.broken = broken;
      this.trials = trials;
      this.trial = trial;
      this.drawn = drawn;
      this.schedule = schedule;
      this.history = history;
      this.why = why;
      this.shrunk = shrunk;
      this.capped = capped;
    }

    public static Result pass(int trials) {
      return new Result(true, false, trials, 0, null, null, null, "", false, false);
    }

    public static Result fail(int trial, int trials, Schedule drawn, Schedule schedule, History history, String why,
                       boolean shrunk, boolean capped) {
      return new Result(false, !history.ok(), trials, trial, drawn, schedule, history, why, shrunk, capped);
    }
  }
}
