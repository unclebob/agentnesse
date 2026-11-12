package fitnesse.markdown;

import fitnesse.wiki.PathParser;
import fitnesse.wiki.SystemVariableSource;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import fitnesse.wiki.fs.FileSystemPageFactory;
import fitnesse.wiki.fs.WikiFiles;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SamplePagesTest {
  @Test
  public void theSamplePagesRenderThroughTheWiki() throws Exception {
    WikiPage root = new FileSystemPageFactory(new fitnesse.wiki.fs.DiskFileSystem(), new WikiFiles())
      .makePage(new File("FitNesseRoot"), "FitNesseRoot", null, new SystemVariableSource());

    WikiPage decision = root.getPageCrawler().getPage(PathParser.parse("AgentNesseSamples.DecisionDivision"));
    WikiPage script = root.getPageCrawler().getPage(PathParser.parse("AgentNesseSamples.ScriptLogin"));
    WikiPage withSetUp = root.getPageCrawler().getPage(PathParser.parse("AgentNesseSamples.WithSetUp"));
    WikiPage blocks = root.getPageCrawler().getPage(PathParser.parse("AgentNesseSamples.Blocks"));

    assertEquals(MarkdownPage.class, decision.getClass());
    assertTrue(decision.getHtml().contains("agentnesse-table decision"));
    assertTrue(decision.getHtml().contains("id=\"divides-numbers\""));
    assertTrue(script.getHtml().contains("agentnesse-table scenario"));
    assertTrue(script.getHtml().contains("agentnesse-table script"));
    assertTrue(withSetUp.getHtml().contains("agentnesse-setup"));
    assertTrue(withSetUp.getHtml().contains("agentnesse-include"));
    assertTrue(withSetUp.getHtml().contains("agentnesse-table query"));
    assertTrue(blocks.getHtml().contains("agentnesse-define"));
    assertTrue(blocks.getHtml().contains("agentnesse-alias"));
    assertTrue(blocks.getHtml().contains("agentnesse-contents"));
    assertEquals("PageHeader", WikiPageUtil.getHeaderPage(decision).getName());
    assertEquals("PageFooter", WikiPageUtil.getFooterPage(decision).getName());
    assertEquals("SetUp", decision.getParent().getChildPage("SetUp").getName());
  }
}
