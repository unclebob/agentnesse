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
import agnesse.testsystems.slim.sequence.Advance;
import agnesse.testsystems.slim.sequence.Candidates;
import agnesse.testsystems.slim.sequence.Command;
import agnesse.testsystems.slim.sequence.Course;
import agnesse.testsystems.slim.sequence.Guard;
import agnesse.testsystems.slim.sequence.Reading;
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
    List<Trace> drawn = draw();
    List<SlimAssertion> batch = new ArrayList<>();
    for (int index = 0; index < drawn.size(); index++) {
      Attempt attempt = new Attempt(index, drawn.get(index));
      trials.add(attempt);
      batch.addAll(script(attempt, getTableName() + "_t" + index));
    }
    return batch;
  }

  private List<Trace> draw() throws SyntaxError {
    try {
      return Course.draw(sheet.plan, new Seed(sheet.seed).random(), sheet.trials);
    } catch (IllegalArgumentException e) {
      throw new SyntaxError(e.getMessage());
    }
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
    attempt.cast = Candidates.start(sheet.plan.initial);
    Candidates image = attempt.cast;
    for (int index = 0; index < attempt.trace.steps.size(); index++) {
      image = addStep(batch, attempt, instance, index, image);
    }
    return batch;
  }

  private Instruction make(String instance) {
    return new MakeInstruction(makeInstructionTag(), instance, sheet.fixture, sheet.arguments);
  }

  private Candidates addStep(List<SlimAssertion> batch, Attempt attempt, String instance,
                             int index, Candidates image) {
    Step step = attempt.trace.steps.get(index);
    Command command = sheet.plan.commands.get(step.command);
    List<String> queries = image.queries(sheet.plan, step);
    Noted noted = new Noted(attempt, index, queries);
    batch.add(makeAssertion(invoke(instance, command.name, command.call(step.values())), noted));
    ask(batch, instance, queries, noted);
    return image.image(sheet.plan, step);
  }

  private void ask(List<SlimAssertion> batch, String instance, List<String> queries, Noted noted) {
    for (String query : queries) {
      batch.add(makeAssertion(invoke(instance, query, List.of()), noted));
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
    Map<String, String> state = replay(attempt, attempt.hit.step);
    String expect = sheet.plan.substitute(guard.expect, state, argsAt(attempt, attempt.hit.step));
    String query = guard.query == null ? "" : guard.query + "?";
    int row = markdown().addRow(List.of("invariant", guard.whenText, guard.modelText, query, expect));
    show(row);
    markdown().updateContent(attempt.hit.model ? 2 : 4, row, attempt.hit.result);
    getTestContext().increment(attempt.hit.result.getExecutionResult());
  }

  private List<String> cells(Attempt attempt, int index) {
    Step step = attempt.trace.steps.get(index);
    Command command = sheet.plan.commands.get(attempt.shown[index]);
    String[] row = blanks(command, step);
    fillExpects(row, command, replay(attempt, index), argsAt(attempt, index));
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
    return sheet.plan.commands.get(attempt.shown[index]).args(step.values());
  }

  private Map<String, String> replay(Attempt attempt, int inclusive) {
    Map<String, String> state = new LinkedHashMap<>(sheet.plan.initial);
    for (int index = 0; index <= inclusive; index++) {
      Step step = attempt.trace.steps.get(index);
      state = sheet.plan.apply(state, new Step(attempt.shown[index], step.values()));
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
    markdown().secondCell(sheet.trialsRow);
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
    return "trial " + (best.index + 1) + " of " + sheet.trials + " drew " + original + "; " + where() + detail() + tail();
  }

  private String detail() {
    if (best.hit.detail.isEmpty()) {
      return "";
    }
    return "; " + best.hit.detail;
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
      markdown().secondCell(row);
      markdown().substitute(1, row, Long.toString(sheet.seed));
    }
    markdown().updateContent(1, row, SlimTestResult.plain(Long.toString(sheet.seed)));
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

  private final class Noted extends Check {
    private final List<String> queries;
    private final Map<String, String> answers = new LinkedHashMap<>();
    private int got;
    private String returned;

    private Noted(Attempt attempt, int step, List<String> queries) {
      super(attempt, step);
      this.queries = queries;
    }

    @Override
    public TestResult evaluateExpectation(Object returnValue) {
      if (attempt.failed() || attempt.absent(step, returnValue)) {
        return null;
      }
      keep(returnValue.toString());
      if (got == queries.size() + 1) {
        judge();
      }
      return null;
    }

    private void keep(String value) {
      if (got == 0) {
        returned = value;
      } else {
        answers.put(queries.get(got - 1), value);
      }
      got++;
    }

    private void judge() {
      Advance advance = attempt.cast.take(sheet.plan, attempt.trace.steps.get(step), new Reading(returned, answers));
      attempt.shown[step] = advance.shown;
      if (advance.ok()) {
        attempt.cast = advance.candidates;
        return;
      }
      attempt.fail(step, advance);
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
    private final int[] shown;
    private Candidates cast;
    private Hit hit;

    private Attempt(int index, Trace trace) {
      this.index = index;
      this.trace = trace;
      shown = new int[trace.steps.size()];
      for (int i = 0; i < shown.length; i++) {
        shown[i] = trace.steps.get(i).command;
      }
    }

    private void fail(int step, Advance advance) {
      if (advance.kind == Advance.Kind.GUARD) {
        guard(step, advance.guard, advance.name, advance.actual, advance.expected, advance.model);
        return;
      }
      mismatch(step, advance.column, advance.actual, advance.expected, advance.detail);
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

    private void mismatch(int step, int column, String actual, String expected, String detail) {
      hit = new Hit(Hit.Kind.CELL, step).at(column).result(SlimTestResult.fail(actual, expected)).detail(detail);
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
    private String name = "";
    private String detail = "";
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
    private Hit detail(String value) { this.detail = value; return this; }
    private Hit model(boolean value) { this.model = value; return this; }
    private Hit result(SlimTestResult value) { this.result = value; return this; }
    private Hit stop(boolean value) { this.stop = value; return this; }
  }
}
