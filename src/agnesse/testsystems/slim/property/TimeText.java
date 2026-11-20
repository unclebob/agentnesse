package agnesse.testsystems.slim.property;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** ISO-8601 cells at second resolution, and the documented default window. */
final class TimeText {
  static final LocalDate FIRST = LocalDate.of(2000, 1, 1);
  static final LocalDate LAST = LocalDate.of(2099, 12, 31);
  static final long DAYS = ChronoUnit.DAYS.between(FIRST, LAST) + 1;
  static final long MONTHS = ChronoUnit.MONTHS.between(FIRST, LAST.withDayOfMonth(1)) + 1;
  static final long WEEKS = ChronoUnit.WEEKS.between(firstWeek(), lastWeek()) + 1;
  static final DateTimeFormatter OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
  static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

  private TimeText() {
  }

  static LocalDate firstWeek() {
    return FIRST.with(TemporalAdjusters.nextOrSame(PeriodGen.WEEK_START));
  }

  static LocalDate lastWeek() {
    return LAST.with(TemporalAdjusters.previousOrSame(PeriodGen.WEEK_START));
  }

  static String date(LocalDate value) {
    return value.toString();
  }

  static String time(LocalTime value) {
    return TIME.format(value);
  }

  static String offset(Instant instant, ZoneId zone) {
    return OFFSET.format(instant.atZone(zone));
  }

  static Instant startOfDay(LocalDate date, ZoneId zone) {
    return date.atStartOfDay(zone).toInstant();
  }

  static long windowStart(ZoneId zone) {
    return startOfDay(FIRST, zone).getEpochSecond();
  }

  static long windowEnd(ZoneId zone) {
    return LAST.atTime(23, 59, 59).atZone(zone).toInstant().getEpochSecond();
  }

  /** An offset datetime, or a local datetime in {@code zone}. */
  static Instant instant(String text, ZoneId zone) {
    try {
      return OffsetDateTime.parse(text, OFFSET).toInstant();
    } catch (DateTimeParseException ignored) {
      return local(text, zone);
    }
  }

  private static Instant local(String text, ZoneId zone) {
    if (zone == null) {
      throw new IllegalArgumentException("zone is not configured");
    }
    return LocalDateTime.parse(text, LOCAL).atZone(zone).toInstant();
  }
}
