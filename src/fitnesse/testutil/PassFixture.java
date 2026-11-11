package fitnesse.testutil;

/**
 * A one-row table constructs this class and counts as a passing test.
 * Acceptance pages use it as a stand-in for a trivial passing test.
 */
public class PassFixture {
  public boolean ok() {
    return true;
  }
}
