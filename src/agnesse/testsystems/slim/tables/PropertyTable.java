package agnesse.testsystems.slim.tables;

import agnesse.slim.instructions.Instruction;
import agnesse.testsystems.ExecutionResult;
import agnesse.testsystems.TestExecutionException;
import agnesse.testsystems.TestResult;
import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.SlimTestContext;
import agnesse.testsystems.slim.Table;
import agnesse.testsystems.slim.results.SlimExceptionResult;
import agnesse.testsystems.slim.results.SlimTestResult;
import agnesse.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One claim checked against generated inputs. The trials travel in a single
 * Slim batch. The page records one assertion. The seed is written on the
 * result and not saved back onto the page.
 */
public class PropertyTable extends SlimTable {
  private PropertySheet sheet;
  private List<Trial> trials = List.of();

  public PropertyTable(Table table, String id, SlimTestContext context) {
    super(table, id, context);
  }

  @Override
  protected String getTableType() {
    return "propertyTable";
  }

  @Override
  public List<SlimAssertion> getAssertions() throws TestExecutionException {
    sheet = PropertySheet.read(table, getFixtureName(), getTestContext());
    trials = trials(sheet.draw());
    List<SlimAssertion> assertions = new ArrayList<>();
    if (sheet.scenario == null) {
      assertions.addAll(fixtureAssertions());
    } else {
      assertions.addAll(scenarioAssertions());
    }
    assertions.add(makeAssertion(Instruction.NOOP_INSTRUCTION, new Report()));
    return assertions;
  }

  private List<Trial> trials(List<List<String>> draws) {
    List<Trial> built = new ArrayList<>();
    for (int index = 0; index < draws.size(); index++) {
      built.add(new Trial(index, draws.get(index)));
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
      assertions.addAll(scenarioTrial(trial));
    }
    return assertions;
  }

  private List<SlimAssertion> scenarioTrial(Trial trial) throws TestExecutionException {
    QuietContext quiet = new QuietContext(getTestContext());
    trial.quiet = quiet;
    Table template = sheet.scenario.getTable().asTemplate(content -> substitute(content, arguments(trial)));
    ScriptTable script = SlimTableFactory.createTable(
      getTestContext().getCurrentScriptClass(), template, id + "t" + trial.index, quiet);
    script.adoptedBy(this);
    return script.getAssertions();
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

  private void report() {
    showSeed();
    int summary = summaryRow();
    Trial failed = firstFailed();
    if (failed == null) {
      mark(summary, SlimTestResult.pass(sheet.trials + " trials"));
      return;
    }
    int row = markdown().addRow(failed.cells);
    paintRow(failed, row);
    if (failed.failResult != null) {
      getTestContext().increment(failed.failResult.getExecutionResult());
      caption(summary, failed);
      return;
    }
    mark(summary, failure(failed));
  }

  private void paintRow(Trial failed, int row) {
    for (int column = 0; column < failed.cells.size(); column++) {
      if (column == failed.failAt && failed.failResult != null) {
        markdown().updateContent(column, row, failed.failResult);
      } else {
        markdown().updateContent(column, row, shown(failed.cells.get(column)));
      }
    }
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
    markdown().updateContent(1, row, result);
    getTestContext().increment(result.getExecutionResult());
  }

  private void caption(int row, Trial failed) {
    markdown().updateContent(1, row, shown(label(failed)));
  }

  private SlimTestResult failure(Trial failed) {
    if (failed.stop || failed.quiet != null) {
      return SlimTestResult.fail(label(failed));
    }
    return SlimTestResult.error(label(failed));
  }

  private String label(Trial failed) {
    return "failed trial " + (failed.index + 1) + " of " + sheet.trials;
  }

  private static SlimTestResult shown(String text) {
    return SlimTestResult.plain(text);
  }

  static final class Trial {
    final int index;
    final List<String> cells;
    QuietContext quiet;
    int failAt = -1;
    SlimTestResult failResult;
    boolean broken;
    boolean stop;

    Trial(int index, List<String> cells) {
      this.index = index;
      this.cells = cells;
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

  private final class Report implements SlimExpectation {
    @Override
    public TestResult evaluateExpectation(Object returnValue) {
      report();
      return null;
    }

    @Override
    public SlimExceptionResult evaluateException(SlimExceptionResult exceptionResult) {
      return null;
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
