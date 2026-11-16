package agnesse.testsystems.slim.results;

import agnesse.testsystems.ExecutionResult;
import agnesse.testsystems.TestResult;

/**
 * The recorded outcome of a table, a row, or a cell. HTML is rendered from
 * this. It is not parsed back out of the HTML.
 */
public final class CellRecord {
  private final String status;
  private final String actual;
  private final String expected;
  private final SlimFailure failure;
  private final String symbol;
  private final String value;
  private final boolean assertion;
  private final boolean trouble;

  private CellRecord(String status, String actual, String expected, SlimFailure failure, String symbol, String value,
                     boolean assertion, boolean trouble) {
    this.status = status;
    this.actual = actual;
    this.expected = expected;
    this.failure = failure;
    this.symbol = symbol;
    this.value = value;
    this.assertion = assertion;
    this.trouble = trouble;
  }

  public static CellRecord from(SlimTestResult result, String original) {
    Assignment assignment = Assignment.read(result.getMessage());
    if (assignment != null) {
      return symbol(assignment, original);
    }
    return compared(result, original);
  }

  private static CellRecord symbol(Assignment assignment, String original) {
    return new CellRecord("symbol", assignment.value, original, null, assignment.name, assignment.value, false, false);
  }

  private static CellRecord compared(SlimTestResult result, String original) {
    ExecutionResult execution = result.getExecutionResult();
    boolean assertion = countsAsAssertion(result);
    String actual = result.hasActual() ? result.getActual() : result.getMessage();
    String expected = result.hasExpected() ? result.getExpected() : original;
    String status = execution == null ? null : execution.toString();
    boolean trouble = !assertion && execution == ExecutionResult.ERROR;
    return new CellRecord(status, actual, expected, null, null, null, assertion, trouble);
  }

  public static CellRecord from(TestResult result) {
    if (result instanceof SlimTestResult slim) {
      return from(slim, null);
    }
    return status(null);
  }

  public static CellRecord error(SlimExceptionResult exception) {
    ExecutionResult execution = exception.getExecutionResult();
    SlimFailure failure = exception.failure();
    boolean assertion = failure.kind() == SlimFailure.Kind.EXCEPTION
      || execution == ExecutionResult.IGNORE || execution == ExecutionResult.FAIL;
    String status = assertion ? (execution == null ? "error" : execution.toString()) : null;
    return new CellRecord(status, null, null, failure, null, null, assertion, !assertion);
  }

  public static CellRecord status(String status) {
    return new CellRecord(status, null, null, null, null, null, false, false);
  }

  /** A construction or syntax failure is a problem with the table, not an assertion. */
  public CellRecord asTableProblem() {
    return new CellRecord(null, actual, expected, failure, symbol, value, false, true);
  }

  public boolean assertion() {
    return assertion;
  }

  public boolean trouble() {
    return trouble;
  }

  private static boolean countsAsAssertion(SlimTestResult result) {
    ExecutionResult execution = result.getExecutionResult();
    if (!result.doesCount() || execution == null) {
      return false;
    }
    if (execution == ExecutionResult.ERROR) {
      return result.hasExpected() || result.hasActual();
    }
    return true;
  }

  public String status() {
    return status;
  }

  public String actual() {
    return actual;
  }

  public String expected() {
    return expected;
  }

  public SlimFailure failure() {
    return failure;
  }

  public String symbol() {
    return symbol;
  }

  public String value() {
    return value;
  }

  public String dump() {
    StringBuilder out = new StringBuilder();
    out.append("{\"status\":").append(SlimFailure.json(status));
    out.append(",\"actual\":").append(SlimFailure.json(actual));
    out.append(",\"expected\":").append(SlimFailure.json(expected));
    if (symbol != null) {
      out.append(",\"symbol\":").append(SlimFailure.json(symbol));
      out.append(",\"value\":").append(SlimFailure.json(value));
    }
    if (failure != null) {
      out.append(",\"error\":").append(failure.dump());
    }
    return out.append('}').toString();
  }

  private static final class Assignment {
    private final String name;
    private final String value;

    private Assignment(String name, String value) {
      this.name = name;
      this.value = value;
    }

    private static Assignment read(String message) {
      if (message == null || !message.startsWith("$") || !message.endsWith("]")) {
        return null;
      }
      int arrow = message.indexOf("<-[");
      if (arrow < 2) {
        return null;
      }
      return new Assignment(message.substring(1, arrow), message.substring(arrow + 3, message.length() - 1));
    }
  }
}
