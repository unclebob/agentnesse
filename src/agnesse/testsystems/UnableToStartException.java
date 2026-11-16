package agnesse.testsystems;

public class UnableToStartException extends Exception {
  public UnableToStartException(final String message, final Exception exception) {
    super(message, exception);
  }
}
