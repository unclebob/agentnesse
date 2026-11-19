package agnesse.testsystems.slim.property;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TimeTest {
  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
  private static final Instant NOON = Instant.parse("2024-06-02T12:00:00Z");

  @Test
  public void theSameSeedIsTheSameClock() {
    Instant start = Instant.parse("2000-01-01T00:00:00Z");
    Instant end = Instant.parse("2100-01-01T00:00:00Z");
    long span = end.getEpochSecond() - start.getEpochSecond();
    assertEquals(start, Clock.fromSeed(0));
    assertEquals(start.plusSeconds(42), Clock.fromSeed(42));
    assertEquals(end.minusSeconds(1), Clock.fromSeed(span - 1));
    assertEquals(start, Clock.fromSeed(span));
    assertEquals(start.plusSeconds(Long.remainderUnsigned(-1, span)), Clock.fromSeed(-1));
    assertTrue(Clock.fromSeed(-1).isBefore(end));
    Clock clock = new Clock(UTC, Clock.fromSeed(42));
    assertEquals("2000-01-01T00:00:42Z", clock.text());
    assertEquals(UTC, clock.zone());
    assertEquals(Clock.fromSeed(42), clock.now());
  }

  @Test
  public void aClockNeedsAnInstantAndAZoneToBeShown() {
    assertThrows(IllegalArgumentException.class, () -> new Clock(UTC, null));
    IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
      () -> new Clock(null, NOON).text());
    assertTrue(missing.getMessage(), missing.getMessage().contains("zone is not configured"));
    assertNull(Clock.named(null));
    assertNull(Clock.named("  "));
    assertEquals(UTC, Clock.named("  UTC  "));
    IllegalArgumentException bad = assertThrows(IllegalArgumentException.class, () -> Clock.named("Not/AZone"));
    assertTrue(bad.getMessage(), bad.getMessage().contains("zone id"));
  }

  @Test
  public void nowAcceptsAnOffsetOrALocalDatetime() {
    assertEquals(NOON, Clock.parse("2024-06-02T12:00:00Z", null));
    assertEquals(NOON, Clock.parse(" 2024-06-02T08:00:00-04:00 ", NEW_YORK));
    assertEquals(NOON, Clock.parse("2024-06-02T08:00:00", NEW_YORK));
    assertTrue(message(() -> Clock.parse("2024-06-02T12:00:00", null)).contains("zone is not configured"));
    assertTrue(message(() -> Clock.parse("tomorrow", UTC)).contains("ISO-8601"));
    assertTrue(message(() -> Clock.parse("2024-06-02", UTC)).contains("ISO-8601"));
  }

  @Test
  public void aSinglePointDrawsThatPoint() {
    Clock clock = clock(NOON);
    assertEquals("2020-01-15", draw("date:2020-01-15..2020-01-15", clock, ordinary(0)));
    assertEquals("2020-01-17", draw("date:2020-01-15..2020-01-17", clock, ordinary(2)));
    assertEquals("09:30:00", draw("time:09:30:00..09:30:00", clock, ordinary(0)));
    assertEquals("2024-03-10T07:00:00Z",
      draw("datetime:2024-03-10T07:00:00Z..2024-03-10T07:00:00Z", clock, ordinary(0)));
    assertEquals("[2020-01-15]", draw("list of date:2020-01-15..2020-01-15 size 1..1", clock, ordinary(0)));
    assertEquals(clock.text(), draw("now", clock, ordinary(0)));
  }

  @Test
  public void defaultsStayInsideTheDocumentedWindow() {
    Clock clock = clock(NOON);
    for (int i = 0; i < 20; i++) {
      LocalDate date = LocalDate.parse(draw("date", clock, new Seed(i).random()));
      assertFalse(date.isBefore(LocalDate.of(2000, 1, 1)));
      assertFalse(date.isAfter(LocalDate.of(2099, 12, 31)));
      String time = draw("time", clock, new Seed(i).random());
      assertTrue(time.compareTo("00:00:00") >= 0);
      assertTrue(time.compareTo("23:59:59") <= 0);
      ZonedDateTime when = ZonedDateTime.parse(draw("datetime", clock, new Seed(i).random()));
      assertFalse(when.toLocalDate().isBefore(LocalDate.of(2000, 1, 1)));
      assertFalse(when.toLocalDate().isAfter(LocalDate.of(2099, 12, 31)));
    }
  }

  @Test
  public void theSameSeedDrawsTheSameTimes() {
    Clock clock = clock(NOON);
    List<String> first = sequence("datetime:2024-01-01T00:00:00Z..2024-12-31T23:59:59Z", clock, 7, 4);
    assertEquals(first, sequence("datetime:2024-01-01T00:00:00Z..2024-12-31T23:59:59Z", clock, 7, 4));
    assertEquals(4, first.size());
  }

  @Test
  public void offsetsAreExactAndDatesUseCalendarDays() {
    Clock utc = clock(Instant.parse("2024-06-01T00:00:00Z"));
    assertEquals("2024-06-02T11:59:00Z", point("datetime:now+35h59m..now+35h59m", utc));
    assertEquals("2024-06-02T12:01:00Z", point("datetime:now+36h01m..now+36h01m", utc));
    assertEquals("2024-05-31T02:00:00Z", point("datetime:now-1d+2h..now-1d+2h", utc));
    assertEquals("2024-03-11T06:00:00Z",
      point("datetime:now+1d..now+1d", clock(Instant.parse("2024-03-10T06:00:00Z"))));
    assertEquals("23:00:00", point("time:now-1h..now-1h", utc));
    assertEquals("11:00:00", point("time:now-1h..now-1h", clock(NOON)));
    assertEquals("2024-06-02T13:00:00Z", point("datetime:now +1h..now +1h", clock(NOON)));
    assertEquals("2024-06-01T01:30:00Z", point("datetime:now+2h-30m..now+2h-30m", utc));
    assertEquals(utc.text(), point("datetime:now..now", utc));
    assertEquals("2024-06-02", point("date:now+1d..now+1d", utc));
    Instant evening = Instant.parse("2024-06-02T03:00:00Z");
    assertEquals("2024-06-01", point("date:now..now", new Clock(NEW_YORK, evening)));
    assertEquals("2024-06-02", point("date:now..now", clock(evening)));
  }

  @Test
  public void aZoneClauseOverridesTheClockZone() {
    Clock utc = clock(NOON);
    assertEquals("2024-06-02T08:00:00-04:00",
      point("datetime zone:America/New_York :2024-06-02T12:00:00Z..2024-06-02T12:00:00Z", utc));
    assertEquals("2020-01-15", point("date zone: UTC : 2020-01-15 .. 2020-01-15", utc));
  }

  @Test
  public void biasedDrawsHitBoundariesAndOrdinaryDrawsDoNot() {
    Clock clock = clock(Instant.parse("1999-01-01T00:00:00Z"));
    String spec = "date:2020-01-01..2020-01-15";
    assertEquals("2020-01-03", draw(spec, clock, ordinary(2)));
    assertEquals("2020-01-01", draw(spec, clock, biased(0)));
    assertTrue(bias("date:2024-02-01..2024-03-01", clock).contains("2024-02-29"));
    assertTrue(bias("date:2024-02-01..2024-03-01", clock).contains("2024-03-01"));
    assertFalse(bias("date:2023-02-01..2023-03-01", clock).contains("2023-02-29"));
    assertTrue(bias("date:2023-02-01..2023-03-01", clock).contains("2023-02-28"));
    assertTrue(bias("date:2024-12-15..2025-01-15", clock).contains("2024-12-31"));
    assertTrue(bias("date:2024-12-15..2025-01-15", clock).contains("2025-01-01"));
  }

  @Test
  public void datetimeBiasIncludesTheDstTransitionAndTheLeapDay() {
    Clock clock = new Clock(NEW_YORK, Instant.parse("2024-01-01T00:00:00Z"));
    String spring = "datetime zone:America/New_York :2024-03-10T06:59:00Z..2024-03-10T07:01:00Z";
    assertEquals(Set.of(
      "2024-03-10T01:59:59-05:00",
      "2024-03-10T03:00:00-04:00",
      "2024-03-10T03:00:01-04:00"), bias(spring, clock));
    assertEquals("2024-03-10T01:59:00-05:00", draw(spring, clock, ordinary(0)));
    String leap = "datetime:2024-02-29T00:00:00Z..2024-02-29T00:00:01Z";
    assertTrue(bias(leap, clock(Instant.parse("2024-01-01T00:00:00Z"))).contains("2024-02-29T00:00:00Z"));
  }

  @Test
  public void timeBiasUsesTheLocalClockOfATransition() {
    Clock clock = new Clock(NEW_YORK, NOON);
    Set<String> hits = bias("time zone:America/New_York :01:00:00..03:30:00", clock);
    assertTrue(hits.contains("01:59:59"));
    assertTrue(hits.contains("02:00:00"));
    assertTrue(hits.contains("03:00:00"));
    assertTrue(hits.contains("03:00:01"));
    assertEquals("09:00:00", draw("time:09:00:00..17:00:00", clock(NOON), ordinary(0)));
    Set<String> gap = bias("time zone:America/New_York :02:10:00..02:20:00", clock);
    assertFalse(gap.contains("01:59:59"));
    assertFalse(gap.contains("03:00:01"));
  }

  @Test
  public void shrinkingMovesTowardTheRangeStartAndWholeUnits() {
    Clock outside = clock(Instant.parse("2024-06-01T00:00:00Z"));
    Generator deadline = CellLanguage.parse("datetime:2024-06-02T11:59:00Z..2024-06-02T12:01:00Z", outside);
    assertEquals(List.of(
      "2024-06-02T11:59:00Z",
      "2024-06-02T12:00:00Z",
      "2024-06-02T11:59:45Z",
      "2024-06-02T12:00:29Z"), cells(deadline, "2024-06-02T12:00:30Z"));
    assertTrue(cells(deadline, "2024-06-02T11:59:00Z").isEmpty());

    Clock inside = clock(NOON);
    Generator window = CellLanguage.parse("datetime:2024-06-02T11:00:00Z..2024-06-02T13:00:00Z", inside);
    assertEquals(List.of(
      "2024-06-02T12:00:00Z",
      "2024-06-02T11:31:00Z",
      "2024-06-02T11:45:00Z",
      "2024-06-02T11:30:01Z"), cells(window, "2024-06-02T11:30:00Z"));

    Clock later = clock(Instant.parse("2024-06-02T13:30:00Z"));
    Generator afternoon = CellLanguage.parse("datetime:2024-06-02T11:00:00Z..2024-06-02T14:00:00Z", later);
    assertEquals(List.of(
      "2024-06-02T13:30:00Z",
      "2024-06-02T12:00:00Z",
      "2024-06-02T11:31:00Z",
      "2024-06-02T12:30:00Z",
      "2024-06-02T11:30:01Z"), cells(afternoon, "2024-06-02T11:30:00Z"));

    Clock may = clock(Instant.parse("2024-05-15T00:00:00Z"));
    Generator month = CellLanguage.parse("datetime:2024-05-01T00:00:00Z..2024-06-30T23:59:59Z", may);
    assertEquals(List.of(
      "2024-05-15T00:00:00Z",
      "2024-06-01T00:00:00Z",
      "2024-06-01T23:00:00Z",
      "2024-06-01T23:59:00Z",
      "2024-05-24T00:00:00Z",
      "2024-06-01T23:59:59Z"), cells(month, "2024-06-02T00:00:00Z"));
  }

  @Test
  public void shrinkingStaysInsideTheRange() {
    Generator times = CellLanguage.parse("time:10:00:00..11:00:00", clock(Instant.parse("2024-06-02T08:00:00Z")));
    assertEquals(List.of("10:00:00"), cells(times, "12:30:00"));
    Generator towardNoon = CellLanguage.parse("time:10:00:00..14:00:00", clock(NOON));
    assertEquals(List.of("12:00:00", "10:00:00", "10:45:00"), cells(towardNoon, "09:30:00"));
    Generator aligned = CellLanguage.parse("time:10:30:00..14:00:00", clock(Instant.parse("2024-06-02T10:45:00Z")));
    assertEquals(List.of("10:45:00", "10:59:00", "10:52:30", "10:59:59"), cells(aligned, "11:00:00"));
    Generator backOne = CellLanguage.parse("time:10:00:00..11:00:00", clock(Instant.parse("2024-06-02T10:00:00Z")));
    assertEquals(List.of("10:00:00"), cells(backOne, "10:00:01"));
    Generator forwardOne = CellLanguage.parse("time:10:00:00..11:00:00", clock(Instant.parse("2024-06-02T10:00:01Z")));
    assertEquals(List.of("10:00:01"), cells(forwardOne, "10:00:00"));
    Generator over = CellLanguage.parse("time:10:00:00..12:00:00", clock(Instant.parse("2024-06-02T10:30:00Z")));
    assertEquals(List.of("10:30:00", "10:01:00", "10:15:00", "10:00:01"), cells(over, "10:00:00"));
    Generator undershoot = CellLanguage.parse("time:10:00:00..12:00:00", clock(Instant.parse("2024-06-02T10:15:00Z")));
    assertEquals(List.of("10:15:00", "10:29:00", "10:22:30", "10:29:59"), cells(undershoot, "10:30:00"));
  }

  @Test
  public void datesAndTimesShrinkTowardNowWhenItIsInside() {
    Clock early = clock(Instant.parse("1999-06-01T00:00:00Z"));
    Generator dates = CellLanguage.parse("date:2020-01-01..2020-12-31", early);
    assertEquals(List.of("2020-01-01", "2020-06-01", "2020-03-24", "2020-06-14"),
      cells(dates, "2020-06-15"));
    Generator later = CellLanguage.parse("date:2020-01-01..2020-12-31", clock(Instant.parse("2020-06-15T00:00:00Z")));
    assertEquals(List.of("2020-06-15", "2020-04-01", "2020-04-23", "2020-03-02"),
      cells(later, "2020-03-01"));
    Generator years = CellLanguage.parse("date:2018-01-01..2021-12-31", clock(Instant.parse("2021-06-01T00:00:00Z")));
    assertEquals(List.of("2021-06-01", "2021-01-01", "2020-04-01", "2020-10-15", "2020-03-02"),
      cells(years, "2020-03-01"));
    Generator back = CellLanguage.parse("date:2018-01-01..2020-12-31", clock(Instant.parse("2018-06-01T00:00:00Z")));
    assertEquals(List.of("2018-06-01", "2019-01-01", "2019-12-01", "2019-03-18", "2019-12-31"),
      cells(back, "2020-01-01"));
    Generator months = CellLanguage.parse("date:2020-01-01..2020-12-31", clock(Instant.parse("2020-03-15T00:00:00Z")));
    assertEquals(List.of("2020-03-15", "2020-05-01", "2020-04-23", "2020-05-31"),
      cells(months, "2020-06-01"));

    Generator times = CellLanguage.parse("time:10:00:00..14:00:00", clock(Instant.parse("2024-06-02T12:00:00Z")));
    assertEquals(List.of("12:00:00", "11:00:00", "10:31:00", "11:15:22", "10:30:46"),
      cells(times, "10:30:45"));
    Generator morning = CellLanguage.parse("time:10:00:00..14:00:00", clock(Instant.parse("2024-06-02T08:00:00Z")));
    assertEquals(List.of("10:00:00", "12:00:00", "11:00:15", "12:00:29"), cells(morning, "12:00:30"));
  }

  @Test
  public void aPeriodDrawsJustAroundABoundaryAndShrinksOntoIt() {
    Clock clock = clock(NOON);
    assertEquals("2000-01-03T00:00:00Z", draw("period:week", clock, fixed(0, 1)));
    assertEquals("2000-01-02T23:59:59Z", draw("period:week", clock, fixed(0, 0)));
    assertEquals("2000-01-03T00:00:01Z", draw("period:week", clock, fixed(0, 2)));
    assertEquals(java.time.DayOfWeek.MONDAY, LocalDate.parse("2000-01-03").getDayOfWeek());
    Generator weeks = CellLanguage.parse("period:week", clock);
    assertEquals(List.of("2000-01-03T00:00:00Z"), cells(weeks, "2000-01-02T23:59:59Z"));
    assertEquals(List.of("2000-01-03T00:00:00Z"), cells(weeks, "2000-01-03T00:00:01Z"));
    assertTrue(cells(weeks, "2000-01-03T00:00:00Z").isEmpty());
    assertTrue(cells(weeks, "2000-01-01T12:00:00Z").isEmpty());
    assertEquals("2000-01-01T00:00:00Z", draw("period:day", clock, fixed(0, 1)));
    assertEquals("2000-01-01T00:00:00Z", draw("period:month", clock, fixed(0, 1)));
    Generator days = CellLanguage.parse("period:day", clock);
    assertEquals(List.of("2000-01-02T00:00:00Z"), cells(days, "2000-01-01T23:59:59Z"));
    assertEquals(List.of("2000-01-02T00:00:00Z"), cells(days, "2000-01-02T00:00:01Z"));
    Generator months = CellLanguage.parse("period:month", clock);
    assertEquals(List.of("2000-02-01T00:00:00Z"), cells(months, "2000-01-31T23:59:59Z"));
    assertEquals(List.of("2000-02-01T00:00:00Z"), cells(months, "2000-02-01T00:00:01Z"));
    assertEquals("2000-02-01T00:00:00-05:00",
      draw("period:month zone:America/New_York", new Clock(UTC, NOON), fixed(1, 1)));
  }

  @Test
  public void nowDoesNotShrinkAndAListOfTimesIsTimed() {
    Clock clock = clock(NOON);
    assertTrue(cells(CellLanguage.parse("now", clock), clock.text()).isEmpty());
    assertTrue(CellLanguage.parse("now", clock).timed());
    assertTrue(CellLanguage.parse("list of datetime:2020-01-01T00:00:00Z..2020-01-01T00:00:00Z", clock).timed());
    assertTrue(CellLanguage.parse("period:day", clock).timed());
    assertFalse(CellLanguage.parse("int").timed());
    assertFalse(CellLanguage.parse("list of int:0..1").timed());
  }

  @Test
  public void badTimeSpecsNameTheProblem() {
    Clock clock = clock(NOON);
    assertTrue(message(() -> CellLanguage.parse("date", null)).contains("zone is not configured"));
    assertTrue(message(() -> CellLanguage.parse("datetime zone:UTC", null)).contains("zone is not configured"));
    assertTrue(message(() -> CellLanguage.parse("now", new Clock(null, NOON))).contains("zone is not configured"));
    assertTrue(message(() -> CellLanguage.parse("date zone:Nope", clock)).contains("zone id"));
    assertTrue(message(() -> CellLanguage.parse("date zone", clock)).contains("expected :"));
    assertTrue(message(() -> CellLanguage.parse("period day", clock)).contains("expected :"));
    assertTrue(message(() -> CellLanguage.parse("period:year", clock)).contains("day, week, or month"));
    assertTrue(message(() -> CellLanguage.parse("datetime:", clock)).contains("expected a bound"));
    assertTrue(message(() -> CellLanguage.parse("datetime:now+..now", clock)).contains("expected a duration"));
    assertTrue(message(() -> CellLanguage.parse("datetime:now+35..now", clock)).contains("expected a duration"));
    assertTrue(message(() -> CellLanguage.parse("date:now+1h..now", clock)).contains("date offset uses d"));
    assertTrue(message(() -> CellLanguage.parse("date:now+1m..now", clock)).contains("date offset uses d"));
    assertTrue(message(() -> CellLanguage.parse("date:now+1s..now", clock)).contains("date offset uses d"));
    assertTrue(message(() -> CellLanguage.parse("datetime:now+h..now", clock)).contains("expected a duration"));
    assertTrue(message(() -> CellLanguage.parse("date:nope..2020-01-01", clock)).contains("expected a date"));
    assertTrue(message(() -> CellLanguage.parse("time:25:00:00..26:00:00", clock)).contains("expected a time"));
    assertTrue(message(() -> CellLanguage.parse("datetime:nope..2020-01-01T00:00:00Z", clock)).contains("expected a datetime"));
    assertTrue(message(() -> CellLanguage.parse("datetime:2020-01-02T00:00:00Z..2020-01-01T00:00:00Z", clock))
      .contains("inverted range"));
    assertTrue(message(() -> CellLanguage.parse("time:now-2h..now+1h", clock(Instant.parse("2024-06-01T00:30:00Z"))))
      .contains("inverted range"));
    assertTrue(message(() -> CellLanguage.parse("datetime:now+99999999999999999999s..now", clock))
      .contains("duration is too large"));
    assertTrue(message(() -> CellLanguage.parse("datetime:now+9223372036854775807s1s..now", clock))
      .contains("duration is too large"));
    assertTrue(message(() -> CellLanguage.parse("datetime:now+9223372036854775807s..now", clock))
      .contains("duration is too large"));
    assertTrue(message(() -> CellLanguage.parse("datetime:now+9223372036854775807d..now", clock))
      .contains("duration is too large"));
    assertTrue(message(() -> CellLanguage.parse("date:now+1000000000000d..now", clock)).contains("date is out of range"));
    assertTrue(message(() -> CellLanguage.parse("datetime:now+35h59m", clock)).contains("expected .."));
    assertTrue(message(() -> CellLanguage.parse("time:09:00:00..", clock)).contains("expected a bound"));
    assertTrue(message(() -> Moments.shift(NOON, "x")).contains("expected a duration"));
    assertTrue(message(() -> Moments.shift(NOON, "+")).contains("expected a duration"));
    assertTrue(message(() -> Moments.bound(MomentGen.Kind.DATE, "now+", UTC, NOON))
      .contains("expected a duration"));
    assertTrue(message(() -> Moments.bound(MomentGen.Kind.DATETIME, "now+1x", UTC, NOON))
      .contains("expected a duration"));
    assertTrue(message(() -> Clock.parse("   ", UTC)).contains("ISO-8601"));
  }

  @Test
  public void timeCodeDoesNotReadTheSystemClock() throws Exception {
    try (Stream<Path> files = Files.list(Path.of("src/agnesse/testsystems/slim/property"))) {
      for (Path file : files.toList()) {
        String text = Files.readString(file);
        assertFalse(file.toString(), text.contains("Instant.now("));
        assertFalse(file.toString(), text.contains("systemDefault"));
        assertFalse(file.toString(), text.contains("LocalDate.now("));
        assertFalse(file.toString(), text.contains("LocalTime.now("));
        assertFalse(file.toString(), text.contains("LocalDateTime.now("));
        assertFalse(file.toString(), text.contains("ZonedDateTime.now("));
        assertFalse(file.toString(), text.contains("OffsetDateTime.now("));
        assertFalse(file.toString(), text.contains("Clock.system"));
      }
    }
  }

  private static String message(Throwing call) {
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, call::run);
    return error.getMessage();
  }

  private static Clock clock(Instant now) {
    return new Clock(UTC, now);
  }

  private static String point(String spec, Clock clock) {
    return draw(spec, clock, ordinary(0));
  }

  private static String draw(String spec, Clock clock, Random random) {
    return CellLanguage.parse(spec, clock).draw(random).cell();
  }

  private static List<String> sequence(String spec, Clock clock, long seed, int count) {
    Random random = new Seed(seed).random();
    Generator generator = CellLanguage.parse(spec, clock);
    List<String> out = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      out.add(generator.draw(random).cell());
    }
    return out;
  }

  private static Set<String> bias(String spec, Clock clock) {
    Set<String> seen = new HashSet<>();
    for (int index = 0; index < 32; index++) {
      seen.add(draw(spec, clock, biased(index)));
    }
    return seen;
  }

  private static List<String> cells(Generator generator, String current) {
    List<String> out = new ArrayList<>();
    for (Value value : generator.simpler(new TextValue(current))) {
      out.add(value.cell());
    }
    return out;
  }

  private static Random ordinary(long number) {
    return new Fixed(false, number, 0);
  }

  private static Random biased(int index) {
    return new Fixed(true, 0, index);
  }

  private static Random fixed(long number, int index) {
    return new Fixed(false, number, index);
  }

  @FunctionalInterface
  private interface Throwing {
    void run();
  }

  private static final class Fixed extends Random {
    private final boolean bias;
    private final long number;
    private final int index;

    private Fixed(boolean bias, long number, int index) {
      this.bias = bias;
      this.number = number;
      this.index = index;
    }

    @Override
    public boolean nextBoolean() {
      return bias;
    }

    @Override
    public long nextLong(long bound) {
      return Math.floorMod(number, bound);
    }

    @Override
    public int nextInt(int bound) {
      return Math.floorMod(index, bound);
    }
  }
}
