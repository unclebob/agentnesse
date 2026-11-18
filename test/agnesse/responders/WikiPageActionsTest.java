package agnesse.responders;

import agnesse.wiki.PageData;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageDummy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WikiPageActionsTest {
  @Test
  public void flagsFollowThePageAttributes() {
    WikiPage page = new WikiPageDummy("MySuite", "", new WikiPageDummy());
    PageData data = page.getData();
    data.setAttribute("Test");
    data.setAttribute("Suite");
    data.setAttribute("Edit");
    data.setAttribute("Refactor");
    data.setAttribute("Search");
    data.setAttribute("Files");
    page.commit(data);

    WikiPageActions actions = new WikiPageActions(page);
    assertTrue(actions.isTestPage());
    assertTrue(actions.isSuitePage());
    assertTrue(actions.isWithEdit());
    assertTrue(actions.isWithRefactor());
    assertTrue(actions.isWithSearch());
    assertTrue(actions.isWithFiles());
    assertTrue(actions.isWithVariablesScope());
    assertEquals("MySuite", actions.getLocalPageName());
  }

  @Test
  public void aPageWithoutThoseAttributesHasNoActions() {
    WikiPageActions actions = new WikiPageActions(new WikiPageDummy("Plain", "", new WikiPageDummy()));
    assertFalse(actions.isTestPage());
    assertFalse(actions.isSuitePage());
    assertFalse(actions.isWithEdit());
    assertFalse(actions.isWithRefactor());
    assertFalse(actions.isWithSearch());
    assertFalse(actions.isWithFiles());
    assertTrue(actions.isWithVariablesScope());
    assertEquals("Plain", actions.getLocalPageName());
  }

  @Test
  public void aMissingPageHasNoActionsAndNoName() {
    WikiPageActions actions = new WikiPageActions(null);
    assertFalse(actions.isTestPage());
    assertFalse(actions.isSuitePage());
    assertFalse(actions.isWithEdit());
    assertFalse(actions.isWithRefactor());
    assertFalse(actions.isWithSearch());
    assertFalse(actions.isWithFiles());
    assertFalse(actions.isWithVariablesScope());
    assertNull(actions.getLocalPageName());
  }
}
