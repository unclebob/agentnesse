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

  private CellRecord(String status, String actual, String expected, SlimFailure failure) {
    this.status = status;
    this.actual = actual;
    this.expected = expected;
    this.failure = failure;
  }

  public static CellRecord from(SlimTestResult result, String original) {
    ExecutionResult execution = result.getExecutionResult();
    String actual = result.hasActual() ? result.getActual() : result.getMessage();
    String expected = result.hasExpected() ? result.getExpected() : original;
    return new CellRecord(execution == null ? null : execution.toString(), actual, expected, null);
  }

  public static CellRecord from(TestResult result) {
    ExecutionResult execution = result.getExecutionResult();
    return new CellRecord(execution == null ? null : execution.toString(),
      result.hasActual() ? result.getActual() : null,
      result.hasExpected() ? result.getExpected() : null,
      null);
  }

  public static CellRecord error(SlimExceptionResult exception) {
    ExecutionResult execution = exception.getExecutionResult();
    return new CellRecord(execution == null ? "error" : execution.toString(),
      null, null, exception.failure());
  }

  public static CellRecord status(String status) {
    return new CellRecord(status, null, null, null);
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

  public String dump() {
    StringBuilder out = new StringBuilder();
    out.append("{\"status\":").append(quote(status));
    out.append(",\"actual\":").append(quote(actual));
    out.append(",\"expected\":").append(quote(expected));
    if (failure != null) {
      out.append(",\"error\":{\"kind\":").append(quote(failure.kind().name()));
      out.append(",\"type\":").append(quote(failure.type()));
      out.append(",\"page\":").append(quote(failure.page()));
      out.append(",\"block\":").append(quote(failure.block()));
      out.append(",\"row\":").append(failure.row() == null ? "null" : failure.row());
      out.append(",\"cell\":").append(failure.cell() == null ? "null" : failure.cell());
      out.append(",\"message\":").append(quote(failure.message()));
      out.append('}');
    }
    return out.append('}').toString();
  }

  private static String quote(String value) {
    if (value == null) {
      return "null";
    }
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
