package fitnesse.testsystems.slim.results;

import fitnesse.testsystems.ExecutionResult;
import fitnesse.testsystems.TestResult;

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

  private CellRecord(String status, String actual, String expected, SlimFailure failure, String symbol, String value) {
    this.status = status;
    this.actual = actual;
    this.expected = expected;
    this.failure = failure;
    this.symbol = symbol;
    this.value = value;
  }

  public static CellRecord from(SlimTestResult result, String original) {
    Assignment assignment = Assignment.read(result.getMessage());
    if (assignment != null) {
      return new CellRecord("symbol", assignment.value, original, null, assignment.name, assignment.value);
    }
    ExecutionResult execution = result.getExecutionResult();
    String actual = result.hasActual() ? result.getActual() : result.getMessage();
    String expected = result.hasExpected() ? result.getExpected() : original;
    return new CellRecord(execution == null ? null : execution.toString(), actual, expected, null, null, null);
  }

  public static CellRecord from(TestResult result) {
    ExecutionResult execution = result.getExecutionResult();
    return new CellRecord(execution == null ? null : execution.toString(),
      result.hasActual() ? result.getActual() : null,
      result.hasExpected() ? result.getExpected() : null,
      null, null, null);
  }

  public static CellRecord error(SlimExceptionResult exception) {
    ExecutionResult execution = exception.getExecutionResult();
    return new CellRecord(execution == null ? "error" : execution.toString(),
      null, null, exception.failure(), null, null);
  }

  public static CellRecord status(String status) {
    return new CellRecord(status, null, null, null, null, null);
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
