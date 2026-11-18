package agnesse.testsystems.slim;

import agnesse.slim.SlimServer;
import agnesse.testsystems.slim.results.SlimExceptionResult;
import agnesse.testsystems.slim.results.SlimTestResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MarkdownTableHtmlTest {
  @Test
  public void anExceptionCellShowsTheMessageOrTheRawException() {
    MarkdownTable escaped = table();
    escaped.updateContent(0, 1, new SlimExceptionResult("k", "message:<<hello <there>>>"));
    assertTrue(escaped.toHtml(), escaped.toHtml().contains("b <span class=\"error\">hello &lt;there&gt;</span>"));

    MarkdownTable html = table();
    html.updateContent(0, 1, new SlimExceptionResult("k", "message:<<<em>hi</em>>>"));
    assertTrue(html.toHtml(), html.toHtml().contains("b <span class=\"error\"><em>hi</em></span>"));

    MarkdownTable raw = table();
    raw.updateContent(0, 1, new SlimExceptionResult("k", "boom <x>"));
    assertTrue(raw.toHtml(), raw.toHtml().contains("b <span class=\"error\">boom &lt;x&gt;</span>"));

    MarkdownTable failed = table();
    failed.updateContent(0, 1, new SlimExceptionResult("k", SlimServer.EXCEPTION_STOP_TEST_TAG));
    assertTrue(failed.toHtml(), failed.toHtml().contains("class=\"fail\""));
  }

  @Test
  public void aResultWithNoExecutionStatusShowsTheMessageOrTheCell() {
    MarkdownTable plain = table();
    plain.updateContent(0, 1, SlimTestResult.plain());
    assertTrue(plain.toHtml(), plain.toHtml().contains("<td>b</td>"));
    assertFalse(plain.toHtml(), plain.toHtml().contains("<span"));

    MarkdownTable message = table();
    message.updateContent(0, 1, SlimTestResult.plain("a < b"));
    assertTrue(message.toHtml(), message.toHtml().contains("<td>a &lt; b</td>"));

    MarkdownTable passed = table();
    passed.updateContent(0, 1, SlimTestResult.pass("ok"));
    assertTrue(passed.toHtml(), passed.toHtml().contains("<span class=\"pass\">ok</span>"));
  }

  @Test
  public void aChildTableIsAScenarioRow() {
    MarkdownTable parent = table();
    parent.appendChildTable(0, table());

    String html = parent.toHtml();
    assertTrue(html, html.contains("<tr class=\"scenario closed\"></tr>"));
    assertTrue(html, html.contains("colspan=\"1\""));
  }

  @Test
  public void aChildTableFollowsTheRowItWasPlacedOn() {
    MarkdownTable parent = table();
    parent.appendChildTable(1, table());

    String html = parent.toHtml();
    assertTrue(html, html.indexOf("scenario") > html.indexOf(">b<"));
  }

  @Test
  public void aPassDoesNotReplaceAnotherPassOrMarkTheRowIgnored() {
    MarkdownTable table = table();
    table.updateContent(0, 1, SlimTestResult.pass("one"));
    assertEquals("pass", table.tableRecord().status());
    assertEquals("one", table.tableRecord().actual());
    assertNull(table.rowRecord(1).status());

    table.updateContent(0, 1, SlimTestResult.pass("two"));
    assertEquals("one", table.tableRecord().actual());

    table.updateContent(0, 1, SlimTestResult.ignore("skip"));
    assertEquals("ignore", table.rowRecord(1).status());
  }

  @Test
  public void aWeakerFailureStillReplacesTheTableRecord() {
    MarkdownTable table = table();
    table.updateContent(0, 1, new SlimExceptionResult("k", "message:<<bad>>"));
    assertEquals("error", table.tableRecord().status());

    table.updateContent(0, 1, new SlimExceptionResult("k", SlimServer.EXCEPTION_IGNORE_ALL_TESTS_TAG));
    assertEquals("ignore", table.tableRecord().status());
  }

  @Test
  public void anExceptionOnTheRowIsInsertedUnderThatRow() {
    MarkdownTable table = table();
    table.updateContent(-1, 0, new SlimExceptionResult("k", "message:<<boom>>"));

    assertEquals("a", table.getCellContents(0, 0));
    assertTrue(table.getCellContents(0, 1), table.getCellContents(0, 1).contains("boom"));
    assertNull(table.rowRecord(0).status());
    assertEquals("error", table.rowRecord(1).status());
  }

  @Test
  public void anExceptionCellShowsItsMessageBesideTheOriginalText() {
    MarkdownTable table = table();
    table.updateContent(0, 1, new SlimExceptionResult("k", "message:<<boom>>"));
    assertTrue(table.getCellContents(0, 1), table.getCellContents(0, 1).contains("b boom"));

    MarkdownTable raw = table();
    raw.updateContent(0, 1, new SlimExceptionResult("k", "boom"));
    assertEquals("b", raw.getCellContents(0, 1));
  }

  @Test
  public void locatingABlockNamesTheFailure() {
    MarkdownTable table = table();
    table.locate("Suite.Page", "script");
    SlimExceptionResult named = new SlimExceptionResult("k", "message:<<boom>>");
    table.updateContent(0, 1, named);
    assertEquals("Suite.Page", named.failure().page());
    assertEquals("script", named.failure().block());

    MarkdownTable blank = table();
    blank.locate("Suite.Page", null);
    SlimExceptionResult unnamed = new SlimExceptionResult("k", "message:<<boom>>");
    blank.updateContent(0, 1, unnamed);
    assertEquals("", unnamed.failure().block());
  }

  @Test
  public void markingACellNotAnAssertionKeepsAQuietCellQuiet() {
    MarkdownTable failed = table();
    failed.updateContent(0, 1, SlimTestResult.fail("no"));
    assertTrue(failed.cellRecord(1, 0).assertion());
    failed.markNotAnAssertion(0, 1);
    assertFalse(failed.cellRecord(1, 0).assertion());
    assertTrue(failed.cellRecord(1, 0).trouble());

    MarkdownTable quiet = table();
    quiet.markNotAnAssertion(0, 0);
    assertNull(quiet.cellRecord(0, 0).status());
  }

  private static MarkdownTable table() {
    return MarkdownTable.parse("| a |\n| --- |\n| b |\n");
  }
}
