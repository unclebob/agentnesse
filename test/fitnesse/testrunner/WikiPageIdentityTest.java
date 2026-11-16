package fitnesse.testrunner;

import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import fitnesse.wiki.fs.InMemoryPage;
import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class WikiPageIdentityTest {
  @Test
  public void aPageWithoutATestSystemIsSlim() throws Exception {
    WikiPage page = page("PlainPage", "just a page");

    assertEquals("slim", new WikiPageIdentity(page).testSystem());
  }

  @Test
  public void anUnsetRunnerMatchesAnEmptyRunner() throws Exception {
    WikiPage unset = page("Unset", "body");
    WikiPage empty = page("Empty", "!define TEST_RUNNER {}\n!define COMMAND_PATTERN {}\n");

    assertEquals(new WikiPageIdentity(unset), new WikiPageIdentity(empty));
  }

  @Test
  public void slimLaunchSettingsSeparatePages() throws Exception {
    WikiPage plain = page("Plain", "body");
    String[] settings = {
      "TEST_RUNNER", "COMMAND_PATTERN", "SLIM_PORT", "SLIM_HOST", "SLIM_FLAGS", "SLIM_VERSION",
      "MANUALLY_START_TEST_RUNNER", "MANUALLY_START_TEST_RUNNER_ON_DEBUG",
      "REMOTE_DEBUG_COMMAND", "REMOTE_DEBUG_RUNNER",
      "slim.port", "slim.host", "slim.flags", "slim.timeout", "slim.pool.size"
    };
    for (String name : settings) {
      WikiPage changed = page("Changed" + name, "!define " + name + " {other}\n");
      assertNotEquals(name, new WikiPageIdentity(plain), new WikiPageIdentity(changed));
    }
  }

  @Test
  public void equalsRejectsNullAndOtherTypes() throws Exception {
    WikiPageIdentity identity = new WikiPageIdentity(page("Plain", "body"));

    assertEquals(false, identity.equals(null));
    assertEquals(false, identity.equals("slim"));
  }

  @Test
  public void aDefinedTestSystemIsRejected() throws Exception {
    WikiPage page = page("DefinesSlim", """
      ```agentnesse
      define TEST_SYSTEM
      foo
      ```
      """);

    try {
      new WikiPageIdentity(page);
      fail("expected an error");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("DefinesSlim"));
      assertThat(e.getMessage(), containsString("Slim is the only test system"));
    }
  }

  private static WikiPage page(String name, String content) throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT", new Properties());
    return WikiPageUtil.addPage(root, PathParser.parse(name), content);
  }
}
