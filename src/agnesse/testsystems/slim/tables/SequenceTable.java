package agnesse.testsystems.slim.tables;

import agnesse.slim.instructions.Instruction;
import agnesse.slim.instructions.MakeInstruction;
import agnesse.testsystems.TestExecutionException;
import agnesse.testsystems.TestResult;
import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.SlimTestContext;
import agnesse.testsystems.slim.Table;
import agnesse.testsystems.slim.property.Seed;
import agnesse.testsystems.slim.property.Value;
import agnesse.testsystems.slim.results.SlimExceptionResult;
import agnesse.testsystems.slim.results.SlimTestResult;
import agnesse.testsystems.slim.sequence.Clause;
import agnesse.testsystems.slim.sequence.Command;
import agnesse.testsystems.slim.sequence.Course;
import agnesse.testsystems.slim.sequence.Guard;
import agnesse.testsystems.slim.sequence.Step;
import agnesse.testsystems.slim.sequence.Trace;
import agnesse.testsystems.slim.sequence.Walk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * A command sequence checked against a model. Each trial is a new fixture.
 * After the first failure the sequence is shrunk, and the page shows that
 * shorter sequence, the failing step or invariant, and the seed. The seed is
 * not written back onto the page. One run is one assertion.
 */
public class SequenceTable extends SlimTable {
  static SequenceTable latest;

  private SequenceSheet sheet;
  private final List<Attempt> trials = new ArrayList<>();
  private final List<Attempt> probes = new ArrayList<>();
  private Walk walk;
  private Attempt best;
  private String original = "";
  private boolean painted;
  private int round;

  public SequenceTable(Table table, String id, SlimTestContext context) {
    super(table, id, context);
    latest = this;
  }

  @Override
  protected String getTableType() {
    return "sequenceTable";
  }

  @Override
  public List<SlimAssertion> getAssertions() throws TestExecutionException {
    sheet = SequenceSheet.read(table, getFixtureName());
    List<Trace> drawn = Course.draw(sheet.plan, new Seed(sheet.seed).random(), sheet.trials);
    List<SlimAssertion> batch = new ArrayList<>();
    for (int index = 0; index < drawn.size(); index++) {
      Attempt attempt = new Attempt(index, drawn.get(index));
      trials.add(attempt);
      batch.addAll(script(attempt, getTableName() + "_t" + index));
    }
    return batch;
  }

  @Override
  public List<SlimAssertion> furtherAssertions() {
    if (painted) {
      return List.of();
    }
    if (walk == null) {
      return open();
    }
    applyRound();
    return nextRound();
  }

  @Override
  public void finishNow() {
    if (painted) {
      return;
    }
    applyRound();
    if (best == null) {
      best = first(trials);
    }
    if (best == null) {
      return;
    }
    if (original.isEmpty()) {
      original = names(best.trace);
    }
    paintFailure();
  }

  private List<SlimAssertion> open() {
    best = first(trials);
    if (best == null) {
      paintPass();
      return List.of();
    }
    original = names(best.trace);
    walk = new Walk(sheet.plan, best.trace);
    return nextRound();
  }

  private List<SlimAssertion> nextRound() {
    List<Trace> batch = walk.next();
    if (batch.isEmpty()) {
      paintFailure();
      return List.of();
    }
    round++;
    return probes(batch);
  }

  private List<SlimAssertion> probes(List<Trace> batch) {
    probes.clear();
    List<SlimAssertion> assertions = new ArrayList<>();
    for (int index = 0; index < batch.size(); index++) {
      Attempt attempt = new Attempt(best.index, batch.get(index));
      probes.add(attempt);
      assertions.addAll(script(attempt, getTableName() + "_s" + round + "_" + index));
    }
    return assertions;
  }

  private void applyRound() {
    if (probes.isEmpty()) {
      return;
    }
    Attempt failed = first(probes);
    probes.clear();
    if (failed == null) {
      return;
    }
    walk.accept(failed.trace);
    best = failed;
  }

  private List<SlimAssertion> script(Attempt attempt, String instance) {
    List<SlimAssertion> batch = new ArrayList<>();
    batch.add(makeAssertion(make(instance), new Quiet(attempt, -1)));
    Map<String, String> state = new LinkedHashMap<>(sheet.plan.initial);
    for (int index = 0; index < attempt.trace.steps.size(); index++) {
      state = addStep(batch, attempt, instance, index, state);
    }
    return batch;
  }

