package agnesse.markdown;

import agnesse.wiki.PathParser;
import agnesse.wiki.SystemVariableSource;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.FileSystemPageFactory;
import agnesse.wiki.fs.WikiFiles;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SamplePagesTest {
  @Test
  public void theSamplePagesRenderThroughTheWiki() throws Exception {
    WikiPage root = new FileSystemPageFactory(new agnesse.wiki.fs.DiskFileSystem(), new WikiFiles())
      .makePage(new File("AgNesseRoot"), "AgNesseRoot", null, new SystemVariableSource());

    WikiPage samples = root.getPageCrawler().getPage(PathParser.parse("AgNesseSamples"));
    String contents = samples.getHtml();
    for (String child : new String[] {
      "Blocks", "DecisionDivision", "PageFooter", "PageHeader", "ScriptLogin", "SetUp", "WithSetUp"}) {
      assertTrue(contents, contents.contains("<a href=\"AgNesseSamples." + child + "\">" + child + "</a>"));
    }
    assertTrue(!contents.contains("agnesse-include"));

    WikiPage decision = root.getPageCrawler().getPage(PathParser.parse("AgNesseSamples.DecisionDivision"));
    WikiPage script = root.getPageCrawler().getPage(PathParser.parse("AgNesseSamples.ScriptLogin"));
    WikiPage withSetUp = root.getPageCrawler().getPage(PathParser.parse("AgNesseSamples.WithSetUp"));
    WikiPage blocks = root.getPageCrawler().getPage(PathParser.parse("AgNesseSamples.Blocks"));

    assertEquals(MarkdownPage.class, decision.getClass());
    assertTrue(decision.getHtml().contains("agnesse-table decision"));
    assertTrue(decision.getHtml().contains("id=\"divides-numbers\""));
    assertTrue(script.getHtml().contains("agnesse-table scenario"));
    assertTrue(script.getHtml().contains("agnesse-table script"));
    assertTrue(!withSetUp.getHtml().contains("agnesse-include"));
    assertTrue(!withSetUp.getHtml().contains("agnesse-setup"));
    assertTrue(withSetUp.getHtml().contains("agnesse-table query"));
    assertTrue(blocks.getHtml().contains("agnesse-define"));
    assertTrue(blocks.getHtml().contains("agnesse-alias"));
    assertTrue(blocks.getHtml().contains("agnesse-contents"));
    assertEquals("PageHeader", WikiPageUtil.getHeaderPage(decision).getName());
    assertEquals("PageFooter", WikiPageUtil.getFooterPage(decision).getName());
    assertEquals("SetUp", decision.getParent().getChildPage("SetUp").getName());
  }
}
