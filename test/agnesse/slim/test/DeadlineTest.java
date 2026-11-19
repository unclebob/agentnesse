package agnesse.slim.test;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeadlineTest {
  @Test
  public void theDeadlineIsThirtySixHoursFromTheClock() {
    Deadline deadline = new Deadline();
    deadline.setNow("2024-06-01T00:00:00Z");
    deadline.setAt("2024-06-02T11:59:00Z");
    assertFalse(deadline.past());
    deadline.setAt("2024-06-02T12:00:00Z");
    assertTrue(deadline.past());
    deadline.setAt("2024-06-02T12:01:00Z");
    assertTrue(deadline.past());
  }
}