  private Instruction make(String instance) {
    return new MakeInstruction(makeInstructionTag(), instance, sheet.fixture, sheet.arguments);
  }

  private Map<String, String> addStep(List<SlimAssertion> batch, Attempt attempt, String instance,
                                      int index, Map<String, String> before) {
    Step step = attempt.trace.steps.get(index);
    Command command = sheet.plan.commands.get(step.command);
    Map<String, String> args = command.args(step.values());
    Map<String, String> after = sheet.plan.apply(before, step);
    batch.add(makeAssertion(invoke(instance, command.name, command.call(step.values())), new CommandSaw(attempt, index, command, after, args)));
    addQueries(batch, attempt, instance, index, command, after, args);
    addGuards(batch, attempt, instance, index, after, args);
    return after;
  }

  private void addQueries(List<SlimAssertion> batch, Attempt attempt, String instance, int index,
                          Command command, Map<String, String> state, Map<String, String> args) {
    for (Command.Expect expect : command.expects) {
      if (!expect.result) {
        String expected = sheet.plan.substitute(expect.template, state, args);
        batch.add(makeAssertion(invoke(instance, expect.method, List.of()), new Compare(attempt, index, expected, expect.column, -1, "")));
      }
    }
  }

  private void addGuards(List<SlimAssertion> batch, Attempt attempt, String instance, int index,
                         Map<String, String> state, Map<String, String> args) {
    for (int guard = 0; guard < sheet.plan.guards.size(); guard++) {
      Guard check = sheet.plan.guards.get(guard);
      if (check.query != null && Clause.holds(check.when, state)) {
        String expected = sheet.plan.substitute(check.expect, state, args);
        batch.add(makeAssertion(invoke(instance, check.query, List.of()), new Compare(attempt, index, expected, 4, guard, check.query)));
      }
    }
  }

  private Instruction invoke(String instance, String name, List<String> args) {
    return callFunction(instance, name, (Object[]) args.toArray(new String[0]));
  }

  private void paintPass() {
    painted = true;
    mark(summary(), SlimTestResult.pass(sheet.trials + " trials"));
  }

  private void paintFailure() {
    painted = true;
    paintSteps(best);
    if (best.hit.guardHit()) {
      paintGuard(best);
    }
    int row = summary();
    if (best.hit.brokenHit()) {
      mark(row, brokenResult());
    } else {
      caption(row);
    }
  }

  private void paintSteps(Attempt attempt) {
    for (int index = 0; index < attempt.trace.steps.size(); index++) {
      int row = markdown().addRow(cells(attempt, index));
      show(row);
      if (cellFailure(attempt, index)) {
        markdown().updateContent(attempt.hit.at, row, attempt.hit.result);
        getTestContext().increment(attempt.hit.result.getExecutionResult());
      }
    }
  }

  private void show(int row) {
    MarkdownTable table = markdown();
    for (int column = 0; column < table.getColumnCountInRow(row); column++) {
      table.updateContent(column, row, SlimTestResult.plain(table.originalCell(column, row)));
    }
  }

  private static boolean cellFailure(Attempt attempt, int index) {
    return attempt.hit.cellHit() && attempt.hit.step == index;
  }

  private void paintGuard(Attempt attempt) {
    Guard guard = sheet.plan.guards.get(attempt.hit.at);
    Map<String, String> state = replay(attempt.trace, attempt.hit.step);
    String expect = sheet.plan.substitute(guard.expect, state, argsAt(attempt, attempt.hit.step));
    String query = guard.query == null ? "" : guard.query + "?";
    int row = markdown().addRow(List.of("invariant", guard.whenText, guard.modelText, query, expect));
    show(row);
    markdown().updateContent(attempt.hit.model ? 2 : 4, row, attempt.hit.result);
    getTestContext().increment(attempt.hit.result.getExecutionResult());
  }

  private List<String> cells(Attempt attempt, int index) {
    Step step = attempt.trace.steps.get(index);
    Command command = sheet.plan.commands.get(step.command);
    String[] row = blanks(command, step);
    fillExpects(row, command, replay(attempt.trace, index), command.args(step.values()));
    return List.of(row);
  }

