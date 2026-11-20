package agnesse.slim.test;

/** A command that throws an ordinary exception. */
public class SequenceBoom {
  public void boom() {
    throw new IllegalStateException("boom");
  }
}
