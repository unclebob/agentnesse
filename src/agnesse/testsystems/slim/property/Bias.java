package agnesse.testsystems.slim.property;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Boundary points mixed into ordinary draws. Dates use month ends, the first
 * of each month, and leap days. Times use the local clock times of DST
 * transitions. Datetimes use those instants, one second either side, plus
 * month boundaries and leap-day midnights.
 */
final class Bias {
  private Bias() {
  }

  static long[] dates(long low, long high) {
    List<Long> out = new ArrayList<>();
    LocalDate end = LocalDate.ofEpochDay(high);
    LocalDate month = LocalDate.ofEpochDay(low).withDayOfMonth(1);
    while (!month.isAfter(end)) {
      addDay(out, month, low, high);
      addDay(out, month.withDayOfMonth(month.lengthOfMonth()), low, high);
      month = month.plusMonths(1);
    }
    return unique(out);
  }

  static long[] times(ZoneId zone, int low, int high) {
    List<Long> out = new ArrayList<>();
    walk(zone, (transition) -> addClock(out, transition, low, high));
    return unique(out);
  }

  static long[] instants(ZoneId zone, long low, long high) {
    List<Long> out = new ArrayList<>();
    Instant start = Instant.ofEpochSecond(low);
    Instant end = Instant.ofEpochSecond(high);
    addMonths(out, zone, start, end);
    walk(zone, (transition) -> addTransition(out, transition.getInstant(), start, end));
    return unique(out);
  }

  private static void addMonths(List<Long> out, ZoneId zone, Instant low, Instant high) {
    LocalDate end = high.atZone(zone).toLocalDate();
    LocalDate month = low.atZone(zone).toLocalDate().withDayOfMonth(1);
    while (!month.isAfter(end)) {
      addPoint(out, month, LocalTime.MIDNIGHT, zone, low, high);
      addPoint(out, month.withDayOfMonth(month.lengthOfMonth()), LocalTime.of(23, 59, 59), zone, low, high);
      if (month.isLeapYear() && month.getMonthValue() == 2) {
        addPoint(out, LocalDate.of(month.getYear(), 2, 29), LocalTime.MIDNIGHT, zone, low, high);
      }
      month = month.plusMonths(1);
    }
  }

  private static void walk(ZoneId zone, Transition consumer) {
    ZoneRules rules = zone.getRules();
    Instant cursor = TimeText.startOfDay(TimeText.FIRST, zone).minusSeconds(1);
    Instant stop = TimeText.LAST.atTime(23, 59, 59).atZone(zone).toInstant().plusSeconds(1);
    while (true) {
      ZoneOffsetTransition transition = rules.nextTransition(cursor);
      if (transition == null || transition.getInstant().isAfter(stop)) {
        return;
      }
      consumer.accept(transition);
      cursor = transition.getInstant();
    }
  }

  private static void addClock(List<Long> out, ZoneOffsetTransition transition, int low, int high) {
    addAround(out, transition.getDateTimeBefore().toLocalTime(), low, high);
    addAround(out, transition.getDateTimeAfter().toLocalTime(), low, high);
  }

  private static void addAround(List<Long> out, LocalTime time, int low, int high) {
    addSecond(out, time.minusSeconds(1), low, high);
    addSecond(out, time, low, high);
    addSecond(out, time.plusSeconds(1), low, high);
  }

  private static void addTransition(List<Long> out, Instant at, Instant low, Instant high) {
    addInstant(out, at.minusSeconds(1), low, high);
    addInstant(out, at, low, high);
    addInstant(out, at.plusSeconds(1), low, high);
  }

  private static void addPoint(List<Long> out, LocalDate date, LocalTime time, ZoneId zone, Instant low, Instant high) {
    addInstant(out, date.atTime(time).atZone(zone).toInstant(), low, high);
  }

  private static void addInstant(List<Long> out, Instant instant, Instant low, Instant high) {
    if (!instant.isBefore(low) && !instant.isAfter(high)) {
      out.add(instant.getEpochSecond());
    }
  }

  private static void addDay(List<Long> out, LocalDate date, long low, long high) {
    long day = date.toEpochDay();
    if (day >= low && day <= high) {
      out.add(day);
    }
  }

  private static void addSecond(List<Long> out, LocalTime time, int low, int high) {
    int second = time.toSecondOfDay();
    if (second >= low && second <= high) {
      out.add((long) second);
    }
  }

  private static long[] unique(List<Long> values) {
    TreeSet<Long> sorted = new TreeSet<>(values);
    long[] out = new long[sorted.size()];
    int index = 0;
    for (Long value : sorted) {
      out[index] = value;
      index++;
    }
    return out;
  }

  @FunctionalInterface
  private interface Transition {
    void accept(ZoneOffsetTransition transition);
  }
}
