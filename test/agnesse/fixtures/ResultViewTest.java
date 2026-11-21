package agnesse.fixtures;

import agnesse.html.HtmlUtil;
import agnesse.http.MockResponseSender;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResultViewTest {
  @After
  public void clearResponse() {
    AgNesseFixtureContext.sender = null;
  }

  @Test
  public void ndjsonShowsTheTableTheFailureAndTheNote() {
    String html = ResultView.html(
      "{\"event\":\"page-started\",\"path\":\"PropertyAmount\"}\n"
      + "{\"event\":\"table-done\",\"block\":{\"rows\":[{\"cells\":["
      + "{\"value\":\"price\"},"
      + "{\"status\":\"fail\",\"actual\":\"1.00\",\"expected\":\"true\"},"
      + "{\"actual\":\"trial 1 of 1 drew 2.43; shrunk\",\"expected\":\"1\"},"
      + "{\"actual\":\"<script>alert(1)</script>\"},"
      + "{\"symbol\":\"SECRET_token\",\"value\":\"hidden\"},"
      + "{\"symbol\":\"SECRETx\",\"value\":\"shown\"}"
      + "]}]}}\n"
      + "{\"event\":\"output\",\"text\":\"plain note\"}\n"
      + "{\"event\":\"page-done\",\"status\":\"fail\"}\n");

    assertTrue(html, HtmlUtil.isValidTableCellContent(html));
    assertTrue(html, html.contains("<td>price</td>"));
    assertTrue(html, html.contains("<td class=\"fail\">1.00</td>"));
    assertTrue(html, html.contains("drew 2.43; shrunk"));
    assertTrue(html, html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    assertTrue(html, html.contains("$SECRET_token-&gt;[*****]"));
    assertTrue(html, html.contains("$SECRETx&lt;-[shown]"));
    assertTrue(html, html.contains("<p class=\"error\">plain note</p>"));
    assertFalse(html, html.contains("<script>"));
    assertFalse(html, html.contains("class=\"fail\">true"));
    assertFalse(html, html.contains("hidden"));
  }

  @Test
  public void aRunDocumentAndAnEmptyBodyStillRender() {
    String html = ResultView.html(
      "{\"schemaVersion\":1,\"run\":{\"pages\":[{\"blocks\":[{\"rows\":[{\"cells\":[{\"value\":\"seed\"},{\"actual\":\"1\"}]}]}]}]}}");
    assertTrue(html, html.contains("<td>seed</td>"));
    assertTrue(html, html.contains("<td>1</td>"));
    assertTrue(HtmlUtil.isValidTableCellContent(ResultView.html("")));
    assertTrue(HtmlUtil.isValidTableCellContent(ResultView.html("not json")));
  }

  @Test
  public void anErrorCellAndAHostileStatusAreSafe() {
    String html = ResultView.html(
      "{\"event\":\"table-done\",\"block\":{\"rows\":[{\"cells\":["
      + "{\"error\":{\"message\":\"<b>boom</b>\"}},"
      + "{\"actual\":\"nope\",\"error\":{\"message\":\"later\"}},"
      + "{\"status\":\"fail\",\"error\":{\"message\":\"kept\"},\"actual\":\"kept-fail\"},"
      + "{\"error\":\"plain\"},"
      + "{\"status\":\"fail><script>\",\"actual\":\"nope\"}"
      + "]}]}}");
    assertTrue(html, html.contains("class=\"error\""));
    assertTrue(html, html.contains("class=\"fail\""));
    assertTrue(html, html.contains("&lt;b&gt;boom&lt;/b&gt;"));
    assertTrue(html, html.contains(">nope later</td>"));
    assertTrue(html, html.contains("class=\"fail\">kept-fail kept</td>"));
    assertFalse(html, html.contains(">plain</td>"));
    assertFalse(html, html.contains("fail><script>"));
    assertTrue(html, html.contains(">nope</td>"));
  }

  @Test
  public void renderedResultReadsTheLastResponse() throws Exception {
    AgNesseFixtureContext.sender = new MockResponseSender();
    AgNesseFixtureContext.sender.send((
      "{\"event\":\"table-done\",\"block\":{\"rows\":[{\"cells\":[{\"actual\":\"1.00\",\"status\":\"fail\"}]}]}}\n"
    ).getBytes());

    String html = new ResultView().renderedResult();

    assertTrue(html, html.contains("class=\"fail\""));
    assertTrue(html, html.contains("1.00"));
  }

  @Test
  public void renderedResultIsEmptyWhenNothingWasRequested() throws Exception {
    String html = new ResultView().renderedResult();
    assertTrue(HtmlUtil.isValidTableCellContent(html));
    assertFalse(html.contains("<td"));
  }
}
