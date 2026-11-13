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
  public void aPlainSymbolPassSplitsThePrefixAndTheValue() {
    SlimTestResult result = (SlimTestResult) SlimTestResult.pass("$S->[cat]", "$S->[cat]");
    assertEquals("<span class=\"pass\">$S-&gt;[cat]</span>", CellMarkup.format("x", result));
  }

  @Test
  public void htmlQualificationMatchesTheCellsSlimStillRenders() {
    assertFalse(CellMarkup.qualifiesAsHtml("Some text"));
    assertFalse(CellMarkup.qualifiesAsHtml("less < more"));
    assertTrue(CellMarkup.qualifiesAsHtml("<table><tr><td>blah etc.</td></tr></table>"));
    assertTrue(CellMarkup.qualifiesAsHtml("<table>\n\t<tr>\n\t\t<td>table2</td>\n\t</tr>\n</table>"));
    assertTrue(CellMarkup.qualifiesAsHtml("<img src='http://fitnesse.org'/>"));
    assertFalse(CellMarkup.qualifiesAsHtml("<table><tr><td>blah etc.</td></tr></table>And extra text"));
    assertTrue(CellMarkup.qualifiesAsHtml("<div>less < more</div>"));
    assertTrue(CellMarkup.qualifiesAsHtml("$foo->[<div>less < more</div>]"));
    assertFalse(CellMarkup.qualifiesAsHtml("<content>blah</content>"));
    assertFalse(CellMarkup.qualifiesAsHtml("<html><head></head><body></body></html>"));
  }

  @Test
  public void aConvertedListKeepsAHashTableAndEscapesOtherHtml() {
    String hash = "$list<-[[a, b, <table class=\"hash_table\"><tr><td>first</td><td>1</td></tr></table>]]";
    String image = "$list<-[[a, b, <img src='http://fitnesse.org'/>]]";
    assertTrue(CellMarkup.qualifiesAsConvertedList("[a, b, c]"));
    assertTrue(CellMarkup.qualifiesAsConvertedList(hash));
    assertTrue(CellMarkup.qualifiesAsConvertedList("$list<-[[a, b]]"));
    assertFalse(CellMarkup.format("x", SlimTestResult.plain(hash)).contains("&lt;"));
    assertTrue(CellMarkup.format("x", SlimTestResult.plain(image)).contains("&lt;"));
  }

  @Test
  public void anIgnoredCellRendersTheMessageOrTheOriginalText() {
    assertEquals("<span class=\"ignore\">original content</span>",
      CellMarkup.format("original content", SlimTestResult.ignore()));
    assertEquals("<span class=\"ignore\">a message</span>",
      CellMarkup.format("original content", SlimTestResult.ignore("a message")));
  }

  @Test
  public void aListWrappedInASymbolIsStillAList() {
    assertTrue(CellMarkup.qualifiesAsConvertedList("$S->[[a]]"));
    assertFalse(CellMarkup.qualifiesAsConvertedList("plain"));
    assertTrue(CellMarkup.qualifiesAsConvertedList("[a, b]"));
  }
}
