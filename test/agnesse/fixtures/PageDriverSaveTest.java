package agnesse.fixtures;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PageDriverSaveTest {

  @Before
  public void setUp() throws Exception {
    new SetUp();
  }

  @After
  public void tearDown() throws Exception {
    new TearDown();
  }

  @Test
  public void savePutsTheNewContentsOnThePage() throws Exception {
    PageDriver driver = new PageDriver();
    driver.createPageWithContent("Saved", "old");

    assertEquals(303, driver.requestPageSaveWithContents("Saved", "new content"));
    assertEquals("new content", driver.pageSource("Saved"));
  }

  @Test
  public void saveRecordsTheUserWhoWroteThePage() throws Exception {
    PageDriver driver = new PageDriver();
    driver.createPageWithContent("Saved", "old");

    assertEquals(303, driver.requestPageSaveWithContentsByUserAndPassword("Saved", "new content", "Aladdin", ""));
    assertEquals("Aladdin", driver.lastModifiedOfPage("Saved"));
    assertEquals("new content", driver.pageSource("Saved"));
  }

  @Test
  public void contentContainsLooksAtTheLastResponse() throws Exception {
    PageDriver driver = new PageDriver();
    driver.createPageWithContent("Shown", "visible text");
    driver.requestPage("Shown");

    assertTrue(driver.contentContains("visible text"));
  }
}
