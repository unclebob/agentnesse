package agnesse.testsystems.slim.property;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class CellLanguageTest {
  @Test
  public void parsesTheDesignNoteExamples() {
    assertDomain("int:-10..10", -10, 10);
    assertDomain("int!=0:-1000..1000", -1000, 1000);
    assertEquals(0, list("list of int:-10..10 size 0..8").shortest());
    assertEquals(8, list("list of int:-10..10 size 0..8").longest());
    assertEquals(-10, ints(list("list of int:-10..10 size 0..8").element()).lowest());
    assertEquals(10, ints(list("list of int:-10..10 size 0..8").element()).highest());
    assertEquals(0, text("string:ascii length 0..20").shortest());
    assertEquals(20, text("string:ascii length 0..20").longest());
    assertTrue(!text("string:ascii").adversarial());
    assertTrue(text("string:adversarial length 0..40").adversarial());
    assertEquals(40, text("string:adversarial length 0..40").longest());
    assertTrue(CellLanguage.parse("boolean") instanceof BoolGen);
    assertEquals(List.of("red", "green", "blue"), one("one of:red,green,blue").choices());
    assertEquals(2, dec("decimal:-1.50..2 scale 2").scale());
    assertEquals(2, dec("money:-1.50..2").scale());
  }

  @Test
  public void missingBoundsUseTheDefaults() {
    assertDomain("int", -10_000, 10_000);
    assertEquals(0, text("string").shortest());
    assertEquals(20, text("string").longest());
    assertEquals(0, text("string:ascii").shortest());
    assertEquals(8, list("list of boolean").longest());
    assertEquals(0, list("list of boolean").shortest());
  }

  @Test
  public void filtersNarrowTheIntDomain() {
    assertDomain("int>5:0..10", 6, 10);
    assertDomain("int>=5:0..10", 5, 10);
    assertDomain("int<0:-5..5", -5, -1);
    assertDomain("int<=0:-5..5", -5, 0);
    assertDomain("int>0", 1, 10_000);
    assertDomain("int<0", -10_000, -1);
    assertDomain("int>=0", 0, 10_000);
    assertDomain("int<=0", -10_000, 0);
  }

  @Test
  public void whitespaceAndTabsAreOptional() {
    assertDomain("  int != 0 : -2 .. 2\t", -2, 2);
    assertEquals(List.of("red", "green"), one("one of: red, green").choices());
    assertEquals(1, list("list of  int : 2 .. 2  size  1 .. 1").shortest());
    assertEquals(3, text("string : ascii length 0 .. 3").longest());
  }

  @Test
  public void sizeBindsToTheListJustClosedAndLengthToTheString() {
    ListGen outer = list("list of list of int:-5..5 size 0..2 size 1..3");
    assertEquals(1, outer.shortest());
    assertEquals(3, outer.longest());
    ListGen inner = (ListGen) outer.element();
    assertEquals(0, inner.shortest());
    assertEquals(2, inner.longest());
    assertDomainText(inner.element(), -5, 5);
    ListGen strings = list("list of string length 0..3 size 1..2");
    assertEquals(1, strings.shortest());
    assertEquals(2, strings.longest());
    assertEquals(0, text(strings.element()).shortest());
    assertEquals(3, text(strings.element()).longest());
    ListGen choices = list("list of one of:red,green size 0..2");
    assertEquals(0, choices.shortest());
    assertEquals(List.of("red", "green"), ((OneOfGen) choices.element()).choices());
  }

  @Test
  public void aChoiceMayContainAColonButNotACommaOrASpace() {
    assertEquals(List.of("a:b", "c"), one("one of:a:b,c").choices());
    assertEquals(List.of(), one("one of:red,green").weights());
    assertEquals(List.of(70, 20, 10), one("one of:a 70, b 20, c 10").weights());
    assertEquals(List.of("a", "b", "c"), one("one of: a 70, b 20, c 10").choices());
    assertEquals(List.of(1), one("one of:only 1").weights());
    assertThrows(IllegalArgumentException.class, () -> new OneOfGen(List.of("a"), List.of(0)));
  }

  @Test
  public void asciiIsPrintableWithoutAPipe() {
    StringBuilder expected = new StringBuilder();
    for (int c = 32; c <= 126; c++) {
      if (c != '|') {
        expected.append((char) c);
      }
    }
    assertEquals(expected.toString(), StringGen.ASCII);
    assertEquals(' ', StringGen.ASCII.charAt(0));
    assertEquals('~', StringGen.ASCII.charAt(StringGen.ASCII.length() - 1));
    assertEquals(-1, StringGen.ASCII.indexOf('|'));
  }

  @Test
  public void anUnsatisfiableFilterIsRejected() {
    assertRejects("int!=5:5..5", "excludes");
    assertRejects("int>5:0..5", "excludes");
    assertRejects("int>=6:0..5", "excludes");
    assertRejects("int<0:0..5", "excludes");
    assertRejects("int<=-1:0..5", "excludes");
  }

  @Test
  public void theBoundaryFiltersThatStillHaveAnIntAreKept() {
    assertDomain("int!=5:5..6", 5, 6);
    assertDomain("int>5:0..6", 6, 6);
    assertDomain("int>=5:5..5", 5, 5);
    assertDomain("int<6:5..5", 5, 5);
    assertDomain("int<=5:5..5", 5, 5);
    assertDomain("int!=9:0..2", 0, 2);
    assertDomain("int!=-9:-2..0", -2, 0);
  }

  @Test
  public void intsMustFitTheSigned32BitRange() {
    assertDomain("int:2147483647..2147483647", Integer.MAX_VALUE, Integer.MAX_VALUE);
    assertDomain("int:-2147483648..-2147483648", Integer.MIN_VALUE, Integer.MIN_VALUE);
    assertRejects("int:2147483648..2147483648", "out of range");
    assertRejects("int:-2147483649..0", "out of range");
  }

  @Test
  public void mapsDatesWeightsAndBrokenSpecsAreRejected() {
    assertRejects("map", "expected");
    assertRejects("date", "expected");
    assertRejects("weighted", "expected");
    assertRejects("", "missing");
    assertRejects("   ", "missing");
    assertRejects(null, "missing");
    assertRejects("int:10..1", "inverted");
    assertRejects("int:1..2 extra", "trailing");
    assertRejects("int!=", "integer");
    assertRejects("int!0:0..1", "filter");
    assertRejects("boolean true", "trailing");
    assertRejects("Boolean", "expected");
    assertRejects("string:unicode", "ascii");
    assertRejects("string:ASCII", "ascii");
    assertRejects("listof int:0..1", "expected int");
    assertRejects("integer:1..2", "expected int");
    assertRejects("list of", "expected");
    assertRejects("list int:0..1", "of");
    assertRejects("one red,green", "of");
    assertRejects("one of", ":");
    assertRejects("one of:", "empty");
    assertRejects("one of:red,", "empty");
    assertRejects("one of:,red", "empty");
    assertRejects("one of:red,red", "duplicate");
    assertRejects("one of:red, red", "duplicate");
    assertRejects("one of:a 70, b", "weight");
    assertRejects("one of:a, b 20", "weight");
    assertRejects("one of:a 0, b 1", "at least 1");
    assertRejects("one of:a 1, a 2", "duplicate");
    assertRejects("one of:a -1, b 1", "trailing");
    assertRejects("list of int:0..1 size 3..1", "inverted");
    assertRejects("int:1..", "integer");
    assertRejects("int:1", "..");
    assertRejects("int:1..x", "integer");
    assertRejects("int:99999999999999999999..1", "integer");
    assertRejects("decimal", ":");
    assertRejects("decimal:1..2", "scale");
    assertRejects("decimal:1..2 scale -1", "scale must be");
    assertRejects("decimal:1..2 scale 9", "scale must be");
    assertRejects("decimal:1.234..2 scale 2", "decimal places");
    assertRejects("decimal:2..1 scale 0", "inverted");
    assertRejects("decimal:..1 scale 0", "decimal");
    assertRejects("decimal:1. scale 0", "decimal");
    assertRejects("decimal:1.. scale 0", "decimal");
    assertRejects("money", ":");
    assertRejects("money:1..2 scale 2", "trailing");
    assertRejects("decimal:-9223372036854775808..9223372036854775807 scale 0", "too wide");
    assertRejects("decimal:0..10000000000000000000 scale 0", "too wide");
    assertRejects("pattern", ":");
    assertRejects("pattern:", "pattern");
    assertRejects("pattern:[0-9]", "{");
    assertRejects("pattern:[0-9]{0}", "1 through");
    assertRejects("pattern:[0-9]{33}", "1 through");
    assertRejects("pattern:[A-z]{1}", "unknown");
    assertRejects("pattern:[0-9]{32}[0-9]{32}X", "longer");
    assertRejects("pattern:[", "]");
    assertRejects("pattern:AB size 1..1", "trailing");
  }

  @Test
  public void aNotEqualFilterRemovesOnlyItsHole() {
    IntFilter filter = new IntFilter(IntFilter.Kind.NE, 0);
    Span span = new Span(-2, 2);
    assertEquals(List.of(-2, -1, 1, 2), mapped(filter, span));
    assertEquals(List.of(1, 2, 3), mapped(new IntFilter(IntFilter.Kind.NE, 0), new Span(0, 3)));
    assertEquals(List.of(0, 1, 2), mapped(new IntFilter(IntFilter.Kind.NE, 3), new Span(0, 3)));
    assertEquals(List.of(-1, 1), mapped(filter, new Span(-1, 1)));
  }

  @Test
  public void aHoleIsInsideOnlyWhenTheBoundSitsInTheSpan() {
    IntFilter zero = new IntFilter(IntFilter.Kind.NE, 0);
    assertTrue(zero.hole(new Span(0, 3)));
    assertTrue(new IntFilter(IntFilter.Kind.NE, 3).hole(new Span(0, 3)));
    assertTrue(!zero.hole(new Span(1, 3)));
    assertTrue(!zero.hole(new Span(-5, -1)));
    assertTrue(!IntFilter.ANY.hole(new Span(0, 3)));
  }

  @Test
  public void comparisonsAllowTheBoundaryTheyName() {
    assertTrue(new IntFilter(IntFilter.Kind.GT, 5).allows(6));
    assertTrue(!new IntFilter(IntFilter.Kind.GT, 5).allows(5));
    assertTrue(new IntFilter(IntFilter.Kind.GE, 5).allows(5));
    assertTrue(!new IntFilter(IntFilter.Kind.GE, 5).allows(4));
    assertTrue(new IntFilter(IntFilter.Kind.LT, 0).allows(-1));
    assertTrue(!new IntFilter(IntFilter.Kind.LT, 0).allows(0));
    assertTrue(new IntFilter(IntFilter.Kind.LE, 0).allows(0));
    assertTrue(!new IntFilter(IntFilter.Kind.LE, 0).allows(1));
    assertTrue(new IntFilter(IntFilter.Kind.NE, 0).allows(1));
    assertTrue(!new IntFilter(IntFilter.Kind.NE, 0).allows(0));
    assertTrue(IntFilter.ANY.allows(0));
  }

  private static List<Integer> mapped(IntFilter filter, Span span) {
    List<Integer> got = new java.util.ArrayList<>();
    for (long k = 0; k < (long) span.max - span.min; k++) {
      got.add(filter.mapHole(span, k));
    }
    return got;
  }

  private static void assertDomain(String text, int low, int high) {
    IntGen gen = ints(CellLanguage.parse(text));
    assertEquals(low, gen.lowest());
    assertEquals(high, gen.highest());
  }

  private static void assertDomainText(Generator generator, int low, int high) {
    IntGen gen = ints(generator);
    assertEquals(low, gen.lowest());
    assertEquals(high, gen.highest());
  }

  private static void assertRejects(String text, String reason) {
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> CellLanguage.parse(text));
    assertTrue(error.getMessage(), error.getMessage().contains(reason));
  }

  private static IntGen ints(Generator generator) {
    return (IntGen) generator;
  }

  private static StringGen text(String spec) {
    return text(CellLanguage.parse(spec));
  }

  private static StringGen text(Generator generator) {
    return (StringGen) generator;
  }

  private static ListGen list(String spec) {
    return (ListGen) CellLanguage.parse(spec);
  }

  private static OneOfGen one(String spec) {
    return (OneOfGen) CellLanguage.parse(spec);
  }

  private static DecimalGen dec(String spec) {
    return (DecimalGen) CellLanguage.parse(spec);
  }
}
