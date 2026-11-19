package agnesse.testsystems.slim.property;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Random;

/**
 * A datetime one second before, at, or one second after a day, week, or month
 * boundary. Weeks start on Monday, the ISO-8601 week. Shrinking moves onto
 * the boundary itself.
 */
final class PeriodGen extends Generator {
  enum Unit { DAY, WEEK, MONTH }

  static final DayOfWeek WEEK_START = DayOfWeek.MONDAY;

  private final Unit unit;
  private final ZoneId zone;

  PeriodGen(Unit unit, ZoneId zone) {
    this.unit = unit;
    this.zone = zone;
  }

  @Override
  public boolean timed() {
    return true;
  }

  @Override
  public Value draw(Random random) {
    Instant boundary = TimeText.startOfDay(boundary(random), zone);
    return new TextValue(TimeText.offset(boundary.plusSeconds(random.nextInt(3) - 1L), zone));
  }

  @Override
  List<Value> simpler(Value value) {
    Instant instant = OffsetDateTime.parse(value.cell(), TimeText.OFFSET).toInstant();
    Instant boundary = nearest(instant);
    if (Math.abs(instant.getEpochSecond() - boundary.getEpochSecond()) != 1) {
      return List.of();
    }
    return List.of(new TextValue(TimeText.offset(boundary, zone)));
  }

  private LocalDate boundary(Random random) {
    return switch (unit) {
      case DAY -> TimeText.FIRST.plusDays(random.nextLong(TimeText.DAYS));
      case WEEK -> TimeText.firstMonday().plusWeeks(random.nextLong(TimeText.WEEKS));
      case MONTH -> TimeText.FIRST.plusMonths(random.nextLong(TimeText.MONTHS));
    };
  }

  private Instant nearest(Instant instant) {
    LocalDate snapped = snap(instant.atZone(zone).toLocalDate());
    Instant best = TimeText.startOfDay(shift(snapped, -1), zone);
    for (int step = 0; step <= 1; step++) {
      Instant candidate = TimeText.startOfDay(shift(snapped, step), zone);
      if (distance(instant, candidate) < distance(instant, best)) {
        best = candidate;
      }
    }
    return best;
  }

  private LocalDate snap(LocalDate date) {
    return switch (unit) {
      case DAY -> date;
      case WEEK -> date.with(TemporalAdjusters.previousOrSame(WEEK_START));
      case MONTH -> date.withDayOfMonth(1);
    };
  }

  private LocalDate shift(LocalDate date, int steps) {
    return switch (unit) {
      case DAY -> date.plusDays(steps);
      case WEEK -> date.plusWeeks(steps);
      case MONTH -> date.plusMonths(steps);
    };
  }

  private static long distance(Instant left, Instant right) {
    return Math.abs(left.getEpochSecond() - right.getEpochSecond());
  }
}
