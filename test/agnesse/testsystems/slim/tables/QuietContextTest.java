package agnesse.testsystems.slim.tables;

import agnesse.testrunner.WikiTestPage;
import agnesse.testsystems.slim.SlimTestContextImpl;
import agnesse.wiki.WikiPageDummy;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuietContextTest {
  @Test
  public void onlyAWrongOrAnExceptionIsAFailure() {
    QuietContext quiet = quiet();
    assertFalse(quiet.failed());
    quiet.incrementPassedTestsCount();
    assertFalse(quiet.failed());
    quiet.incrementIgnoredTestsCount();
    assertFalse(quiet.failed());
    quiet.incrementFailedTestsCount();
    assertTrue(quiet.failed());
  }

  @Test
  public void anExceptionIsAFailure() {
    QuietContext quiet = quiet();
    quiet.incrementErroredTestsCount();
    assertTrue(quiet.failed());
  }

  private static QuietContext quiet() {
    return new QuietContext(new SlimTestContextImpl(new WikiTestPage(new WikiPageDummy())));
  }
}