  private String[] blanks(Command command, Step step) {
    String[] row = new String[sheet.width];
    Arrays.fill(row, "");
    row[0] = command.name;
    row[1] = command.whenText;
    row[2] = command.nextText;
    for (int index = 0; index < command.arguments.size(); index++) {
      Value value = step.value(index);
      row[command.arguments.get(index).column] = value == null ? "" : value.cell();
    }
    return row;
  }

  private void fillExpects(String[] row, Command command, Map<String, String> state, Map<String, String> args) {
    for (Command.Expect expect : command.expects) {
      row[expect.column] = sheet.plan.substitute(expect.template, state, args);
    }
  }

  private Map<String, String> argsAt(Attempt attempt, int index) {
    Step step = attempt.trace.steps.get(index);
    return sheet.plan.commands.get(step.command).args(step.values());
  }

  private Map<String, String> replay(Trace trace, int inclusive) {
    Map<String, String> state = new LinkedHashMap<>(sheet.plan.initial);
    for (int index = 0; index <= inclusive; index++) {
      state = sheet.plan.apply(state, trace.steps.get(index));
    }
    return state;
  }

  private String names(Trace trace) {
    StringJoiner out = new StringJoiner(", ");
    for (Step step : trace.steps) {
      out.add(sheet.plan.commands.get(step.command).name);
    }
    return out.toString();
  }

  private int summary() {
    if (!sheet.hasTrials) {
      return markdown().addRow(List.of("trials", ""));
    }
    widen(sheet.trialsRow);
    return sheet.trialsRow;
  }

  private void mark(int row, SlimTestResult result) {
    showSeed();
    markdown().updateContent(1, row, result);
    getTestContext().increment(result.getExecutionResult());
  }

  private void caption(int row) {
    showSeed();
    markdown().updateContent(1, row, SlimTestResult.plain(label()));
  }

  private SlimTestResult brokenResult() {
    if (best.hit.stop) {
      return SlimTestResult.fail(label());
    }
    return SlimTestResult.error(label());
  }

  private String label() {
    return "trial " + (best.index + 1) + " of " + sheet.trials + " drew " + original + "; " + where() + tail();
  }

  private String where() {
    if (best.hit.step < 0) {
      return "make";
    }
    if (best.hit.guardHit()) {
      return "invariant " + best.hit.name;
    }
    Step step = best.trace.steps.get(best.hit.step);
    return "step " + (best.hit.step + 1) + " " + sheet.plan.commands.get(step.command).name;
  }

  private String tail() {
    if (walk != null && walk.capped()) {
      return "; shrink cap";
    }
    if (walk != null && walk.accepted() > 0) {
      return "; shrunk";
    }
    return "";
  }

  private void showSeed() {
    int row = sheet.seedRow;
    if (!sheet.hasSeed) {
      row = markdown().addRow(List.of("seed", Long.toString(sheet.seed)));
    } else {
      widen(row);
      markdown().substitute(1, row, Long.toString(sheet.seed));
    }
    markdown().updateContent(1, row, SlimTestResult.plain(Long.toString(sheet.seed)));
  }

  private void widen(int row) {
    if (markdown().getColumnCountInRow(row) < 2) {
      markdown().addColumnToRow(row, "");
    }
  }

  private MarkdownTable markdown() {
    return (MarkdownTable) table;
  }

  private static Attempt first(List<Attempt> attempts) {
    for (Attempt attempt : attempts) {
      if (attempt.failed()) {
        return attempt;
      }
    }
    return null;
  }

  private static String text(String value) {
    return value == null ? "" : value;
  }

  private abstract class Check implements SlimExpectation {
    final Attempt attempt;
    final int step;

    private Check(Attempt attempt, int step) {
      this.attempt = attempt;
      this.step = step;
    }

    @Override
    public SlimExceptionResult evaluateException(SlimExceptionResult exceptionResult) {
      return attempt.fault(step, exceptionResult);
    }
  }

  private final class CommandSaw extends Check {
    private final Command command;
    private final Map<String, String> state;
    private final Map<String, String> args;

    private CommandSaw(Attempt attempt, int step, Command command, Map<String, String> state, Map<String, String> args) {
      super(attempt, step);
      this.command = command;
      this.state = state;
      this.args = args;
    }

    @Override
    public TestResult evaluateExpectation(Object returnValue) {
      if (attempt.failed() || attempt.absent(step, returnValue)) {
        return null;
      }
      if (!results(returnValue.toString())) {
        model();
      }
      return null;
    }

