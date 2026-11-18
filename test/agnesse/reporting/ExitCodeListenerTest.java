package agnesse.reporting;

import java.io.IOException;

import agnesse.testrunner.WikiTestPage;
import agnesse.testsystems.TestSummary;
import agnesse.wiki.WikiPageDummy;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExitCodeListenerTest {

  @Test
  public void exitCodeShouldNotBeZeroIfTestFails() throws IOException {
    ExitCodeListener listener = new ExitCodeListener();

    listener.testComplete(null, new TestSummary(0,4,0,3));

    assertEquals(1, listener.getFailCount());
  }

  @Test
  public void exitCodeShouldRepresentNumberOfFailingTests() throws IOException {
    ExitCodeListener listener = new ExitCodeListener();

    listener.testComplete(null, new TestSummary(0,4,0,3));
    listener.testComplete(null, new TestSummary(0,4,0,3));
    listener.testComplete(null, new TestSummary(0,4,0,3));
    listener.testComplete(null, new TestSummary(0,4,0,3));

    assertEquals(4, listener.getFailCount());
  }

  @Test
  public void aPageThatRanNoAssertionsFailsTheExitCode() {
    ExitCodeListener listener = new ExitCodeListener();
    listener.testComplete(new WikiTestPage(new WikiPageDummy("Empty", "", null)), new TestSummary(0, 0, 0, 0));
    listener.testComplete(new WikiTestPage(new WikiPageDummy("SuiteSetUp", "", null)), new TestSummary(0, 0, 0, 0));
    assertEquals(1, listener.getFailCount());
  }

  @Test
  public void exitCodeShouldNotBeZeroIfTestSystemStoppedWithException() throws IOException {
    ExitCodeListener listener = new ExitCodeListener();

    listener.testSystemStopped(null, new Exception());

    assertEquals(1, listener.getFailCount());
  }

  @Test
  public void aPassingPageAndQuietCallbacksLeaveTheExitCodeAlone() {
    ExitCodeListener listener = new ExitCodeListener();
    listener.testSystemStarted(null);
    listener.testStarted(null);
    listener.testOutputChunk(null, "chunk");
    listener.testAssertionVerified(null, null);
    listener.testExceptionOccurred(null, null);
    listener.testSystemStopped(null, null);
    listener.testComplete(new WikiTestPage(new WikiPageDummy("Passes", "", null)), new TestSummary(3, 0, 1, 0));
    assertEquals(0, listener.getFailCount());
  }

}