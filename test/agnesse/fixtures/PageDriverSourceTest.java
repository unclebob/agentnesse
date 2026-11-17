package agnesse.fixtures;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PageDriverSourceTest {
  @Test
  public void bundledSourceReadsTheShippedWikiFile() throws Exception {
    PageDriver driver = new PageDriver();

    assertTrue(driver.bundledSourceContains("PageHeader", "page header"));
    assertTrue(driver.bundledSourceContains("AgNesse.UserGuide", "include SuiteAcceptanceTests.SuiteUserGuide.FrontMatter"));
    assertFalse(driver.bundledSourceContains("PageHeader", "no such sentence"));
  }
}
