package agnesse.testsystems.slim;

import agnesse.testsystems.TestExecutionException;

public class SlimCommunicationException extends TestExecutionException {
  public SlimCommunicationException(final String message, final Throwable exception) {
    super(message, exception);
  }
}
