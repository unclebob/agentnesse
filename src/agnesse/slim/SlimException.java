package agnesse.slim;

public class SlimException extends Exception {
  private final String tag;
  private final boolean prettyPrint;

  public SlimException(String message) {
    this(message, "", false);
  }

  public SlimException(String message, boolean prettyPrint) {
    this(message, "", prettyPrint);
  }

  public SlimException(String message, String tag) {
    this(message, tag, false);
  }

  public SlimException(String message, String tag, boolean prettyPrint) {
    super(message);
    this.tag = tag;
    this.prettyPrint = prettyPrint;
  }

  public SlimException(Throwable cause) {
    this(cause, "", false);
  }

  public SlimException(Throwable cause, boolean prettyPrint) {
    this(cause, "", prettyPrint);
  }

  public SlimException(Throwable cause, String tag) {
    this(cause, tag, false);
  }

  public SlimException(Throwable cause, String tag, boolean prettyPrint) {
    super(cause);
    this.tag = tag;
    this.prettyPrint = prettyPrint;
  }

  public SlimException(String message, Throwable cause) {
    this(message, cause, false);
  }

  public SlimException(String message, Throwable cause, boolean prettyPrint) {
    this(message, cause, "", prettyPrint);
  }

  public SlimException(String message, Throwable cause, String tag) {
    this(message, cause, tag, false);
  }

  public SlimException(String message, Throwable cause, String tag, boolean prettyPrint) {
    super(message, cause);
    this.tag = tag;
    this.prettyPrint = prettyPrint;
  }

  public String getTag() {
    return tag;
  }

  public boolean prettyPrint() {
    return prettyPrint;
  }

  /*
  TODO Move this to the serialization and clean it up.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (ignored(sb)) return sb.toString();
    appendTag(sb);
    appendDetail(sb);
    appendTrace(sb);
    return sb.toString();
  }

  private boolean ignored(StringBuilder sb) {
    Throwable cause = getCause();
    if (isIgnoreScriptTestException(cause)) {
      sb.append(SlimServer.EXCEPTION_IGNORE_SCRIPT_TEST_TAG);
      return true;
    }
    if (isIgnoreAllTestsException(cause)) {
      sb.append(SlimServer.EXCEPTION_IGNORE_ALL_TESTS_TAG);
      return true;
    }
    return false;
  }

  private void appendTag(StringBuilder sb) {
    Throwable cause = getCause();
    if (isStopTestException(cause)) {
      sb.append(SlimServer.EXCEPTION_STOP_TEST_TAG);
    } else if (isStopSuiteException(cause)) {
      sb.append(SlimServer.EXCEPTION_STOP_SUITE_TAG);
    } else {
      sb.append(SlimServer.EXCEPTION_TAG);
    }
  }

  private void appendDetail(StringBuilder sb) {
    if (prettyPrint) sb.append(SlimVersion.PRETTY_PRINT_TAG_START);
    appendText(sb, tag, " ");
    appendText(sb, getMessage(), "");
    if (prettyPrint) sb.append(SlimVersion.PRETTY_PRINT_TAG_END);
  }

  private void appendText(StringBuilder sb, String text, String suffix) {
    if (text != null && !text.isEmpty()) {
      sb.append(text).append(suffix);
    }
  }

  private void appendTrace(StringBuilder sb) {
    Throwable cause = getCause();
    Throwable traced = cause == null ? this : cause;
    sb.append(new StackTraceEnricher().getStackTraceAsString(traced));
  }

  public static boolean isStopTestException(Throwable t) {
    return t != null && t.getClass().toString().contains("StopTest");
  }

  public static boolean isStopSuiteException(Throwable t) {
    return t != null && t.getClass().toString().contains("StopSuite") || t instanceof InterruptedException;
  }

  public static boolean isIgnoreScriptTestException(Throwable t) {
    return t != null && t.getClass().toString().contains("IgnoreScriptTest");
  }

  public static boolean isIgnoreAllTestsException(Throwable t) {
    return t != null && t.getClass().toString().contains("IgnoreAllTests");
  }
}
