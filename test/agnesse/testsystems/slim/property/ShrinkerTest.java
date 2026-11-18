package agnesse.testsystems.slim.property;

import java.util.List;
import java.util.function.Predicate;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ShrinkerTest {
  private final Shrinker shrinker = new Shrinker();
  private int calls;

  @Test
  public void intsShrinkTowardZero() {
    assertEquals("0", shrink("int:-100..100", n(80), v -> true, 1));
    assertEquals("10", shrink("int:-100..100", n(80), v -> at(v) >= 10, 80));
    assertEquals("-10", shrink("int:-100..100", n(-80), v -> at(v) <= -10, 80));
  }

  @Test
  public void halvingIsWhatMakesALargeIntSmallInsideTheCap() {
    assertEquals("50", shrink("int:-200..200", n(100), v -> at(v) >= 30, 1));
    assertEquals("-50", shrink("int:-200..200", n(-100), v -> at(v) <= -30, 1));
  }

  @Test
  public void aSingleStepReachesTheNeighborWhenTheHalfPasses() {
    assertEquals("4", shrink("int:-20..20", n(5), v -> at(v) >= 4, 3));
    assertEquals("-4", shrink("int:-20..20", n(-5), v -> at(v) <= -4, 3));
  }

  @Test
  public void shrinkingRespectsTheRangeAndTheFilter() {
    assertEquals("5", shrink("int:5..100", n(20), v -> true, 10));
    assertEquals("-5", shrink("int:-100..-5", n(-20), v -> true, 10));
    assertEquals("1", shrink("int!=0:-100..100", n(40), v -> true, 10));
    assertEquals("3", shrink("int!=0:-100..100", n(40), v -> Math.abs(at(v)) >= 3, 20));
    assertEquals("1", shrink("int>0:-10..10", n(8), v -> true, 5));
    assertEquals("0", shrink("int>=0:-10..10", n(8), v -> true, 5));
    assertEquals("-1", shrink("int<0:-10..10", n(-8), v -> true, 5));
    assertEquals("0", shrink("int<=0:-10..10", n(-8), v -> true, 5));
  }

  @Test
  public void zeroDoesNotShrinkAndTheCapIsExact() {
    calls = 0;
    assertEquals("0", shrink("int:-5..5", n(0), count(v -> true), 4));
    assertEquals(0, calls);
    assertEquals("80", shrink("int:-100..100", n(80), v -> true, 0));
    assertEquals("79", shrink("int:-100..100", n(80), v -> at(v) >= 50, 1));
    assertEquals("50", shrink("int:-100..100", n(80), v -> at(v) >= 50, 40));
  }

  @Test
  public void aNegativeCapIsRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> shrinker.shrink(CellLanguage.parse("int:0..1"), n(1), v -> true, -1));
  }

  @Test
  public void booleansShrinkTowardFalse() {
    assertEquals("false", shrink("boolean", new TextValue("true"), v -> true, 2));
    assertEquals("true", shrink("boolean", new TextValue("true"), v -> "true".equals(v.cell()), 2));
    calls = 0;
    assertEquals("false", shrink("boolean", new TextValue("false"), count(v -> true), 3));
    assertEquals(0, calls);
  }

  @Test
  public void stringsShrinkTowardShorterPrefixes() {
    assertEquals("", shrink("string:ascii length 0..20", text("abcdef"), v -> true, 5));
    assertEquals("abc", shrink("string length 0..20", text("abcdef"), v -> v.cell().length() >= 3, 5));
    assertEquals("abc", shrink("string length 0..20", text("abcd"), v -> v.cell().length() >= 3, 1));
    assertEquals("abcde", shrink("string length 0..20", text("abcdefghij"), v -> v.cell().length() >= 4, 1));
    assertEquals("abcd", shrink("string length 2..10", text("abcdefgh"), v -> v.cell().length() >= 4, 5));
    calls = 0;
    assertEquals("ab", shrink("string length 2..10", text("ab"), count(v -> true), 3));
    assertEquals(0, calls);
  }

  @Test
  public void listsShrinkTowardShorterThenSmallerElements() {
    assertEquals("[]", shrink("list of int:0..9", ints(1, 2, 3), v -> true, 5));
    assertEquals("[5]", shrink("list of int:-20..20 size 0..10", ints(9, 8, 7),
        v -> hasAtLeast(v, 5), 10));
    assertEquals("[0, 0]", shrink("list of int:0..20 size 2..5", ints(9, 8, 7), v -> true, 10));
    assertEquals("[4]", shrink("list of int!=0:-10..10 size 0..5", ints(8, -7),
        v -> hasAbsAtLeast(v, 4), 10));
    assertEquals("[5, 0]", shrink("list of int:-20..20 size 2..4", ints(8, 0),
        v -> atList(v, 0) >= 5, 10));
  }

  @Test
  public void halvingIsWhatMakesALongListOrStringShortInsideTheCap() {
    assertEquals(4, lengthOf(shrink("list of int:1..1 size 0..20", ints(1, 1, 1, 1, 1, 1, 1, 1),
        v -> lengthOf(v.cell()) >= 3, 1)));
    assertEquals("abcde", shrink("string length 0..20", text("abcdefghij"),
        v -> v.cell().length() >= 4, 1));
  }

  @Test
  public void nestedListsShrinkTheInnerElements() {
    ListValue inner = new ListValue(List.of(text("9"), text("8")));
    ListValue start = new ListValue(List.of(inner, new ListValue(List.of(text("7")))));
    assertEquals("[[4]]", shrink("list of list of int:-10..10 size 0..3 size 0..3", start,
        ShrinkerTest::nestedHasAtLeast4, 10));
  }

  @Test
  public void oneOfShrinksTowardTheFirstChoice() {
    Generator colors = CellLanguage.parse("one of:red,green,blue");
    assertEquals("red", shrinker.shrink(colors, new TextValue("blue"), v -> true, 3).cell());
    assertEquals("blue", shrinker.shrink(colors, new TextValue("blue"),
        v -> "blue".equals(v.cell()), 3).cell());
    assertEquals("green", shrinker.shrink(colors, new TextValue("blue"),
        v -> !"red".equals(v.cell()), 5).cell());
    calls = 0;
    assertEquals("red", shrinker.shrink(colors, new TextValue("red"), count(v -> true), 4).cell());
    assertEquals(0, calls);
  }

  @Test
  public void shrinkingAValueThatIsNotAChoiceFails() {
    Generator colors = CellLanguage.parse("one of:red,green");
    assertThrows(IllegalArgumentException.class,
        () -> shrinker.shrink(colors, new TextValue("yellow"), v -> true, 2));
  }

  @Test
  public void theSameSeedDrawsAndShrinksTheSameWay() {
    Generator spec = CellLanguage.parse("int:-80..80");
    Predicate<Value> fails = v -> Math.abs(at(v)) >= 10;
    Value drawn = spec.draw(new Seed(99L).random());
    String once = shrinker.shrink(spec, drawn, fails, 30).cell();
    Value again = spec.draw(new Seed(99L).random());
    assertEquals(drawn.cell(), again.cell());
    assertEquals(once, shrinker.shrink(spec, again, fails, 30).cell());
  }

  @Test
  public void shrinkingDoesNotDependOnTheSeedThatDrewTheValue() {
    Generator spec = CellLanguage.parse("list of int:-15..15 size 0..6");
    Value drawn = spec.draw(new Seed(3L).random());
    Predicate<Value> fails = v -> v.cell().contains("8") || v.cell().contains("9")
        || lengthOf(v.cell()) > 8;
    String once = shrinker.shrink(spec, drawn, fails, 20).cell();
    String twice = shrinker.shrink(spec, drawn, fails, 20).cell();
    assertEquals(once, twice);
  }

  private String shrink(String spec, Value start, Predicate<Value> fails, int cap) {
    return shrinker.shrink(CellLanguage.parse(spec), start, fails, cap).cell();
  }

  private Predicate<Value> count(Predicate<Value> inner) {
    return value -> {
      calls++;
      return inner.test(value);
    };
  }

  private static TextValue n(int value) {
    return new TextValue(Integer.toString(value));
  }

  private static TextValue text(String value) {
    return new TextValue(value);
  }

  private static ListValue ints(int... values) {
    List<Value> items = new java.util.ArrayList<>();
    for (int value : values) {
      items.add(n(value));
    }
    return new ListValue(items);
  }

  private static int at(Value value) {
    return Integer.parseInt(value.cell());
  }

  private static boolean hasAtLeast(Value value, int floor) {
    String body = value.cell();
    if ("[]".equals(body)) {
      return false;
    }
    for (String part : body.substring(1, body.length() - 1).split(", ")) {
      if (Integer.parseInt(part) >= floor) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasAbsAtLeast(Value value, int floor) {
    if ("[]".equals(value.cell())) {
      return false;
    }
    for (String part : value.cell().substring(1, value.cell().length() - 1).split(", ")) {
      if (Math.abs(Integer.parseInt(part)) >= floor) {
        return true;
      }
    }
    return false;
  }

  private static int atList(Value value, int index) {
    String body = value.cell().substring(1, value.cell().length() - 1);
    return Integer.parseInt(body.split(", ")[index]);
  }

  private static int lengthOf(String cell) {
    if ("[]".equals(cell)) {
      return 0;
    }
    return cell.split(", ").length;
  }

  private static boolean nestedHasAtLeast4(Value value) {
    return value.cell().contains("4") || value.cell().contains("5")
        || value.cell().contains("6") || value.cell().contains("7")
        || value.cell().contains("8") || value.cell().contains("9");
  }
}
