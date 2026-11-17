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

  @Test
  public void bundledHtmlRendersIncludedPages() throws Exception {
    PageDriver driver = new PageDriver();

    assertTrue(driver.bundledHtmlContains("AgNesse.UserGuide", "starts with a JSON object"));
    assertFalse(driver.bundledSourceContains("AgNesse.UserGuide", "starts with a JSON object"));
    assertTrue(driver.bundledHtmlContains("AgNesse.UserGuide", "<summary>SuiteAcceptanceTests.SuiteUserGuide.FrontMatter</summary>"));
    assertFalse(driver.bundledHtmlContains("AgNesse.UserGuide", "include SuiteAcceptanceTests.SuiteUserGuide.FrontMatter"));
    assertFalse(driver.bundledHtmlContains("AgNesse.NoSuchGuidePage", "starts with a JSON object"));
  }
}
