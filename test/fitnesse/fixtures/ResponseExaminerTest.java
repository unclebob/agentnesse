package fitnesse.fixtures;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import fitnesse.http.MockResponseSender;
import fitnesse.http.SimpleResponse;
import fitnesse.wiki.WikiPageDummy;

public class ResponseExaminerTest {
  private ResponseExaminer examiner;

  @Before
  public void setup() throws Exception {
    examiner = new ResponseExaminer();
    FitnesseFixtureContext.sender = new MockResponseSender();
    FitnesseFixtureContext.sender.send("&lt;hello".getBytes());
    FitnesseFixtureContext.response = new SimpleResponse();
    FitnesseFixtureContext.response.setStatus(42);
  }

  @Test
  public void linesAreFoundInOrderAndAMissingLineStopsTheSearch() throws Exception {
    FitnesseFixtureContext.sender = new MockResponseSender();
    FitnesseFixtureContext.sender.send("alpha\nbeta\nalpha\n".getBytes());
    examiner.line = "alpha";
    assertTrue(examiner.inOrder());
    examiner.line = "beta";
    assertTrue(examiner.inOrder());
    examiner.line = "gamma";
    assertFalse(examiner.inOrder());
  }

  @Test
  public void aBlankLineIsNotInOrder() throws Exception {
    examiner.line = null;
    assertFalse(examiner.inOrder());
  }

  @Test
  public void shouldBeAbleToExtractContents() throws Exception {
    examiner.type = "contents";
    examiner.extractValueFromResponse();
    assertEquals("<hello", examiner.getValue());
  }

  @Test
  public void shouldBeAbleToExtractFullContents() throws Exception {
    examiner.type = "fullContents";
    examiner.extractValueFromResponse();
    assertEquals("&amp;lt;hello", examiner.getValue());
  }

  @Test
  public void shouldBeAbleToExtractStatus() throws Exception {
    examiner.type = "status";
    examiner.extractValueFromResponse();
    assertEquals("42", examiner.getValue());
  }

  @Test
  public void aLineIsFoundAfterThePreviousMatch() throws Exception {
    FitnesseFixtureContext.sender = new MockResponseSender();
    FitnesseFixtureContext.sender.send("alpha beta gamma".getBytes());
    examiner.line = null;
    assertFalse(examiner.occursAfter());
    examiner.line = "beta";
    assertTrue(examiner.occursAfter());
    examiner.line = "gamma";
    assertTrue(examiner.occursAfter());
    examiner.line = "alpha";
    assertFalse(examiner.occursAfter());
  }

  @Test
  public void stringReadsPageHtmlOrOneLine() throws Exception {
    FitnesseFixtureContext.page = new WikiPageDummy("P", "line1<br/>line2", null);
    examiner.type = "contents";
    assertEquals("<em>line1<br/>line2</em>", examiner.string());
    examiner.type = "line";
    examiner.number = 1;
    assertEquals("<em>line1", examiner.string());
    examiner.number = 2;
    assertEquals("line2</em>", examiner.string());
    examiner.number = 0;
    assertEquals(null, examiner.string());
  }

  @Test
  public void stringRejectsAnUnknownType() throws Exception {
    examiner.type = "nope";
    try {
      examiner.string();
      assertTrue(false);
    } catch (Exception e) {
      assertEquals("Bad type in ResponseExaminer", e.getMessage());
    }
  }

  @Test
  public void extractsRawStringPageAndMatcherValues() throws Exception {
    examiner.type = "rawContents";
    examiner.extractValueFromResponse();
    assertEquals("&lt;hello", examiner.getValue());

    examiner.type = "stringContents";
    examiner.extractValueFromResponse();
    assertEquals(FitnesseFixtureContext.sender.toString(), examiner.getValue());

    FitnesseFixtureContext.page = new WikiPageDummy("P", "body", null);
    examiner.type = "pageContents";
    examiner.extractValueFromResponse();
    assertEquals(FitnesseFixtureContext.page.toString(), examiner.getValue());
    examiner.type = "pageHtml";
    examiner.extractValueFromResponse();
    assertEquals("<em>body</em>", examiner.getValue());

    examiner.type = "contents";
    examiner.pattern = "(hello)";
    assertEquals("hello", examiner.found());
    examiner.type = "matchers";
    examiner.setGroup(1);
    examiner.extractValueFromResponse();
    assertEquals("hello", examiner.getValue());

    examiner.type = "unknown";
    examiner.extractValueFromResponse();
    assertEquals(null, examiner.getValue());
  }

  @Test
  public void shouldBeAbleToExtractHeaders() throws Exception {
    FitnesseFixtureContext.sender = new MockResponseSender();
    FitnesseFixtureContext.sender.send("Headers \r\n\r\n bleh".getBytes());
    examiner.type = "headers";
    examiner.extractValueFromResponse();
    assertEquals("Headers \r\n", examiner.getValue());
  }
}
