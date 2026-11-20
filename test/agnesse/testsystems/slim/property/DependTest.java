package agnesse.testsystems.slim.property;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DependTest {
  private static final Clock CLOCK = new Clock(ZoneId.of("UTC"), Instant.parse("2024-06-01T00:00:00Z"));

  @Test
  public void anIntAfterAnEarlierColumnStaysAboveIt() {
    Generator end = CellLanguage.parse("int after start:0..20");
    Row row = row("start", "5");
    String cell = end.draw(new Seed(1).random(), row).cell();
    assertTrue(Integer.parseInt(cell) > 5);
    assertTrue(Integer.parseInt(cell) <= 20);
    assertTrue(end.holds(new TextValue("20"), row));
    assertEquals(cell, end.draw(new Seed(1).random(), row).cell());
    for (Value simpler : end.simpler(new TextValue(cell), row)) {
      assertTrue(end.holds(simpler, row));
      assertTrue(Integer.parseInt(simpler.cell()) < Integer.parseInt(cell));
    }
  }

  @Test
  public void beforeStaysUnderTheEarlierValueAndAnEmptyWindowIsRefused() {
    Generator low = CellLanguage.parse("int before limit:0..10");
    Row row = row("limit", "4");
    int drawn = Integer.parseInt(low.draw(new Seed(2).random(), row).cell());
    assertTrue(drawn >= 0 && drawn < 4);
    assertEquals("4", low.draw(new Top(), row("limit", "5")).cell());
    assertTrue(low.holds(new TextValue("3"), row("limit", "4")));
    IllegalArgumentException high = assertThrows(IllegalArgumentException.class,
      () -> CellLanguage.parse("int after start:0..20").draw(new Seed(1).random(), row("start", "20")));
    assertTrue(high.getMessage(), high.getMessage().contains("no int is after start"));
    IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
      () -> low.draw(new Seed(1).random(), Row.EMPTY));
    assertTrue(missing.getMessage(), missing.getMessage().contains("limit is not drawn"));
    assertTrue(message(() -> CellLanguage.parse("int after")).contains("expected a column"));
    assertTrue(message(() -> CellLanguage.parse("int after 5")).contains("expected a column"));
    assertTrue(message(() -> new Limit(new BoolGen(), new Relation("flag", true))).contains("number or a time"));
    IllegalArgumentException under = assertThrows(IllegalArgumentException.class,
      () -> CellLanguage.parse("int before limit:0..10").draw(new Seed(1).random(), row("limit", "0")));
    assertTrue(under.getMessage(), under.getMessage().contains("no int is before limit"));
    Generator end = CellLanguage.parse("int after start:0..20");
    assertFalse(end.holds(new TextValue("5"), row("start", "5")));
    assertFalse(end.holds(new TextValue("21"), row("start", "5")));
    assertFalse(end.holds(new TextValue("-1"), row("start", "5")));
    assertThrows(IllegalArgumentException.class, () -> end.draw(new Seed(1).random()));
    assertThrows(IllegalArgumentException.class, () -> end.simpler(new TextValue("6")));
    assertEquals("int", end.family());
    assertEquals("decimal scale 2", CellLanguage.parse("money after paid:0.00..1.00").family());
    assertEquals("date", CellLanguage.parse("date after start:2020-01-01..2020-01-02", CLOCK).family());
  }

  @Test
  public void moneyAndDatesStayStrictlyAfterTheEarlierCell() {
    Generator price = CellLanguage.parse("money after paid:0.00..2.00");
    Row paid = row("paid", "1.00");
    BigDecimal money = new BigDecimal(price.draw(new Seed(3).random(), paid).cell());
    assertTrue(money.compareTo(new BigDecimal("1.00")) > 0);
    assertTrue(money.compareTo(new BigDecimal("2.00")) <= 0);
    assertEquals(2, money.scale());
    assertFalse(price.holds(new TextValue("1.00"), paid));
    for (Value simpler : price.simpler(new TextValue(money.toPlainString()), paid)) {
      assertTrue(price.holds(simpler, paid));
    }
    assertTrue(message(() -> price.draw(new Seed(1).random(), row("paid", "2.00"))).contains("no decimal scale 2 is after paid"));

    Generator day = CellLanguage.parse("date after start:2020-01-01..2020-01-10", CLOCK);
    Row start = row("start", "2020-01-05");
    String date = day.draw(new Seed(4).random(), start).cell();
    assertTrue(date.compareTo("2020-01-05") > 0);
    assertTrue(date.compareTo("2020-01-10") <= 0);
    assertFalse(day.holds(new TextValue("2020-01-05"), start));
    for (Value simpler : day.simpler(new TextValue(date), start)) {
      assertTrue(day.holds(simpler, start));
    }
    Generator time = CellLanguage.parse("time before close:08:00:00..12:00:00", CLOCK);
    String clock = time.draw(new Seed(5).random(), row("close", "10:00:00")).cell();
    assertTrue(clock.compareTo("08:00:00") >= 0);
    assertTrue(clock.compareTo("10:00:00") < 0);
    assertFalse(time.holds(new TextValue("10:00:00"), row("close", "10:00:00")));
    Generator instant = CellLanguage.parse(
      "datetime after start:2024-06-01T00:00:00Z..2024-06-01T00:00:05Z", CLOCK);
    String drawn = instant.draw(new Seed(6).random(), row("start", "2024-06-01T00:00:02Z")).cell();
    assertTrue(drawn.compareTo("2024-06-01T00:00:02Z") > 0);
    assertTrue(drawn.compareTo("2024-06-01T00:00:05Z") <= 0);
    Generator wide = CellLanguage.parse("int after start:0..10");
    int raised = Integer.parseInt(wide.draw(new Seed(2).random(), row("start", "-3")).cell());
    assertTrue(raised >= 0 && raised <= 10);
    Generator capped = CellLanguage.parse("int before limit:0..10");
    int held = Integer.parseInt(capped.draw(new Seed(2).random(), row("limit", "20")).cell());
    assertTrue(held >= 0 && held <= 10);
    assertTrue(message(() -> CellLanguage.parse("int after start > 5")).contains("trailing text"));
  }

  @Test
  public void aListDrawsEveryElementAfterTheEarlierColumn() {
    Generator lists = CellLanguage.parse("list of int after start:0..6 size 2..2");
    Row row = row("start", "3");
    Value drawn = lists.draw(new Seed(7).random(), row);
    String cell = drawn.cell();
    assertTrue(cell.startsWith("["));
    for (String part : cell.substring(1, cell.length() - 1).split(", ")) {
      assertTrue(Integer.parseInt(part) > 3);
    }
    for (Value simpler : lists.simpler(drawn, row)) {
      assertTrue(lists.holds(simpler, row));
    }
    Generator before = CellLanguage.parse("list of int before limit:0..10 size 1..1");
    Row tight = row("limit", "4");
    Value under = before.draw(new Seed(8).random(), tight);
    assertTrue(before.holds(under, tight));
    assertFalse(before.holds(under, row("limit", "0")));
  }

  @Test
  public void cyclesLaterColumnsAndKindMismatchesAreRefused() {
    expect("later column", "end", "int after start", "start", "int:0..1");
    expect("cycle in column end", "end", "int after end");
    expect("nope is not a column", "end", "int after nope");
    expect("start is int, not date", "start", "int:0..1", "end", "date after start");
    expect("", "start", "money:0..1", "end", "decimal after start:0..1 scale 2");
    expect("already a column", "start", "int:0..1", "start", "int:0..1", "end", "int after start");
    expect("", "start", "int:0..3", "end", "int before start:0..9");
    expect("start is string, not int", "start", "string", "end", "int after start");
    expect("flag is boolean, not int", "flag", "boolean", "end", "int after flag");
    expect("flag is list, not int", "flag", "list of int:0..1 size 0..1", "end", "int after flag");
    expect("flag is one of, not int", "flag", "one of:a,b", "end", "int after flag");
    expect("flag is pattern, not int", "flag", "pattern:[0-9]{1}", "end", "int after flag");
    expect("flag is all of, not int", "flag", "all of:a,b", "end", "int after flag");
    expect("flag is period, not int", "flag", "period:day", "end", "int after flag");
    expect("flag is now, not int", "flag", "now", "end", "int after flag");
    expect("start is decimal scale 2, not decimal scale 0",
      "start", "money:0.00..1.00", "end", "decimal after start:0..1 scale 0");
    expect("start is date, not time", "start", "date:2020-01-01..2020-01-02", "end", "time after start");
    expect("start is datetime, not date", "start", "datetime:2020-01-01T00:00:00Z..2020-01-02T00:00:00Z", "end", "date after start");
    new Links();
  }

  private static Row row(String name, String cell) {
    return Row.EMPTY.with(name, new TextValue(cell));
  }

  private static String message(Throwing call) {
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, call::run);
    return error.getMessage();
  }

  private static void expect(String part, String... pairs) {
    String message = refused(pairs);
    assertTrue(message, part.isEmpty() ? message.isEmpty() : message.contains(part));
  }

  private static String refused(String... pairs) {
    List<String> names = new ArrayList<>();
    List<Generator> generators = new ArrayList<>();
    for (int i = 0; i < pairs.length; i += 2) {
      names.add(pairs[i]);
      generators.add(CellLanguage.parse(pairs[i + 1], CLOCK));
    }
    try {
      Links.bind(names, generators);
      return "";
    } catch (IllegalArgumentException error) {
      return error.getMessage();
    }
  }

  private interface Throwing {
    void run();
  }

  /** Always draws the top of the requested span. */
  private static final class Top extends java.util.Random {
    @Override
    public long nextLong(long origin, long bound) {
      return bound - 1;
    }
  }
}
