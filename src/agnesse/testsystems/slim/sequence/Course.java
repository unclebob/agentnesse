package agnesse.testsystems.slim.sequence;

import agnesse.testsystems.slim.property.Row;
import agnesse.testsystems.slim.property.Shrinker;
import agnesse.testsystems.slim.property.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Draws command sequences from a seed and offers shorter ones that still replay. */
public final class Course {
  public static final int CAP = 32;
  private static final int WIDE = 64;
  private static final Shrinker SHRINKER = new Shrinker();

  private Course() {
  }

  public static List<Trace> draw(Plan plan, Random random, int trials) {
    List<Trace> traces = new ArrayList<>();
    for (int i = 0; i < trials; i++) {
      traces.add(drawOne(plan, random));
    }
    return traces;
  }

  public static Trace drawOne(Plan plan, Random random) {
    int length = plan.stepsMin + random.nextInt(plan.stepsMax - plan.stepsMin + 1);
    Map<String, String> state = new LinkedHashMap<>(plan.initial);
    List<Step> steps = new ArrayList<>();
    while (steps.size() < length) {
      List<Integer> enabled = enabled(plan, state);
      if (enabled.isEmpty()) {
        break;
      }
      int index = enabled.get(random.nextInt(enabled.size()));
      Step step = new Step(index, drawArgs(plan.commands.get(index), random));
      state = plan.apply(state, step);
      steps.add(step);
    }
    return new Trace(steps);
  }

  public static List<Trace> shorter(Plan plan, Trace trace, int length) {
    List<Trace> out = new ArrayList<>();
    for (List<Integer> indexes : combinations(trace.steps.size(), length)) {
      Trace candidate = subsequence(trace, indexes);
      if (plan.legal(candidate)) {
        out.add(candidate);
      }
    }
    return out;
  }

  public static List<Trace> simpler(Plan plan, Trace trace, int stepIndex, int argIndex) {
    Step step = trace.steps.get(stepIndex);
    Command command = plan.commands.get(step.command);
    Command.Argument argument = command.arguments.get(argIndex);
    if (argument.generator == null) {
      return List.of();
    }
    List<Trace> out = new ArrayList<>();
    Row row = earlier(command, step, argIndex);
    for (Value candidate : SHRINKER.candidates(argument.generator, step.value(argIndex), row)) {
      Trace next = replace(trace, stepIndex, argIndex, candidate);
      if (plan.legal(next)) {
        out.add(next);
      }
    }
    return out;
  }

  private static List<Integer> enabled(Plan plan, Map<String, String> state) {
    List<Integer> indexes = new ArrayList<>();
    for (int i = 0; i < plan.commands.size(); i++) {
      if (plan.allows(i, state)) {
        indexes.add(i);
      }
    }
    return indexes;
  }

  private static List<Value> drawArgs(Command command, Random random) {
    List<Value> values = new ArrayList<>();
    Row row = Row.EMPTY;
    for (Command.Argument argument : command.arguments) {
      Value value = one(argument, random, row);
      values.add(value);
      if (value != null) {
        row = row.with(argument.name, value);
      }
    }
    return values;
  }

  private static Value one(Command.Argument argument, Random random, Row row) {
    if (argument.generator != null) {
      return argument.generator.draw(random, row);
    }
    if (argument.literal != null) {
      return Value.text(argument.literal);
    }
    return null;
  }

  private static Row earlier(Command command, Step step, int argIndex) {
    Row row = Row.EMPTY;
    for (int i = 0; i < argIndex; i++) {
      Value value = step.value(i);
      if (value != null) {
        row = row.with(command.arguments.get(i).name, value);
      }
    }
    return row;
  }

  private static Trace subsequence(Trace trace, List<Integer> indexes) {
    List<Step> steps = new ArrayList<>();
    for (int index : indexes) {
      steps.add(trace.steps.get(index));
    }
    return new Trace(steps);
  }

  private static Trace replace(Trace trace, int stepIndex, int argIndex, Value candidate) {
    List<Step> steps = new ArrayList<>();
    for (int i = 0; i < trace.steps.size(); i++) {
      steps.add(i == stepIndex ? replaceStep(trace.steps.get(i), argIndex, candidate) : trace.steps.get(i));
    }
    return new Trace(steps);
  }

  private static Step replaceStep(Step step, int argIndex, Value candidate) {
    List<Value> values = step.values();
    values.set(argIndex, candidate);
    return new Step(step.command, values);
  }

  private static List<List<Integer>> combinations(int n, int k) {
    if (k < 1 || k >= n || wide(n, k)) {
      return List.of();
    }
    List<List<Integer>> out = new ArrayList<>();
    collect(out, new int[k], 0, 0, n);
    return out;
  }

  private static boolean wide(int n, int k) {
    long count = 1;
    for (int i = 1; i <= k; i++) {
      count = count * (n - k + i) / i;
      if (count > WIDE) {
        return true;
      }
    }
    return false;
  }

  private static void collect(List<List<Integer>> out, int[] chosen, int filled, int start, int n) {
    if (filled == chosen.length) {
      out.add(row(chosen));
      return;
    }
    int last = n - (chosen.length - filled);
    for (int i = start; i <= last; i++) {
      chosen[filled] = i;
      collect(out, chosen, filled + 1, i + 1, n);
    }
  }

  private static List<Integer> row(int[] chosen) {
    List<Integer> indexes = new ArrayList<>();
    for (int index : chosen) {
      indexes.add(index);
    }
    return indexes;
  }
}
