package agnesse.markdown;

import agnesse.wiki.PathParser;
import agnesse.wiki.SystemVariableSource;
import agnesse.wiki.WikiPage;
import agnesse.wiki.fs.FileSystemPageFactory;
import agnesse.wiki.fs.WikiFiles;
import agnesse.wiki.refactoring.ReferenceIndex;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * The bundled wiki is a build check. A user wiki may still contain create-page links, and the
 * server keeps rendering those as [?]. Words inside a Slim table are not links, so they are
 * not checked here.
 */
public class BundledPageReferencesTest {
  /**
   * Deliberate create-page links. The acceptance tests assert the [?] markup.
   */
  private static final List<String> CREATE_PAGE_LINKS = List.of(
    "FitNesse.SuiteAcceptanceTests.AcceptanceTestPage.ChildWithLinkToNonExistentPage -> .FitNesse.SuiteAcceptanceTests.AcceptanceTestPage.DoesNotExist",
    "FitNesse.SuiteAcceptanceTests.AcceptanceTestPage.ChildWithSubPageLinkToNonExistentPage -> .FitNesse.SuiteAcceptanceTests.AcceptanceTestPage.ChildWithSubPageLinkToNonExistentPage.NonExistentSubPage",
    "FitNesse.SuiteAcceptanceTests.AcceptanceTestPage.ChildWithAliasLinkToNonExistentPage -> NonExistentPage",
    "FitNesse.SuiteAcceptanceTests.AcceptanceTestPage.ChildWithAliasLinkToNonExistentSubPage -> ^NonExistentSubPage"
  );

  @Test
  public void bundledPagesOnlyLinkToPagesThatExist() {
    List<String> dangling = new ArrayList<>(ReferenceIndex.dangling(wiki()));
    dangling.removeAll(CREATE_PAGE_LINKS);
    assertEquals(dangling.toString(), 0, dangling.size());
  }

  @Test
  public void theAcceptanceSuitePageOffersNoCreatePageLinks() {
    WikiPage page = wiki().getPageCrawler().getPage(PathParser.parse(".FitNesse.SuiteAcceptanceTests"));
    assertFalse(page.getHtml(), page.getHtml().contains("[?]"));
  }

  private static WikiPage wiki() {
    return new FileSystemPageFactory(new agnesse.wiki.fs.DiskFileSystem(), new WikiFiles())
      .makePage(new File("FitNesseRoot"), "FitNesseRoot", null, new SystemVariableSource());
  }
}
