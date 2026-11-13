package fitnesse.testsystems.slim;

import fitnesse.testsystems.slim.results.SlimTestResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CellMarkupTest {

  @Test
  public void plainPassFailIgnoreAndErrorAreSpans() {
    assertEquals("<span class=\"pass\">7</span>", CellMarkup.format("7", SlimTestResult.pass("7")));
    assertEquals("<span class=\"fail\">no</span>", CellMarkup.format("no", SlimTestResult.fail("no")));
    assertEquals("cell <span class=\"ignore\">Test not run</span>",
      CellMarkup.format("cell", SlimTestResult.testNotRun()));
    assertEquals("<span class=\"ignore\">skip</span>", CellMarkup.format("cell", SlimTestResult.ignore("skip")));
    assertEquals("cell <span class=\"error\">boom</span>", CellMarkup.format("cell", SlimTestResult.error("boom")));
    assertEquals(null, CellMarkup.format("cell", SlimTestResult.plain()));
  }

  @Test
  public void htmlAndSymbolsStayMarkupAndPlainTextIsEscaped() {
    String html = (String) ((SlimTestResult) SlimTestResult.pass("<div>ok</div>", "<div>ok</div>")).getExpected();
    assertEquals("<span class=\"pass\"><div>ok</div></span>",
      CellMarkup.format("x", (SlimTestResult) SlimTestResult.pass(html, html)));
    assertTrue(CellMarkup.format("x",
      (SlimTestResult) SlimTestResult.pass("$S->[<div>ok</div>]", "$S->[<div>ok</div>]")).contains("$S-&gt;"));
    assertEquals("[<div>no</div>] <span class=\"fail\">expected [<div>yes</div>]</span>",
      CellMarkup.format("x", SlimTestResult.fail("<div>no</div>", "<div>yes</div>")));
    assertTrue(CellMarkup.format("x", SlimTestResult.fail("cat", "car")).contains("expected"));
    assertEquals("[left] <span class=\"fail\">right</span>",
      CellMarkup.format("x", SlimTestResult.fail("left", null, "right")));
    assertEquals("&lt;b&gt;", CellMarkup.format("x", SlimTestResult.plain("<b>")));
    assertEquals("[<table><tr></tr></table>]",
      CellMarkup.format("x", SlimTestResult.plain("[<table><tr></tr></table>]")));
  }

  @Test
  public void aListWrappedInASymbolIsStillAList() {
    assertTrue(CellMarkup.qualifiesAsConvertedList("$S->[[a]]"));
    assertFalse(CellMarkup.qualifiesAsConvertedList("plain"));
    assertTrue(CellMarkup.qualifiesAsConvertedList("[a, b]"));
  }
}
