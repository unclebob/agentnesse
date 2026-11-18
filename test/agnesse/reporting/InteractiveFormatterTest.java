package agnesse.reporting;

import agnesse.testrunner.WikiTestPage;
import agnesse.testsystems.TestSummary;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.InMemoryPage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InteractiveFormatterTest {
  private StringWriter page;
  private SuiteHtmlFormatter formatter;
  private WikiPage root;

  @Before
  public void setUp() {
    page = new StringWriter();
    root = InMemoryPage.makeRoot("RooT");
    formatter = new SuiteHtmlFormatter(root, true, page);
  }

  @Test
  public void executionStatusNamesItsStyle() {
    assertEquals("Execution Status: ok", ExecutionStatus.OK.toString());
    assertEquals("Execution Status: error", ExecutionStatus.ERROR.toString());
    assertTrue(formatter.executionStatus().contains("class=\"ok\""));
  }

  @Test
  public void theSuitePageItselfIsLabeledWithItsName() {
    formatter.testStarted(new WikiTestPage(root));
    assertEquals("(RooT)", formatter.getRelativeName());
  }

  @Test
  public void aWriterFailureBecomesARuntimeException() {
    SuiteHtmlFormatter broken = new SuiteHtmlFormatter(root, false, writerThatFails());
    try {
      broken.testOutputChunk(new WikiTestPage(root), "out");
      fail("expected the write failure to escape");
    } catch (RuntimeException e) {
      assertTrue(e.getCause() instanceof IOException);
      assertEquals("disk", e.getCause().getMessage());
    }
  }

  @Test
  public void stoppingWithACauseMarksTheRunInterrupted() throws Exception {
    formatter.testSystemStopped(null, new IllegalStateException("boom"));
    assertTrue(formatter.wasInterrupted());
    formatter.close();
    String html = page.toString();
    assertTrue(html.contains("Could not complete testing"));
    assertTrue(html.contains("IllegalStateException"));
    assertTrue(html.contains("boom"));
    assertTrue(html.contains("Testing was interrupted"));
    assertTrue(html.contains("class=\"error\""));
    assertFalse(html.contains("initErrorMetadata"));
  }

  @Test
  public void stoppingWithoutACauseLeavesTheRunIntact() {
    formatter.testSystemStopped(null, null);
    assertFalse(formatter.wasInterrupted());
    assertEquals("", page.toString());
    assertTrue(formatter.executionStatus().contains("class=\"ok\""));
  }

  @Test
  public void unableToStartNamesTheTestSystem() {
    formatter.unableToStartTestSystem("slim", new IllegalStateException("down"));
    String html = page.toString();
    assertTrue(html.contains("Unable to start test system 'slim'"));
    assertTrue(html.contains("down"));
  }

  @Test
  public void setTrackingIdOffersAStopLink() {
    formatter.setTrackingId("run-1");
    String html = page.toString();
    assertTrue(html.contains("stoptest"));
    assertTrue(html.contains("run-1"));
    assertTrue(html.contains("Stop Test"));
  }

  @Test
  public void errorCountSumsWrongAndExceptions() {
    formatter.processTestResults("PassPage", new TestSummary(1, 2, 0, 3));
    assertEquals(5, formatter.getErrorCount());
  }

  @Test
  public void aCleanCloseOmitsTheErrorNavigator() throws Exception {
    WikiPage child = WikiPageUtil.addPage(root, PathParser.parse("PassPage"), "content");
    WikiTestPage testPage = new WikiTestPage(child);
    formatter.testStarted(testPage);
    formatter.testComplete(testPage, new TestSummary(2, 0, 0, 0));
    formatter.close();
    String html = page.toString();
    assertTrue(html.contains("PassPage"));
    assertTrue(html.contains("Execution Log"));
    assertTrue(html.contains("class=\\\"ok\\\""));
    assertFalse(html.contains("initErrorMetadata"));
  }

  @Test
  public void aSingleFailureShowsTheErrorNavigator() throws Exception {
    WikiPage child = WikiPageUtil.addPage(root, PathParser.parse("OneFail"), "content");
    WikiTestPage testPage = new WikiTestPage(child);
    formatter.testStarted(testPage);
    formatter.testComplete(testPage, new TestSummary(0, 1, 0, 0));
    formatter.close();
    assertTrue(page.toString().contains("initErrorMetadata"));
    assertEquals(1, formatter.getErrorCount());
  }

  @Test
  public void aFailingCloseShowsTheErrorNavigator() throws Exception {
    WikiPage child = WikiPageUtil.addPage(root, PathParser.parse("FailPage"), "content");
    formatter.testStarted(new WikiTestPage(child));
    formatter.testComplete(new WikiTestPage(child), new TestSummary(0, 1, 0, 1));
    formatter.close();
    assertTrue(page.toString().contains("initErrorMetadata"));
    assertEquals(2, formatter.getErrorCount());
  }

  private static Writer writerThatFails() {
    return new Writer() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        throw new IOException("disk");
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() {
      }
    };
  }
}
