package agnesse.testsystems.slim.tables;

import agnesse.testsystems.TestExecutionException;

public class SyntaxError extends TestExecutionException {
  private static final long serialVersionUID = 1L;

  public SyntaxError(String message) {
    super(message);
  }
}
