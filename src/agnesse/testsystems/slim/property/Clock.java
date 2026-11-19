package agnesse.testsystems.slim.property;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * The fixed clock of one property run. {@code now} is an explicit instant,
 * or {@link #fromSeed} when the page leaves it blank. The zone comes from
 * configuration, or is null when none was set. Nothing here reads the
 * machine clock or the machine zone.
 */
public final class Clock {
  private static final Instant SEED_START = Instant.parse("2000-01-01T00:00:00Z");
  private static final Instant SEED_END = Instant.parse("2100-01-01T00:00:00Z");

  private final ZoneId zone;
  private final Instant now;

  public Clock(ZoneId zone, Instant now) {
    if (now == null) {
      throw new IllegalArgumentException("now is missing");
    }
    this.zone = zone;
    this.now = now;
  }

  public ZoneId zone() {
    return zone;
  }

  public Instant now() {
    return now;
  }

  /** The clock as an ISO-8601 offset datetime in the configured zone. */
  public String text() {
    if (zone == null) {
      throw new IllegalArgumentException("zone is not configured");
    }
    return TimeText.offset(now, zone);
  }

  /**
   * {@code 2000-01-01T00:00:00Z} plus {@code seed} modulo the seconds until
   * {@code 2100-01-01T00:00:00Z}. The same seed is the same instant.
   */
  public static Instant fromSeed(long seed) {
    long span = SEED_END.getEpochSecond() - SEED_START.getEpochSecond();
    return SEED_START.plusSeconds(Long.remainderUnsigned(seed, span));
  }

  public static ZoneId named(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    try {
      return ZoneId.of(name.trim());
    } catch (DateTimeException e) {
      throw new IllegalArgumentException("zone is not a zone id");
    }
  }

  public static Instant parse(String text, ZoneId zone) {
    try {
      return TimeText.instant(text.trim(), zone);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("now must be an ISO-8601 datetime");
    }
  }
}
