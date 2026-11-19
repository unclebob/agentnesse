package agnesse.slim.test;

import java.time.OffsetDateTime;

/** A 36-hour deadline measured from a clock the property table supplies. */
public class Deadline {
  private OffsetDateTime clock = OffsetDateTime.parse("2000-01-01T00:00:00Z");
  private OffsetDateTime at = clock;

  public void setNow(String now) {
    clock = OffsetDateTime.parse(now);
  }

  public void setAt(String at) {
    this.at = OffsetDateTime.parse(at);
  }

  /** True when {@code at} is at or after the clock plus 36 hours. */
  public boolean past() {
    return !at.isBefore(clock.plusHours(36));
  }
}
