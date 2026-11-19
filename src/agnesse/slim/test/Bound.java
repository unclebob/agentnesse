package agnesse.slim.test;

/** An int that is below a fixed line, used to show shrinking. */
public class Bound {
  private int n;

  public void setN(int n) {
    this.n = n;
  }

  public boolean below() {
    return n < 50;
  }

  public boolean belowCap() {
    if (n > 80) {
      throw new IllegalStateException("too big");
    }
    return n < 40;
  }

  /** Fails from 50 up, and a stop-test at 60 so a shrink round can be asked to halt. */
  public boolean haltAtSixty() {
    if (n == 60) {
      throw new StopTestAtSixty();
    }
    return n < 50;
  }

  /** The class name contains StopTest, which is how Slim recognizes the abort. */
  public static class StopTestAtSixty extends RuntimeException {
    public StopTestAtSixty() {
      super("halt");
    }
  }

  public int n() {
    return n;
  }
}
