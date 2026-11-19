package agnesse.testsystems.slim.property;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Shrink steps toward a target, most aggressive first, staying inside the range. */
final class Closer {
  private Closer() {
  }

  static List<Long> days(long current, long target, long low, long high) {
    List<Long> out = new ArrayList<>();
    add(out, current, target, low, high, target);
    add(out, current, target, low, high, yearToward(current, target));
    add(out, current, target, low, high, monthToward(current, target));
    add(out, current, target, low, high, half(current, target));
    add(out, current, target, low, high, step(current, target));
    return out;
  }

  static List<Long> seconds(long current, long target, long low, long high) {
    List<Long> out = new ArrayList<>();
    add(out, current, target, low, high, target);
    add(out, current, target, low, high, unitToward(current, target, 3600));
    add(out, current, target, low, high, unitToward(current, target, 60));
    add(out, current, target, low, high, half(current, target));
    add(out, current, target, low, high, step(current, target));
    return out;
  }

  static List<Long> instants(long current, long target, long low, long high, ZoneId zone) {
    List<Long> out = new ArrayList<>();
    add(out, current, target, low, high, target);
    add(out, current, target, low, high, align(current, target, zone, ChronoUnit.DAYS));
    add(out, current, target, low, high, align(current, target, zone, ChronoUnit.HOURS));
    add(out, current, target, low, high, align(current, target, zone, ChronoUnit.MINUTES));
    add(out, current, target, low, high, half(current, target));
    add(out, current, target, low, high, step(current, target));
    return out;
  }

  private static void add(List<Long> out, long current, long target, long low, long high, long candidate) {
    if (candidate < low || candidate > high) {
      return;
    }
    if (!between(candidate, current, target) || out.contains(candidate)) {
      return;
    }
    out.add(candidate);
  }

  private static boolean between(long candidate, long current, long target) {
    if (target < current) {
      return candidate >= target && candidate < current;
    }
    return candidate <= target && candidate > current;
  }

  private static long yearToward(long current, long target) {
    return calendar(current, target, true);
  }

  private static long monthToward(long current, long target) {
    return calendar(current, target, false);
  }

  private static long calendar(long current, long target, boolean year) {
    LocalDate date = LocalDate.ofEpochDay(current);
    LocalDate snapped = year ? date.withDayOfYear(1) : date.withDayOfMonth(1);
    snapped = stepCalendar(current, target, snapped, year);
    return snapped.toEpochDay();
  }

  private static LocalDate stepCalendar(long current, long target, LocalDate snapped, boolean year) {
    long at = snapped.toEpochDay();
    if (target < current && at == current) {
      return year ? snapped.minusYears(1) : snapped.minusMonths(1);
    }
    if (target > current) {
      return year ? snapped.plusYears(1) : snapped.plusMonths(1);
    }
    return snapped;
  }

  /** Floor division never lands past {@code current}, so equality is the boundary case. */
  private static long unitToward(long current, long target, long unit) {
    long truncated = Math.floorDiv(current, unit) * unit;
    if (target < current && truncated == current) {
      return truncated - unit;
    }
    if (target > current) {
      return truncated + unit;
    }
    return truncated;
  }

  private static long align(long current, long target, ZoneId zone, ChronoUnit unit) {
    ZonedDateTime truncated = Instant.ofEpochSecond(current).atZone(zone).truncatedTo(unit);
    long at = truncated.toEpochSecond();
    if (target < current && at == current) {
      return truncated.minus(1, unit).toEpochSecond();
    }
    if (target > current) {
      return truncated.plus(1, unit).toEpochSecond();
    }
    return at;
  }

  private static long half(long current, long target) {
    return current + (target - current) / 2;
  }

  private static long step(long current, long target) {
    if (target == current) {
      return current;
    }
    return current + (target > current ? 1 : -1);
  }
}