    private boolean results(String actual) {
      for (Command.Expect expect : command.expects) {
        if (expect.result && differs(actual, expect)) {
          return true;
        }
      }
      return false;
    }

    private boolean differs(String actual, Command.Expect expect) {
      String expected = sheet.plan.substitute(expect.template, state, args);
      if (actual.equals(expected)) {
        return false;
      }
      attempt.mismatch(step, expect.column, actual, expected);
      return true;
    }

    private void model() {
      for (int index = 0; index < sheet.plan.guards.size(); index++) {
        if (broken(index)) {
          return;
        }
      }
    }

    private boolean broken(int index) {
      Guard guard = sheet.plan.guards.get(index);
      if (Clause.holds(guard.when, state)) {
        clause(index, guard);
      }
      return attempt.failed();
    }

    private void clause(int index, Guard guard) {
      for (Clause check : guard.model) {
        if (!check.holds(state, args)) {
          attempt.guard(step, index, check.name, text(state.get(check.name)), text(check.expected(state, args)), true);
          return;
        }
      }
    }
  }

  private final class Compare extends Check {
    private final String expected;
    private final int column;
    private final int guard;
    private final String name;

    private Compare(Attempt attempt, int step, String expected, int column, int guard, String name) {
      super(attempt, step);
      this.expected = expected;
      this.column = column;
      this.guard = guard;
      this.name = name;
    }

    @Override
    public TestResult evaluateExpectation(Object returnValue) {
      if (attempt.failed() || attempt.absent(step, returnValue)) {
        return null;
      }
      if (!returnValue.toString().equals(expected)) {
        record(returnValue.toString());
      }
      return null;
    }

    private void record(String actual) {
      if (guard < 0) {
        attempt.mismatch(step, column, actual, expected);
      } else {
        attempt.guard(step, guard, name, actual, expected, false);
      }
    }
  }

  private final class Quiet extends Check {
    private Quiet(Attempt attempt, int step) {
      super(attempt, step);
    }

    @Override
    public TestResult evaluateExpectation(Object returnValue) {
      attempt.absent(step, returnValue);
      return null;
    }
  }

  private static final class Attempt {
    private final int index;
    private final Trace trace;
    private Hit hit;

    private Attempt(int index, Trace trace) {
      this.index = index;
      this.trace = trace;
    }

    private boolean failed() {
      return hit != null;
    }

    private boolean absent(int step, Object returnValue) {
      if (returnValue != null) {
        return false;
      }
      broke(step, null);
      return true;
    }

    private SlimExceptionResult fault(int step, SlimExceptionResult exceptionResult) {
      broke(step, exceptionResult);
      return null;
    }

    private void mismatch(int step, int column, String actual, String expected) {
      hit = new Hit(Hit.Kind.CELL, step).at(column).result(SlimTestResult.fail(actual, expected));
    }

    private void guard(int step, int guard, String name, String actual, String expected, boolean model) {
      hit = new Hit(Hit.Kind.GUARD, step).at(guard).name(name).model(model).result(SlimTestResult.fail(actual, expected));
    }

    private void broke(int step, SlimExceptionResult exception) {
      if (!failed()) {
        hit = new Hit(Hit.Kind.BROKEN, step).stop(stopped(exception));
      }
    }

    private static boolean stopped(SlimExceptionResult exception) {
      return exception != null && (exception.isStopTestException() || exception.isStopSuiteException());
    }
  }

  private static final class Hit {
    private enum Kind { CELL, GUARD, BROKEN }

    private final Kind kind;
    private final int step;
    private int at;
    private String name;
    private boolean model;
    private SlimTestResult result;
    private boolean stop;

    private Hit(Kind kind, int step) {
      this.kind = kind;
      this.step = step;
    }

    private boolean cellHit() {
      return kind == Kind.CELL;
    }

    private boolean guardHit() {
      return kind == Kind.GUARD;
    }

    private boolean brokenHit() {
      return kind == Kind.BROKEN;
    }

    private Hit at(int value) { this.at = value; return this; }
    private Hit name(String value) { this.name = value; return this; }
    private Hit model(boolean value) { this.model = value; return this; }
    private Hit result(SlimTestResult value) { this.result = value; return this; }
    private Hit stop(boolean value) { this.stop = value; return this; }
  }
}
