package agnesse.slim.test;

/** Returns no, and aborts when the amount shrinks to 1. */
public class SequenceStopOnOne {
  public String take(String amount) {
    if ("1".equals(amount)) {
      throw new StopTestOnOne();
    }
    return "no";
  }

  public static class StopTestOnOne extends RuntimeException {
  }
}
