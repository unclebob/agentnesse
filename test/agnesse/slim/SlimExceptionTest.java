package agnesse.slim;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SlimExceptionTest {
  @Test
  public void aPlainFailureNamesTheExceptionAndTheMessage() {
    String text = new SlimException("broke", "CODE").toString();

    assertTrue(text.startsWith(SlimServer.EXCEPTION_TAG + "CODE broke"));
    assertTrue(text.contains("broke"));
    assertFalse(text.contains(SlimVersion.PRETTY_PRINT_TAG_START));
  }

  @Test
  public void prettyPrintWrapsTheMessage() {
    String text = new SlimException("broke", true).toString();

    assertTrue(text.startsWith(SlimServer.EXCEPTION_TAG + SlimVersion.PRETTY_PRINT_TAG_START + "broke" + SlimVersion.PRETTY_PRINT_TAG_END));
  }

  @Test
  public void anEmptyTagAndMessageAddNothing() {
    String text = new SlimException("", (String) null, false).toString();

    assertTrue(text.startsWith(SlimServer.EXCEPTION_TAG));
    assertFalse(text.contains("null"));
  }

  @Test
  public void aBlankTagIsLeftOut() {
    String text = new SlimException("broke", "", false).toString();

    assertTrue(text.startsWith(SlimServer.EXCEPTION_TAG + "broke"));
    assertFalse(text.startsWith(SlimServer.EXCEPTION_TAG + " "));
  }

  @Test
  public void stopTestAndStopSuiteNameTheAbort() {
    String stopTest = new SlimException(new StopTestSlimException("halt")).toString();
    String stopSuite = new SlimException(new InterruptedException("halt")).toString();

    assertTrue(stopTest.startsWith(SlimServer.EXCEPTION_STOP_TEST_TAG));
    assertTrue(stopTest.contains("halt"));
    assertTrue(stopSuite.startsWith(SlimServer.EXCEPTION_STOP_SUITE_TAG));
  }

  @Test
  public void ignoreStopsBeforeTheMessage() {
    assertEquals(SlimServer.EXCEPTION_IGNORE_SCRIPT_TEST_TAG,
      new SlimException(new SlimIgnoreScriptTestException("hidden")).toString());
    assertEquals(SlimServer.EXCEPTION_IGNORE_ALL_TESTS_TAG,
      new SlimException(new SlimIgnoreAllTestsException("hidden")).toString());
  }

  @Test
  public void anEmptyStackTraceIsFilledIn() {
    SlimException bare = hiddenStack("broke", false);

    assertTrue(bare.toString().contains("broke"));
    assertTrue(bare.getStackTrace().length > 0);
  }

  @Test
  public void aNullStackTraceIsFilledIn() {
    SlimException bare = hiddenStack("broke", true);

    assertTrue(bare.toString().startsWith(SlimServer.EXCEPTION_TAG + "broke"));
    assertTrue(bare.getStackTrace().length > 0);
  }

  private static SlimException hiddenStack(String message, boolean asNull) {
    return new SlimException(message) {
      private boolean show;

      {
        show = false;
      }

      @Override
      public StackTraceElement[] getStackTrace() {
        if (!show) return asNull ? null : new StackTraceElement[0];
        return super.getStackTrace();
      }

      @Override
      public synchronized Throwable fillInStackTrace() {
        show = true;
        return super.fillInStackTrace();
      }
    };
  }
}
