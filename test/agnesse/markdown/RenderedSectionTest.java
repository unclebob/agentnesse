package agnesse.markdown;

import agnesse.testrunner.TestPageWithSuiteSetUpAndTearDown;
import agnesse.wiki.PathParser;
import agnesse.wiki.SystemVariableSource;
import agnesse.wiki.WikiPage;
import agnesse.wiki.fs.FileSystemPageFactory;
import agnesse.wiki.fs.WikiFiles;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Rendered HTML of the bundled wiki, the same pages a browser shows. */
public class RenderedSectionTest {
  @Test
  public void aSlimTestPageLinksInheritedSections() throws Exception {
    String html = testHtml("AgNesse.SuiteAcceptanceTests.SuiteSlimTests.ChainTest");

    assertTrue(html, html.contains(marker("AgNesse.SuiteAcceptanceTests.SuiteSlimTests.SetUp", "SetUp")));
    assertTrue(html, html.contains(marker("AgNesse.SuiteAcceptanceTests.TearDown", "TearDown")));
    assertTrue(html, html.contains(marker("AgNesse.SuiteAcceptanceTests.ScenarioLibrary", "ScenarioLibrary")));
    assertFalse(html, html.contains("<summary>SetUp</summary>"));
  }

  @Test
  public void anAuthoredDetailsTitleIsTextBesideTheMarker() throws Exception {
    String html = testHtml("AgNesse.SuiteAcceptanceTests.SuiteSlimTests.TestComparators");

    assertTrue(html, html.contains(
      "<details class=\"agnesse-section\" open><summary>"
        + "<span class=\"disclosure\" aria-hidden=\"true\">&gt;</span>Scenarios</summary>"));
    assertFalse(html, html.contains(">Scenarios</a>"));
  }

  @Test
  public void theUserGuideLinksIncludedPagesByTheirFullPath() throws Exception {
    String html = page("AgNesse.UserGuide").getHtml();

    assertTrue(html, html.contains(marker(
      "AgNesse.SuiteAcceptanceTests.SuiteUserGuide.FrontMatter",
      "SuiteAcceptanceTests.SuiteUserGuide.FrontMatter")));
    assertTrue(html, html.contains(marker(
      "AgNesse.SuiteAcceptanceTests.SuiteUserGuide.Include",
      "SuiteAcceptanceTests.SuiteUserGuide.Include")));
    assertFalse(html, html.contains("<summary>SuiteAcceptanceTests.SuiteUserGuide.FrontMatter</summary>"));
  }

  private static String marker(String path, String name) {
    return "<span class=\"disclosure\" aria-hidden=\"true\">&gt;</span><a href=\"" + path + "\">" + name + "</a>";
  }

  private static String testHtml(String path) throws Exception {
    return new TestPageWithSuiteSetUpAndTearDown(page(path)).getHtml();
  }

  private static WikiPage page(String path) throws Exception {
    WikiPage root = new FileSystemPageFactory(new agnesse.wiki.fs.DiskFileSystem(), new WikiFiles())
      .makePage(new File("AgNesseRoot"), "AgNesseRoot", null, new SystemVariableSource());
    return root.getPageCrawler().getPage(PathParser.parse(path));
  }
}
