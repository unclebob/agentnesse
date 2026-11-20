package agnesse.testsystems.slim.sequence;

/** A fixture call failed before a reading could be recorded. */
public final class CallBroke extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public CallBroke(String message) {
    super(message);
  }
}
