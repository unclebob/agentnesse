package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DrawTest {
  @Test
  public void aSinglePointRangeDrawsThatPoint() {
    assertEquals("7", draw("int:7..7", 1L));
    assertEquals("5", draw("int>=5:5..5", 2L));
    assertEquals("5", draw("int>4:5..5", 3L));
    assertEquals("5", draw("int<=5:5..5", 4L));
    assertEquals("5", draw("int<6:5..5", 5L));
    assertEquals("6", draw("int>5:6..6", 6L));
    assertEquals("4", draw("int<5:4..4", 7L));
    assertEquals("1", draw("int!=0:1..1", 8L));
    assertEquals("2147483647", draw("int:2147483647..2147483647", 9L));
    assertEquals("-2147483648", draw("int:-2147483648..-2147483648", 10L));
    assertEquals("", draw("string length 0..0", 11L));
    assertEquals("", draw("string:ascii length 0..0", 12L));
    assertEquals("[]", draw("list of int:0..0 size 0..0", 13L));
    assertEquals("[3]", draw("list of int:3..3 size 1..1", 14L));
    assertEquals(1, draw("string length 1..1", 15L).length());
    for (long seed = 0; seed < 20; seed++) {
      assertEquals("0", draw("int!=1:0..1", seed));
      assertEquals("1", draw("int!=0:0..1", seed));
    }
  }

  @Test
  public void theSameSeedDrawsTheSameSequence() {
    List<String> first = sequence("int:-10..10", 42L, 5);
    assertEquals(List.of("-6", "-9", "-2", "-3", "-4"), first);
    assertEquals(first, sequence("int:-10..10", 42L, 5));
    assertEquals(sequence("list of boolean size 0..4", 7L, 4),
        sequence("list of boolean size 0..4", 7L, 4));
  }

  @Test
  public void oneSeedDoesNotAdvanceAnother() {
    Generator spec = CellLanguage.parse("list of int:-20..20 size 0..4");
    Random a = new Seed(8L).random();
    Random b = new Seed(8L).random();
    String first = spec.draw(a).cell();
    spec.draw(b);
    spec.draw(b);
    assertEquals(first, spec.draw(new Seed(8L).random()).cell());
    Random fresh = new Seed(8L).random();
    spec.draw(fresh);
    assertEquals(spec.draw(fresh).cell(), spec.draw(a).cell());
  }

  @Test
  public void twoParsedSpecsDoNotShareDraws() {
    Generator first = CellLanguage.parse("one of:red,green,blue");
    Generator second = CellLanguage.parse("one of:red,green,blue");
    assertEquals(first.draw(new Seed(4L).random()).cell(), second.draw(new Seed(4L).random()).cell());
  }

  @Test
  public void drawsStayInsideTheDomainAndRespectFilters() {
    assertIn("int:-10..10", -10, 10, null);
    assertIn("int!=0:-5..5", -5, 5, 0);
    assertIn("int>0:0..5", 1, 5, null);
    assertIn("int>=0:-2..2", 0, 2, null);
    assertIn("int<0:-5..5", -5, -1, null);
    assertIn("int<=0:-5..5", -5, 0, null);
    assertIn("int!=9:0..2", 0, 2, null);
    assertIn("int!=-9:-2..0", -2, 0, null);
    assertIn("int!=0:0..3", 1, 3, 0);
    assertIn("int!=3:0..3", 0, 2, 3);
  }

  @Test
  public void stringsUseTheAsciiAlphabetAndListsUseSlimText() {
    for (long seed = 0; seed < 20; seed++) {
      String cell = draw("string:ascii length 1..8", seed);
      assertTrue(cell.length() >= 1 && cell.length() <= 8);
      for (int i = 0; i < cell.length(); i++) {
        assertTrue(StringGen.ASCII.indexOf(cell.charAt(i)) >= 0);
      }
    }
    String nested = draw("list of list of int:1..1 size 1..1 size 1..1", 3L);
    assertEquals("[[1]]", nested);
    String choice = draw("list of one of:red,blue size 1..1", 1L);
    assertTrue(choice.equals("[red]") || choice.equals("[blue]"));
    String flag = draw("list of boolean size 2..2", 2L);
    assertTrue(flag.equals("[true, true]") || flag.equals("[true, false]")
        || flag.equals("[false, true]") || flag.equals("[false, false]"));
  }

  @Test
  public void booleansAndChoicesReplayFromTheSeed() {
    assertEquals("true", draw("boolean", 0L));
    assertEquals("false", draw("boolean", 4096L));
    assertEquals("red", draw("one of:red,green,blue", 0L));
    assertEquals("green", draw("one of:red,green,blue", 2L));
    assertEquals("blue", draw("one of:red,green,blue", 3L));
    assertEquals(sequence("one of:a 70, b 20, c 10", 5L, 6),
        sequence("one of:a 70, b 20, c 10", 5L, 6));
    assertEquals("only", draw("one of:only 4", 9L));
  }

  @Test
  public void aSmallWeightStillAppears() {
    int rare = 0;
    int common = 0;
    Generator spec = CellLanguage.parse("one of:a 70, b 20, c 10");
    Random random = new Seed(1L).random();
    for (int i = 0; i < 2000; i++) {
      String cell = spec.draw(random).cell();
      if ("a".equals(cell)) {
        common++;
      }
      if ("c".equals(cell)) {
        rare++;
      }
    }
    assertTrue(common > 1000);
    assertTrue(rare > 50);
    assertTrue(common > rare);
  }

  @Test
  public void decimalsAreFixedPointAndReplayFromTheSeed() {
    assertEquals("1.50", draw("decimal:1.50..1.50 scale 2", 1L));
    assertEquals("0.00", draw("money:0..0", 2L));
    assertEquals("7", draw("decimal:7..7 scale 0", 3L));
    assertEquals("-1.25", draw("decimal:-1.25..-1.25 scale 2", 4L));
    List<String> money = sequence("money:0..2", 11L, 4);
    assertEquals(money, sequence("decimal:0..2 scale 2", 11L, 4));
    for (String cell : money) {
      assertTrue(cell.matches("-?\\d+\\.\\d{2}"));
      assertTrue(new java.math.BigDecimal(cell).compareTo(new java.math.BigDecimal("2.00")) <= 0);
    }
    for (long seed = 0; seed < 30; seed++) {
      String cell = draw("decimal:-1.5..1.5 scale 1", seed);
      assertTrue(cell, cell.endsWith(".0") || cell.endsWith(".5") || cell.contains("."));
      java.math.BigDecimal value = new java.math.BigDecimal(cell);
      assertTrue(value.compareTo(new java.math.BigDecimal("-1.5")) >= 0);
      assertTrue(value.compareTo(new java.math.BigDecimal("1.5")) <= 0);
      assertEquals(1, value.scale());
    }
    assertEquals("[0.25]", draw("list of decimal:0.25..0.25 scale 2 size 1..1", 5L));
  }

  @Test
  public void patternsDrawIdentifiersFromTheSeed() {
    String id = draw("pattern:REG-[0-9]{4}", 1L);
    assertTrue(id, id.matches("REG-[0-9]{4}"));
    assertEquals(id, draw("pattern:REG-[0-9]{4}", 1L));
    assertEquals(id, draw("pattern: REG- [0-9] {4}", 1L));
    String part = draw("pattern:PN-[A-Z]{2}[0-9]{2}", 4L);
    assertTrue(part, part.matches("PN-[A-Z]{2}[0-9]{2}"));
    assertEquals("AB", draw("pattern:AB", 6L));
    assertTrue(draw("pattern:[a-z]{1}", 7L).matches("[a-z]"));
    assertEquals("[AB]", draw("list of pattern:AB size 1..1", 8L));
    assertEquals("[size]", draw("list of pattern:size size 1..1", 9L));
  }

  @Test
  public void adversarialStringsMixOrdinaryTextWithHostilePayloads() {
    assertEquals("", draw("string:adversarial length 0..0", 1L));
    assertEquals("", draw("string:adversarial length 0..0", 2L));
    assertEquals(sequence("string:adversarial length 0..30", 8L, 5),
        sequence("string:adversarial length 0..30", 8L, 5));
    Generator spec = CellLanguage.parse("string:adversarial length 0..40");
    List<String> catalog = Adversarial.fitting(new Span(0, 40));
    boolean ordinary = false;
    boolean hostile = false;
    boolean markup = false;
    boolean emoji = false;
    boolean longText = false;
    Random random = new Seed(3L).random();
    for (int i = 0; i < 400; i++) {
      String cell = spec.draw(random).cell();
      if (catalog.contains(cell)) {
        hostile = true;
      } else {
        ordinary = true;
        assertTrue(cell.length() <= 40);
      }
      if (cell.contains("<script>")) {
        markup = true;
      }
      if (cell.contains("\uD83D\uDE00")) {
        emoji = true;
      }
      if (cell.equals("A".repeat(40))) {
        longText = true;
      }
    }
    assertTrue(ordinary);
    assertTrue(hostile);
    assertTrue(markup);
    assertTrue(emoji);
    assertTrue(longText);
    assertTrue(catalog.contains("|"));
    assertTrue(catalog.contains("e\u0301"));
    assertTrue(catalog.contains("\u202Ertl"));
    assertTrue(catalog.contains("a\u200Bb"));
  }

  private static void assertIn(String spec, int low, int high, Integer banned) {
    for (long seed = 0; seed < 40; seed++) {
      int n = Integer.parseInt(draw(spec, seed));
      assertTrue(spec + " drew " + n, n >= low && n <= high);
      if (banned != null) {
        assertFalse(n == banned);
      }
    }
  }

  private static List<String> sequence(String spec, long seed, int count) {
    Generator generator = CellLanguage.parse(spec);
    Random random = new Seed(seed).random();
    List<String> out = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      out.add(generator.draw(random).cell());
    }
    return out;
  }

  private static String draw(String spec, long seed) {
    return CellLanguage.parse(spec).draw(new Seed(seed).random()).cell();
  }
}
