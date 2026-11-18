package agnesse.testsystems;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestSummaryCopyTest {
  @Test
  public void aCopyKeepsTheCountsUntilItIsCleared() {
    TestSummary original = new TestSummary(1, 2, 3, 4);
    original.setRunTimeInMillis(9);
    TestSummary copy = new TestSummary(original);

    assertEquals(original, copy);
    assertEquals("1 right, 2 wrong, 3 ignored, 4 exceptions", copy.toString());
    assertFalse(original.equals(null));
    assertFalse(original.equals("1 right"));

    copy.clear();
    assertEquals(new TestSummary(), copy);
    try {
      copy.hashCode();
    } catch (AssertionError e) {
      assertEquals("hashCode not designed", e.getMessage());
    }
  }
}
