package agnesse.slim.test;

/** A counter that starts at zero for each new instance. */
public class SequenceCounter {
  private int count;

  public SequenceCounter() {
  }

  public SequenceCounter(String start) {
    count = Integer.parseInt(start);
  }

  public void bump() {
    count++;
  }

  public int count() {
    return count;
  }
}
