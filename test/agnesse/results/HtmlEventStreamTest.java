package agnesse.results;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HtmlEventStreamTest {
  @Test
  public void aLineBecomesAScriptAndMarkupStaysInTheJson() throws Exception {
    StringBuilder out = new StringBuilder();
    HtmlEventStream stream = new HtmlEventStream(out);
    stream.append("{\"html\":\"<b>exception</b>\"}");
    assertEquals("", out.toString());
    stream.append('\n');
    String html = out.toString();
    assertTrue(html.startsWith("<script>AgentNesseResults.apply("));
    assertTrue(html.endsWith(");</script>\n"));
    assertFalse(html.contains("<b>"));
    assertTrue(html.contains("exception"));
  }

  @Test
  public void theSecondLineDoesNotKeepACharacterFromTheFirst() throws Exception {
    StringBuilder out = new StringBuilder();
    HtmlEventStream stream = new HtmlEventStream(out);
    stream.append("{\"a\":1}\n{\"b\":2}\n");
    String html = out.toString();
    assertTrue(html, html.contains("apply({\"b\":2})"));
    assertFalse(html, html.contains("apply({{\"b\""));
  }

  @Test
  public void aBlankLineIsSkipped() throws Exception {
    StringBuilder out = new StringBuilder();
    new HtmlEventStream(out).append("\n");
    assertEquals("", out.toString());
  }
}
