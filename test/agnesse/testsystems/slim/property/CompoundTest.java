package agnesse.testsystems.slim.property;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class CompoundTest {
  private static final Clock CLOCK = new Clock(ZoneId.of("UTC"), Instant.parse("2024-06-01T00:00:00Z"));

  @Test
  public void uniqueAndSortedListsKeepTheirShape() {
    Generator ids = parse("list of int:0..20 size 4..4 unique");
    String once = ids.draw(new Seed(3).random()).cell();
    assertEquals(once, ids.draw(new Seed(3).random()).cell());
    assertEquals(4, parts(once).length);
    assertEquals(4, java.util.Set.of(parts(once)).size());
    ListValue crafted = ints(3, 0);
    for (Value simpler : ids.simpler(crafted)) {
      assertTrue(ids.holds(simpler, Row.EMPTY));
      assertFalse(simpler.cell().contains("0, 0") && simpler.cell().equals("[0, 0]"));
    }
    assertFalse(ids.holds(ints(0, 0), Row.EMPTY));

    Generator ordered = parse("list of int:0..20 size 2..2 sorted");
    assertFalse(ordered.holds(ints(10, 2), Row.EMPTY));
    assertTrue(ordered.holds(ints(2, 10), Row.EMPTY));
    Generator strict = parse("list of int:0..20 size 2..2 sorted unique");
    assertFalse(strict.holds(ints(2, 2), Row.EMPTY));
    assertTrue(strict.holds(ints(2, 10), Row.EMPTY));
    assertTrue(parse("list of int:0..20 size 2..2 unique sorted").holds(ints(2, 10), Row.EMPTY));

    Generator words = parse("list of string length 1..1 size 2..2 sorted");
    assertTrue(words.holds(texts("10", "2"), Row.EMPTY));
    assertFalse(words.holds(texts("b", "a"), Row.EMPTY));
    Generator money = parse("list of money:0.00..20.00 size 2..2 sorted");
    assertFalse(money.holds(texts("10.00", "2.00"), Row.EMPTY));
    assertTrue(money.holds(texts("2.00", "10.00"), Row.EMPTY));

    IllegalArgumentException small = assertThrows(IllegalArgumentException.class,
      () -> parse("list of boolean size 3..3 unique"));
    assertTrue(small.getMessage(), small.getMessage().contains("unique list needs 3 values but boolean has 2"));
    assertThrows(IllegalArgumentException.class, () -> parse("list of int:0..1 size 3..3 unique"));
    assertThrows(IllegalArgumentException.class, () -> parse("list of one of:a,b size 3..3 unique"));
    IllegalArgumentException exhausted = assertThrows(IllegalArgumentException.class,
      () -> parse("list of string length 0..0 size 2..2 unique").draw(new Seed(1).random()));
    assertTrue(exhausted.getMessage(), exhausted.getMessage().contains("ran out of values"));
    assertEquals("[false, true]", parse("list of boolean size 2..2 unique").draw(new LastCoin()).cell());
  }

  @Test
  public void datesSortChronologicallyAndASpanStaysInRange() {
    Generator days = CellLanguage.parse("list of date:2020-01-01..2020-01-10 size 2..2 sorted", CLOCK);
    assertFalse(days.holds(texts("2020-01-10", "2020-01-02"), Row.EMPTY));
    assertTrue(days.holds(texts("2020-01-02", "2020-01-10"), Row.EMPTY));

    Generator spans = parse("span:-5..5 length 0..4");
    assertEquals("span", spans.family());
    String drawn = spans.draw(new Seed(4).random()).cell();
    assertEquals(drawn, spans.draw(new Seed(4).random()).cell());
    assertTrue(spans.holds(new TextValue(drawn), Row.EMPTY));
    assertTrue(spans.holds(new TextValue("-3+4"), Row.EMPTY));
    assertFalse(spans.holds(new TextValue("-3+5"), Row.EMPTY));
    assertFalse(spans.holds(new TextValue("3+"), Row.EMPTY));
    for (Value simpler : spans.simpler(new TextValue("-3+4"))) {
      assertTrue(spans.holds(simpler, Row.EMPTY));
    }
    assertTrue(spans.simpler(new TextValue("-3+4")).get(0).cell().endsWith("+0"));
    Generator many = parse("list of span:0..10 length 1..3 size 2..2");
    assertTrue(many.draw(new Seed(1).random()).cell().contains("+"));
    assertTrue(message(() -> parse("span:0..10 length -1..3")).contains("length must be zero or more"));
  }

  @Test
  public void aPartitionSumsExactlyAndShrinksOntoTheFirstPart() {
    Generator money = parse("partition:10.00 into 3");
    assertEquals("partition", money.family());
    String drawn = money.draw(new Seed(2).random()).cell();
    assertEquals(drawn, money.draw(new Seed(2).random()).cell());
    assertEquals(new BigDecimal("10.00"), sum(drawn, 2));
    for (Value simpler : money.simpler(money.draw(new Seed(2).random()))) {
      assertEquals(new BigDecimal("10.00"), sum(simpler.cell(), 2));
      assertTrue(money.holds(simpler, Row.EMPTY));
    }
    Generator whole = parse("partition:10 into 2");
    ListValue split = ints(4, 6);
    assertEquals(List.of("[10, 0]", "[7, 3]", "[5, 5]"), cells(whole.simpler(split)));
    assertEquals("[2, 7, 1]", parse("partition:10 into 3").draw(new Seed(1).random()).cell());
    assertEquals("[10]", parse("partition:10 into 1").draw(new Seed(1).random()).cell());
    assertEquals("[0.00, 0.00]", parse("partition:0.00 into 2").draw(new Seed(1).random()).cell());
    assertFalse(whole.holds(ints(4, 5), Row.EMPTY));
    assertTrue(message(() -> parse("partition:10 into 0")).contains("1 through 8"));
    assertTrue(message(() -> parse("partition:10 into 9")).contains("1 through 8"));
    assertTrue(message(() -> parse("partition:-1 into 2")).contains("zero or more"));
  }

  @Test
  public void metersKeepARunningTotalWhenTheyReset() {
    assertEquals(33, Meter.total(new int[] {10, 25, 3, 8}, 0));
    assertEquals(155, Meter.total(new int[] {100, 0, 55}, 0));
    assertEquals(33, Meter.total(new int[] {33}, 0));
    assertEquals(200, Meter.total(new int[] {100, 0, 100}, 0));
    assertEquals("[33]", text(Meter.canonical(33, 0, 100, 1)));
    assertEquals("[100, 0, 55]", text(Meter.canonical(155, 0, 100, 1)));
    assertEquals("[100, 0, 100]", text(Meter.canonical(200, 0, 100, 1)));
    assertEquals("[0, 0, 0, 0]", text(Meter.canonical(0, 0, 100, 4)));
    assertThrows(IllegalArgumentException.class, () -> Meter.canonical(5, 3, 3, 1));

    Generator plain = parse("monotonic:0..20 size 2..4");
    assertEquals("monotonic", plain.family());
    String rising = plain.draw(new Seed(5).random()).cell();
    assertEquals(rising, plain.draw(new Seed(5).random()).cell());
    assertTrue(plain.holds(intsFrom(rising), Row.EMPTY));
    assertTrue(plain.holds(ints(2, 2, 5), Row.EMPTY));
    assertFalse(plain.holds(ints(5, 2), Row.EMPTY));
    assertEquals("[0, 0]", plain.simpler(ints(5, 8, 9)).get(0).cell());

    Generator reset = parse("monotonic:0..100 size 1..4 reset");
    assertEquals("monotonic reset", reset.family());
    ListValue meter = ints(10, 25, 3, 8);
    assertEquals("[33]", reset.simpler(meter).get(0).cell());
    assertEquals(List.of("[0, 0]", "[0, 1]"), cells(parse("monotonic:0..10 size 2..2").simpler(ints(0, 2))));
    assertEquals(List.of("[8]", "[8, 8]", "[5, 8]", "[3, 8]"),
      cells(parse("monotonic:0..100 size 1..4 reset").simpler(ints(2, 8))));
    assertTrue(cells(parse("monotonic:5..10 size 1..3").simpler(ints(6, 8, 9))).contains("[5]"));
    for (Value simpler : reset.simpler(meter)) {
      assertEquals(33, Meter.total(read(simpler), 0));
      assertTrue(reset.holds(simpler, Row.EMPTY));
    }
    long kept = Meter.total(read(reset.draw(new Seed(6).random())), 0);
    assertEquals(kept, Meter.total(read(reset.draw(new Seed(6).random())), 0));
  }

  @Test
  public void aRecordIsJsonAndFieldsCanDependOnEarlierFields() {
    Generator person = parse("record:name one of:Ada; age int:36..36; balance money:1.50..1.50");
    assertEquals("record", person.family());
    assertEquals("{\"name\":\"Ada\",\"age\":\"36\",\"balance\":\"1.50\"}", person.draw(new Seed(1).random()).cell());
    Generator nested = parse("record:name one of:Ada; doc record:(kind one of:id)");
    assertEquals("{\"name\":\"Ada\",\"doc\":{\"kind\":\"id\"}}", nested.draw(new Seed(1).random()).cell());
    assertEquals("{\"nums\":[\"1\",\"1\"]}",
      parse("record:nums list of int:1..1 size 2..2").draw(new Seed(1).random()).cell());
    assertEquals("[{\"n\":\"1\"}, {\"n\":\"1\"}]",
      parse("list of record:n int:1..1 size 2..2").draw(new Seed(1).random()).cell());
    assertEquals("\"a\\\"b\\\\c\\b\\f\\n\\r\\t\\u0001\"", JsonText.quote("a\"b\\c\b\f\n\r\t\u0001"));
    assertEquals("\" \"", JsonText.quote(" "));

    Generator shadow = CellLanguage.parse(
      "record:(start date:2020-06-01..2020-06-01; end date after start:2020-01-02..2020-01-10)", CLOCK);
    IllegalArgumentException hidden = assertThrows(IllegalArgumentException.class,
      () -> shadow.draw(new Seed(1).random(), Row.EMPTY.with("start", new TextValue("2020-01-01"))));
    assertTrue(hidden.getMessage(), hidden.getMessage().contains("no date is after start"));

    Generator bounded = parse("record:start int:0..5; end int after start:0..9");
    RecordValue row = new RecordValue(List.of("start", "end"), List.of(new TextValue("4"), new TextValue("6")));
    for (Value simpler : bounded.simpler(row)) {
      assertTrue(bounded.holds(simpler, Row.EMPTY));
    }
    assertFalse(bounded.holds(new RecordValue(List.of("start", "end"),
      List.of(new TextValue("6"), new TextValue("6"))), Row.EMPTY));

    Generator afterColumn = parse("record:end int after start:0..20");
    int end = Integer.parseInt(jsonField(afterColumn.draw(new Seed(1).random(),
      Row.EMPTY.with("start", new TextValue("5"))).cell(), "end"));
    assertTrue(end > 5 && end <= 20);

    assertTrue(message(() -> Links.bind(List.of("row"), List.of(parse("record:end int after end")))).contains("cycle in field end"));
    assertTrue(message(() -> Links.bind(
      List.of("end", "person"),
      List.of(parse("int:0..9"), parse("record:end int after end:0..9"))
    )).contains("cycle in field end"));
    assertTrue(message(() -> Links.bind(List.of("row"), List.of(parse("record:end int after expires; expires int:0..1")))).contains("later field"));
    assertTrue(message(() -> Links.bind(List.of("row"), List.of(parse("record:end int after nope")))).contains("nope is not a column"));
    assertTrue(message(() -> parse("record:name string; name string")).contains("already a field"));
    assertTrue(message(() -> parse("record:doc record:kind string")).contains("expected ("));
    assertTrue(message(() -> parse("record:flag all of:a,b")).contains("enumerated"));
    assertTrue(message(() -> parse("record:")).contains("expected a field"));
    Generator mixed = CellLanguage.parse("record:name one of:Ada; when now", CLOCK);
    assertTrue(mixed.timed());
    Links.bind(List.of("person"), List.of(parse("record:start int:0..1; end int after start:0..9")));
    assertFalse(bounded.holds(new TextValue("{}"), Row.EMPTY));
    assertFalse(bounded.holds(new RecordValue(List.of("start"), List.of(new TextValue("1"))), Row.EMPTY));
  }

  @Test
  public void edgesOutsideADrawnValueAreRefused() {
    assertEquals(4, ((IntGen) parse("int!=0:-2..2")).variety());
    assertEquals(Integer.MAX_VALUE, ((IntGen) parse("int:0..2147483646")).variety());
    assertEquals(-1, ((IntGen) parse("int:-2147483648..2147483647")).variety());
    assertEquals(11, ((DecimalGen) parse("decimal:0..10 scale 0")).variety());
    assertEquals(Integer.MAX_VALUE, ((DecimalGen) parse("decimal:0..2147483646 scale 0")).variety());
    assertEquals(-1, ((DecimalGen) parse("decimal:0..3000000000 scale 0")).variety());
    assertEquals(10, CellLanguage.parse("date:2020-01-01..2020-01-10", CLOCK).variety());
    assertEquals(-1, CellLanguage.parse("datetime", CLOCK).variety());
    assertTrue(message(() -> parse("list of int!=0:-2..2 size 5..5 unique")).contains("has 4"));
    Generator strict = parse("list of int:0..20 size 2..2 sorted unique");
    assertFalse(strict.holds(texts("2", "02"), Row.EMPTY));
    assertTrue(parse("list of int:0..20 size 2..2 sorted").holds(ints(2, 2), Row.EMPTY));
    assertEquals("int", parse("int after a_2:0..1").family());

    Generator spans = parse("span:-5..5 length 0..4");
    assertTrue(spans.holds(new TextValue("3+4"), Row.EMPTY));
    assertTrue(spans.holds(new TextValue("-3+4"), Row.EMPTY));
    assertFalse(spans.holds(new TextValue("3+x"), Row.EMPTY));
    assertFalse(spans.holds(new TextValue("6+1"), Row.EMPTY));
    assertFalse(spans.holds(new TextValue("+5"), Row.EMPTY));
    assertFalse(spans.holds(new TextValue("3+"), Row.EMPTY));
    assertFalse(spans.holds(new TextValue("35"), Row.EMPTY));
    assertTrue(message(() -> spans.simpler(new TextValue("nope"))).contains("not a span"));
    List<String> tooLong = new java.util.ArrayList<>();
    for (Value simpler : spans.simpler(new TextValue("3+100"))) {
      tooLong.add(simpler.cell());
    }
    assertTrue(tooLong.contains("3+0"));
    assertFalse(tooLong.contains("3+50"));
    Generator high = parse("span:5..10 length 1..4");
    for (Value simpler : high.simpler(new TextValue("8+4"))) {
      assertTrue(high.holds(simpler, Row.EMPTY));
    }
    assertTrue(message(() -> parse("span")).contains("expected :"));
    assertTrue(message(() -> parse("span:0..1")).contains("expected length"));

    Generator whole = parse("partition:10 into 2");
    assertFalse(whole.holds(new TextValue("10"), Row.EMPTY));
    assertFalse(whole.holds(ints(10), Row.EMPTY));
    assertFalse(whole.holds(ints(-1, 11), Row.EMPTY));
    assertFalse(whole.holds(texts("1.5", "8.5"), Row.EMPTY));
    assertFalse(whole.holds(texts("nope", "10"), Row.EMPTY));
    assertFalse(parse("partition:1 into 1").holds(texts("1.5"), Row.EMPTY));
    assertFalse(parse("partition:0 into 1").holds(texts("1.5"), Row.EMPTY));
    assertFalse(parse("partition:1 into 1").holds(texts("nope"), Row.EMPTY));
    assertFalse(whole.holds(texts("100000000000000000000", "0"), Row.EMPTY));
    assertTrue(message(() -> parse("partition:1.000000000 into 1")).contains("scale must be 0 through 8"));
    assertTrue(message(() -> parse("partition:10000000000000000000 into 1")).contains("total is too wide"));
    assertTrue(message(() -> parse("partition")).contains("expected :"));
    assertTrue(message(() -> parse("partition:10")).contains("expected into"));

    Generator plain = parse("monotonic:0..20 size 2..4");
    assertEquals(0, plain.simpler(new TextValue("nope")).size());
    assertEquals(0, plain.simpler(texts("a", "b")).size());
    assertFalse(plain.holds(new TextValue("1"), Row.EMPTY));
    assertFalse(plain.holds(ints(1), Row.EMPTY));
    assertFalse(plain.holds(ints(1, 2, 3, 4, 5), Row.EMPTY));
    assertFalse(plain.holds(ints(0, 21), Row.EMPTY));
    assertFalse(plain.holds(ints(-1, 0), Row.EMPTY));
    Generator reset = parse("monotonic:0..100 size 0..4 reset");
    assertEquals(0, reset.simpler(ints()).size());
    for (Value simpler : parse("monotonic:0..100 size 1..2 reset").simpler(ints(100, 0, 100, 0))) {
      assertTrue(parse("monotonic:0..100 size 1..2 reset").holds(simpler, Row.EMPTY));
    }
    assertTrue(message(() -> parse("monotonic:0..10 size -1..2")).contains("size must be zero or more"));
    assertTrue(message(() -> parse("monotonic")).contains("expected :"));
    assertEquals("monotonic", parse("monotonic:0..5").family());
    assertTrue(message(() -> parse("record")).contains("expected :"));
    assertTrue(message(() -> parse("record:(name string")).contains("expected )"));
    assertEquals("2020-01-02", jsonField(CellLanguage.parse(
      "record:(day date:2020-01-02..2020-01-02)", CLOCK).draw(new Seed(1).random()).cell(), "day"));
    assertTrue(message(() -> CellLanguage.parse("date:2020-01-01)..2020-01-02", CLOCK)).length() > 0);
    Generator before = parse("record:limit int:0..10; end int before limit:0..10");
    RecordValue meter = new RecordValue(List.of("limit", "end"), List.of(new TextValue("6"), new TextValue("5")));
    for (Value simpler : before.simpler(meter)) {
      assertTrue(before.holds(simpler, Row.EMPTY));
    }

    assertTrue(message(() -> Meter.canonical(1, 5, 4, 1)).contains("inverted range"));
    assertTrue(message(() -> Meter.canonical(-1, 0, 10, 1)).contains("zero or more"));
    assertEquals(3, Meter.total(new int[] {10, 3}, 5));
    assertEquals("[5]", text(Meter.canonical(0, 5, 5, 1)));
    assertEquals("[7, 8]", text(Meter.fit(new int[] {7, 8}, 2)));
    assertEquals("[7, 8, 8, 8]", text(Meter.fit(new int[] {7, 8}, 4)));
    assertNull(Meter.fit(new int[] {1, 2}, 1));
    assertEquals("[4, 5]", text(Meter.raise(new int[] {3, 5}, 1, 9)));
    assertEquals("[5, 5]", text(Meter.raise(new int[] {3, 5}, 2, 9)));
    assertEquals("[5, 5]", text(Meter.raise(new int[] {3, 5}, 2, 5)));
    assertNull(Meter.raise(new int[] {1}, 1, 10));
    assertNull(Meter.raise(new int[] {1, 3}, 0, 10));
    assertNull(Meter.raise(new int[] {5, 3}, 1, 10));
    assertNull(Meter.raise(new int[] {1, 3}, 3, 10));
    assertNull(Meter.raise(new int[] {8, 12}, 2, 9));
  }

  private static Generator parse(String spec) {
    return CellLanguage.parse(spec);
  }

  private static String message(Throwing call) {
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, call::run);
    return error.getMessage();
  }

  private static ListValue ints(int... values) {
    List<Value> items = new java.util.ArrayList<>();
    for (int value : values) {
      items.add(new TextValue(Integer.toString(value)));
    }
    return new ListValue(items);
  }

  private static ListValue texts(String... values) {
    List<Value> items = new java.util.ArrayList<>();
    for (String value : values) {
      items.add(new TextValue(value));
    }
    return new ListValue(items);
  }

  private static ListValue intsFrom(String cell) {
    return ints(read(new TextValue(cell)));
  }

  private static String[] parts(String cell) {
    String body = cell.substring(1, cell.length() - 1);
    return body.isEmpty() ? new String[0] : body.split(", ");
  }

  private static int[] read(Value value) {
    String[] parts = parts(value.cell());
    int[] readings = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      readings[i] = Integer.parseInt(parts[i]);
    }
    return readings;
  }

  private static String text(int[] readings) {
    return ints(readings).cell();
  }

  private static List<String> cells(List<Value> values) {
    List<String> out = new java.util.ArrayList<>();
    for (Value value : values) {
      out.add(value.cell());
    }
    return out;
  }

  private static BigDecimal sum(String cell, int scale) {
    BigDecimal total = BigDecimal.ZERO.setScale(scale);
    for (String part : parts(cell)) {
      total = total.add(new BigDecimal(part));
    }
    return total;
  }

  private static String jsonField(String json, String name) {
    String key = "\"" + name + "\":\"";
    int start = json.indexOf(key) + key.length();
    return json.substring(start, json.indexOf('"', start));
  }

  private interface Throwing {
    void run();
  }

  /** Sixty-three repeats, then the missing boolean. One fewer attempt gives up. */
  private static final class LastCoin extends java.util.Random {
    private int drawn;

    @Override
    public boolean nextBoolean() {
      boolean missing = drawn == 63;
      drawn++;
      return missing;
    }
  }
}
