package agnesse.markdown;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DetailsRegionsTest {

  @Test
  public void aDetailsBlockNotAtTheStartStillCollapsesAndStopsAtItsEnd() {
    List<String> chunks = new ArrayList<>();
    String html = DetailsRegions.render("A\n<details><summary>T</summary>x</details>", chunk -> keep(chunks, chunk));

    assertTrue(html, html.contains("agnesse-section"));
    assertEquals(List.of("A\n", "x"), chunks);
    assertTrue(html.indexOf("x") < html.indexOf("</details>"));
  }

  @Test
  public void textAfterTheBlockStaysOutsideAndATrailingNewlineIsConsumed() {
    List<String> chunks = new ArrayList<>();
    String html = DetailsRegions.render("<details><summary>T</summary>x</details>\nAFTER", chunk -> keep(chunks, chunk));

    assertEquals(List.of("", "x", "AFTER"), chunks);
    assertTrue(html.indexOf("AFTER") > html.indexOf("</details>"));
  }

  @Test
  public void aBlockWithNoTrailingNewlineDoesNotReadPastTheEnd() {
    List<String> chunks = new ArrayList<>();
    String html = DetailsRegions.render("<details><summary>T</summary>x</details>", chunk -> keep(chunks, chunk));

    assertEquals(List.of("", "x"), chunks);
    assertTrue(html.contains("</details>"));
  }

  @Test
  public void textTightAgainstTheCloseStaysIntact() {
    List<String> chunks = new ArrayList<>();
    DetailsRegions.render("<details><summary>T</summary>x</details>AFTER", chunk -> keep(chunks, chunk));

    assertEquals(List.of("", "x", "AFTER"), chunks);
  }

  @Test
  public void theBodyStartsOnTheLineAfterTheSummary() {
    List<String> chunks = new ArrayList<>();
    String html = DetailsRegions.render("<details><summary>T</summary>\nBODY\n</details>", chunk -> keep(chunks, chunk));

    assertEquals(List.of("", "BODY\n"), chunks);
    assertTrue(html, html.contains("&gt;</button>T</summary>"));
  }

  @Test
  public void aSummaryTightAgainstItsBodyKeepsThatBody() {
    List<String> chunks = new ArrayList<>();
    DetailsRegions.render("<details><summary>T</summary>BODY</details>", chunk -> keep(chunks, chunk));

    assertEquals(List.of("", "BODY"), chunks);
  }

  @Test
  public void aBlockWithoutASummaryStartsTheBodyOnTheNextLine() {
    List<String> chunks = new ArrayList<>();
    DetailsRegions.render("<details>\nBODY\n</details>", chunk -> keep(chunks, chunk));

    assertEquals(List.of("", "BODY\n"), chunks);
  }

  @Test
  public void aDetailsTagWithNoNewlineAndNoCloseHasAnEmptyBody() {
    List<String> chunks = new ArrayList<>();
    DetailsRegions.render("<details>", chunk -> keep(chunks, chunk));

    assertEquals(List.of(""), chunks);
  }

  @Test
  public void aCloseBeforeTheSummaryLeavesTheTitleBlank() {
    List<String> chunks = new ArrayList<>();
    String html = DetailsRegions.render("<details></summary><summary>T</summary>x</details>", chunk -> keep(chunks, chunk));

    assertTrue(html, html.contains("&gt;</button></summary>"));
    assertEquals(List.of("", "<summary>T</summary>x"), chunks);
  }

  @Test
  public void aStrayCloseSummaryKeepsTheTitleBlank() {
    List<String> chunks = new ArrayList<>();
    String html = DetailsRegions.render("<details></summary>\nBODY\n</details>", chunk -> keep(chunks, chunk));

    assertTrue(html, html.contains("&gt;</button></summary>"));
    assertEquals(List.of("", "BODY\n"), chunks);
  }

  @Test
  public void anUnclosedSummaryHasABlankTitle() {
    String html = DetailsRegions.render("<details><summary>T\nBODY\n</details>", chunk -> chunk);

    assertTrue(html, html.contains("&gt;</button></summary>"));
    assertFalse(html, html.contains("T"));
  }

  @Test
  public void attributesWithoutASummaryStillStartTheBodyOnTheNextLine() {
    List<String> chunks = new ArrayList<>();
    DetailsRegions.render("<details open>\nBODY\n</details>", chunk -> keep(chunks, chunk));

    assertEquals(List.of("", "BODY\n"), chunks);
  }

  @Test
  public void aSummaryThatEndsTheBlockHasAnEmptyBody() {
    List<String> chunks = new ArrayList<>();
    String html = DetailsRegions.render("<details><summary>T</summary>", chunk -> keep(chunks, chunk));

    assertEquals(List.of(""), chunks);
    assertTrue(html, html.contains("&gt;</button>T</summary>"));
  }

  @Test
  public void nestedBlocksCloseAtTheirOwnTagAndTheTailStaysOutside() {
    String html = MarkdownRenderer.render("""
      <details>
      <summary>Out</summary>
      <details>
      <summary>In</summary>
      inner
      </details>
      outer
      </details>
      AFTER
      """);

    assertTrue(html, html.contains(">Out<"));
    assertTrue(html, html.contains(">In<"));
    assertTrue(html, html.indexOf("inner") < html.indexOf("outer"));
    assertTrue(html, html.indexOf("AFTER") > html.lastIndexOf("</details>"));
    assertEquals(2, html.split("agnesse-section", -1).length - 1);
  }

  private static String keep(List<String> chunks, String chunk) {
    chunks.add(chunk);
    return chunk;
  }

  @Test
  public void anEmptyDocumentRendersNothing() {
    assertEquals("", DetailsRegions.render("", chunk -> "X" + chunk));
  }
}
