package agnesse.testsystems.slim.tables;

import agnesse.slim.instructions.Instruction;
import agnesse.testsystems.ExecutionResult;
import agnesse.testsystems.TestExecutionException;
import agnesse.testsystems.TestResult;
import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.SlimTestContext;
import agnesse.testsystems.slim.Table;
import agnesse.testsystems.slim.property.Value;
import agnesse.testsystems.slim.results.SlimExceptionResult;
import agnesse.testsystems.slim.results.SlimTestResult;
import agnesse.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One claim checked against generated inputs. The trials travel in a single
 * Slim batch. After the first failure, each shrink round is another single
 * batch: every simpler candidate, most aggressive first, and the first that
 * still fails is kept. The page shows that shrunk row and records one
 * assertion. The seed is written on the result and not saved back onto the page.
 *
 * <p>A fixture exception during a round counts as a failing candidate, so
 * shrinking continues, and it is not itself an assertion. A stop-test
 * exception ends the walk after the current round: the first failure in that
 * round is shown and no further batch is sent. The walk also stops at
 * {@link ShrinkLoop#CAP} accepted steps, and the trials cell then says
 * {@code shrink cap}.
 */
public class PropertyTable extends SlimTable {
  private PropertySheet sheet;
  private List<Trial> trials = List.of();
  private ShrinkLoop shrink;
  private Trial best;
  private final List<Trial> probes = new ArrayList<>();
  private final List<Value> probeValues = new ArrayList<>();
  private boolean painted;
  private String originalDraw = "";
  private boolean seedShown;
  int shrinkRounds;
  static int lastRounds;

  private void showSeedOnce() {
    if (seedShown) {
      return;
    }
    seedShown = true;
    showSeed();
  }

  static PropertyTable latest;

  public PropertyTable(Table table, String id, SlimTestContext context) {
    super(table, id, context);
    latest = this;
  }

  @Override
  protected String getTableType() {
    return "propertyTable";
  }

  @Override
  public List<SlimAssertion> getAssertions() throws TestExecutionException {
    sheet = PropertySheet.read(table, getFixtureName(), getTestContext());
    trials = trials(sheet.draw());
    if (sheet.scenario == null) {
      return fixtureAssertions();
    }
    return scenarioAssertions();
  }

  @Override
  public List<SlimAssertion> furtherAssertions() throws TestExecutionException {
    if (painted) {
      return List.of();
    }
    if (best == null) {
      return openFailure();
    }
    if (!probes.isEmpty()) {
      applyRound();
    }
    return nextBatch();
  }

  @Override
  public void finishNow() {
    if (painted) {
      return;
    }
    if (!probes.isEmpty()) {
      applyRound();
    }
    if (best == null) {
      best = firstFailed();
    }
    if (best == null) {
      paintPass();
    } else {
      paintRowOnly();
    }
    painted = true;
  }

  private List<SlimAssertion> openFailure() throws TestExecutionException {
    best = firstFailed();
    if (best == null) {
      paintPass();
      painted = true;
      return List.of();
    }
    shrink = ShrinkLoop.start(sheet, best.cells, best.values);
    originalDraw = drawText(best);
    return nextBatch();
  }

  private String drawText(Trial trial) {
    StringBuilder out = new StringBuilder();
    for (PropertySheet.Column column : sheet.columns) {
      if (column.kind != PropertySheet.Kind.INPUT) {
        continue;
      }
      if (out.length() > 0) {
        out.append(", ");
      }
      out.append(trial.cells.get(column.index));
    }
    return out.toString();
  }

  private List<SlimAssertion> show() throws TestExecutionException {
    int row = markdown().addRow(new ArrayList<>(best.cells));
    paintInputs(row);
    if (sheet.scenario != null) {
      showSeedOnce();
      caption(summaryRow());
      painted = true;
      lastRounds = shrinkRounds;
      return shownScenario(best, row);
    }
    if (best.failResult != null) {
      markdown().updateContent(best.failAt, row, best.failResult);
      getTestContext().increment(best.failResult.getExecutionResult());
      showSeedOnce();
      caption(summaryRow());
    } else {
      showSeedOnce();
      mark(summaryRow(), failure(best));
    }
    painted = true;
    lastRounds = shrinkRounds;
    return List.of();
  }

  private void paintInputs(int row) {
    for (int column = 0; column < best.cells.size(); column++) {
      markdown().updateContent(column, row, shown(best.cells.get(column)));
    }
  }

  private void paintPass() {
    showSeedOnce();
    mark(summaryRow(), SlimTestResult.pass(sheet.trials + " trials"));
  }

  private void paintRowOnly() {
    int row = markdown().addRow(new ArrayList<>(best.cells));
    paintInputs(row);
    if (best.failResult != null) {
      markdown().updateContent(best.failAt, row, best.failResult);
      getTestContext().increment(best.failResult.getExecutionResult());
      showSeedOnce();
      caption(summaryRow());
    } else {
      showSeedOnce();
      mark(summaryRow(), failure(best));
    }
    lastRounds = shrink == null ? 0 : shrinkRounds;
  }

  private List<SlimAssertion> nextBatch() throws TestExecutionException {
    List<Value> round = shrink.nextRound();
    if (round.isEmpty()) {
      return show();
    }
    shrinkRounds++;
    return probesFor(round);
  }

  private void applyRound() {
    shrink.accept(firstProbe());
    probes.clear();
    probeValues.clear();
  }

  private Value firstProbe() {
    for (int i = 0; i < probes.size(); i++) {
      if (probes.get(i).failed()) {
        remember(probes.get(i));
        return probeValues.get(i);
      }
    }
    return null;
  }

  private void remember(Trial probe) {
    best.failAt = probe.failAt;
    best.failResult = probe.failResult;
    best.broken = probe.broken;
    best.stop = probe.stop;
  }

  private List<Trial> trials(List<PropertySheet.Drawn> draws) {
    List<Trial> built = new ArrayList<>();
    for (int index = 0; index < draws.size(); index++) {
      PropertySheet.Drawn drawn = draws.get(index);
      built.add(new Trial(index, drawn.cells, drawn.values));
    }
    return built;
  }

  private List<SlimAssertion> fixtureAssertions() {
    List<SlimAssertion> assertions = new ArrayList<>();
    Trial host = trials.get(0);
    assertions.add(constructFixture(sheet.name));
    assertions.add(optional(callFunction(getTableName(), "table", tableAsList()), host));
    assertions.add(optional(callFunction(getTableName(), "beginTable"), host));
    for (Trial trial : trials) {
      assertions.add(optional(callFunction(getTableName(), "reset"), trial));
      assertions.addAll(sets(trial));
      assertions.add(optional(callFunction(getTableName(), "execute"), trial));
      assertions.addAll(checks(trial));
    }
    assertions.add(optional(callFunction(getTableName(), "endTable"), host));
    return assertions;
  }

  private List<SlimAssertion> sets(Trial trial) {
    List<SlimAssertion> assertions = new ArrayList<>();
    for (PropertySheet.Column column : sheet.columns) {
      if (column.kind == PropertySheet.Kind.INPUT) {
        String value = trial.cells.get(column.index);
        assertions.add(required(callFunction(getTableName(), "set " + column.name, value), trial));
      }
    }
    return assertions;
  }

  private List<SlimAssertion> checks(Trial trial) {
    List<SlimAssertion> assertions = new ArrayList<>();
    for (PropertySheet.Column column : sheet.columns) {
      if (column.kind == PropertySheet.Kind.OUTPUT) {
        assertions.add(makeAssertion(
          callFunction(getTableName(), column.name),
          new Check(trial, column.index, new Judge(column.literal))));
      }
    }
    return assertions;
  }

  private List<SlimAssertion> scenarioAssertions() throws TestExecutionException {
    List<SlimAssertion> assertions = new ArrayList<>();
    for (Trial trial : trials) {
      assertions.addAll(quietScenario(trial, "t" + trial.index));
    }
    return assertions;
  }

  private List<SlimAssertion> quietScenario(Trial trial, String token) throws TestExecutionException {
    QuietContext quiet = new QuietContext(getTestContext());
    trial.quiet = quiet;
    return scenarioScript(trial, quiet, token, null);
  }

  private List<SlimAssertion> shownScenario(Trial trial, int row) throws TestExecutionException {
    return scenarioScript(trial, getTestContext(), "show", row);
  }

  private List<SlimAssertion> scenarioScript(Trial trial, SlimTestContext context, String token, Integer row)
      throws TestExecutionException {
    Table template = sheet.scenario.getTable().asTemplate(content -> substitute(content, arguments(trial)));
    ScriptTable script = SlimTableFactory.createTable(
      getTestContext().getCurrentScriptClass(), template, id + "s" + shrinkRounds + token, context);
    if (row == null) {
      script.adoptedBy(this);
    } else {
      addChildTable(script, row);
    }
    return script.getAssertions();
  }

  private List<SlimAssertion> probesFor(List<Value> round) throws TestExecutionException {
    probes.clear();
    probeValues.clear();
    List<SlimAssertion> assertions = new ArrayList<>();
    int column = shrink.column();
    for (Value candidate : round) {
      Trial probe = probe(candidate, column);
      probes.add(probe);
      probeValues.add(candidate);
      if (sheet.scenario == null) {
        assertions.add(optional(callFunction(getTableName(), "reset"), probe));
        assertions.addAll(sets(probe));
        assertions.add(optional(callFunction(getTableName(), "execute"), probe));
        assertions.addAll(checks(probe));
      } else {
        assertions.addAll(quietScenario(probe, "p" + probes.size()));
      }
    }
    return assertions;
  }

  private Trial probe(Value candidate, int column) {
    List<String> cells = new ArrayList<>(best.cells);
    List<Value> values = new ArrayList<>(best.values);
    cells.set(column, candidate.cell());
    values.set(column, candidate);
    return new Trial(best.index, cells, values);
  }

  private Map<String, String> arguments(Trial trial) {
    Map<String, String> args = new HashMap<>();
    for (PropertySheet.Column column : sheet.columns) {
      args.put(column.name, trial.cells.get(column.index));
    }
    return args;
  }

  private static String substitute(String content, Map<String, String> args) {
    String replaced = content;
    for (Map.Entry<String, String> arg : args.entrySet()) {
      replaced = StringUtils.replace(replaced, "@{" + arg.getKey() + "}", arg.getValue());
      replaced = StringUtils.replace(replaced, "@" + arg.getKey(), arg.getValue());
    }
    return replaced;
  }

  private SlimAssertion optional(Instruction instruction, Trial trial) {
    return makeAssertion(instruction, new QuietCall(trial, true));
  }

  private SlimAssertion required(Instruction instruction, Trial trial) {
    return makeAssertion(instruction, new QuietCall(trial, false));
  }

  private MarkdownTable markdown() {
    return (MarkdownTable) table;
  }

  private Trial firstFailed() {
    for (Trial trial : trials) {
      if (trial.failed()) {
        return trial;
      }
    }
    return null;
  }

  private void showSeed() {
    String text = Long.toString(sheet.seed.value());
    int row = sheet.seedRow;
    if (!sheet.hasSeed) {
      row = markdown().addRow(List.of("seed", text));
    } else {
      widen(row);
      markdown().substitute(1, row, text);
    }
    markdown().updateContent(1, row, shown(text));
  }

  private int summaryRow() {
    if (sheet.hasTrials) {
      widen(sheet.trialsRow);
      return sheet.trialsRow;
    }
    return markdown().addRow(List.of("trials", ""));
  }

  private void widen(int row) {
    if (markdown().getColumnCountInRow(row) < 2) {
      markdown().addColumnToRow(row, "");
    }
  }

  private void mark(int row, SlimTestResult result) {
    showSeedOnce();
    markdown().updateContent(1, row, result);
    getTestContext().increment(result.getExecutionResult());
  }

  private void caption(int row) {
    showSeedOnce();
    markdown().updateContent(1, row, shown(label(best)));
  }

  private SlimTestResult failure(Trial failed) {
    if (failed.stop || failed.quiet != null) {
      return SlimTestResult.fail(label(failed));
    }
    return SlimTestResult.error(label(failed));
  }

  private String label(Trial failed) {
    String story = "trial " + (failed.index + 1) + " of " + sheet.trials;
    if (shrink != null && shrink.capped()) {
      return story + " drew " + originalDraw + "; shrink cap";
    }
    if (shrink != null && shrink.steps() > 0) {
      return story + " drew " + originalDraw + "; shrunk";
    }
    return story;
  }

  private static SlimTestResult shown(String text) {
    return SlimTestResult.plain(text);
  }

  static final class Trial {
    final int index;
    final List<String> cells;
    final List<Value> values;
    QuietContext quiet;
    int failAt = -1;
    SlimTestResult failResult;
    boolean broken;
    boolean stop;

    Trial(int index, List<String> cells, List<Value> values) {
      this.index = index;
      this.cells = cells;
      this.values = values;
    }

    boolean failed() {
      if (quiet != null) {
        return quiet.failed();
      }
      return broken || failResult != null;
    }

    void failCheck(SlimTestResult result, int column) {
      if (failed()) {
        return;
      }
      failAt = column;
      failResult = result;
    }

    void breakTrial(SlimExceptionResult exceptionResult) {
      if (failed()) {
        return;
      }
      broken = true;
      stop = exceptionResult != null && (exceptionResult.isStopTestException() || exceptionResult.isStopSuiteException());
    }
  }

  private final class QuietCall implements SlimExpectation {
    private final Trial trial;
    private final boolean optional;

    private QuietCall(Trial trial, boolean optional) {
      this.trial = trial;
      this.optional = optional;
    }

    @Override public TestResult evaluateExpectation(Object returnValue) { return null; }

    @Override
    public SlimExceptionResult evaluateException(SlimExceptionResult exceptionResult) {
      if (optional && missing(exceptionResult)) {
        return null;
      }
      trial.breakTrial(exceptionResult);
      return null;
    }

    private boolean missing(SlimExceptionResult exceptionResult) {
      return exceptionResult.isNoMethodInClassException() || exceptionResult.isNoInstanceException();
    }
  }

  private final class Check implements SlimExpectation {
    private final Trial trial;
    private final int column;
    private final Judge judge;

    private Check(Trial trial, int column, Judge judge) {
      this.trial = trial;
      this.column = column;
      this.judge = judge;
    }

    @Override
    public TestResult evaluateExpectation(Object returnValue) {
      if (returnValue == null) {
        trial.breakTrial(null);
        return null;
      }
      note(judge.judge(returnValue.toString()));
      return null;
    }

    @Override
    public SlimExceptionResult evaluateException(SlimExceptionResult exceptionResult) {
      trial.breakTrial(exceptionResult);
      return null;
    }

    private void note(SlimTestResult result) {
      ExecutionResult execution = result.getExecutionResult();
      if (execution == ExecutionResult.PASS || execution == ExecutionResult.IGNORE) {
        return;
      }
      trial.failCheck(result, column);
    }
  }

  private final class Judge extends ReturnedValueExpectation {
    private Judge(String expected) {
      super(0, 0, expected);
    }

    private SlimTestResult judge(String actual) {
      return createEvaluationMessage(actual, getExpected());
    }
  }
}
