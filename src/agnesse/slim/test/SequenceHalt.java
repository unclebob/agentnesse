package agnesse.slim.test;

/** Stops the test. The class name contains StopTest, which is how Slim recognizes the abort. */
public class SequenceHalt {
  public void halt() {
    throw new HaltStopTest();
  }

  public void suite() {
    throw new HaltStopSuite();
  }

  public static class HaltStopTest extends RuntimeException {
  }

  public static class HaltStopSuite extends RuntimeException {
  }
}
