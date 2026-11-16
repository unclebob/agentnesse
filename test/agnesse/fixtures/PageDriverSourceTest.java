package agnesse.fixtures;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PageDriverSourceTest {
  @Test
  public void bundledSourceReadsTheShippedWikiFile() throws Exception {
    PageDriver driver = new PageDriver();

    assertTrue(driver.bundledSourceContains("FrontPage", "I like this"));
    assertTrue(driver.bundledSourceContains("AgNesse.UserGuide", "include SuiteAcceptanceTests.SuiteUserGuide.FrontMatter"));
    assertFalse(driver.bundledSourceContains("FrontPage", "no such sentence"));
  }
}
