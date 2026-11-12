package fitnesse.markdown;

import fitnesse.components.TraversalListener;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPageLoadException;
import fitnesse.wiki.PrunedPagePruningStrategy;
import fitnesse.wiki.SystemVariableSource;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.fs.FileSystemPageFactory;
import fitnesse.wiki.fs.MemoryFileSystem;
import fitnesse.wiki.fs.WikiFiles;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MarkdownPageTest {
  private MemoryFileSystem fileSystem;
  private WikiPage root;

  @Before
  public void setUp() throws Exception {
    fileSystem = new MemoryFileSystem();
    fileSystem.makeFile(new File("root/_root.md"), "");
    root = new FileSystemPageFactory(fileSystem, new WikiFiles(fileSystem))
      .makePage(new File("root"), "root", null, new SystemVariableSource());
  }

  @Test
  public void aMarkdownFileIsAPageWithItsFrontMatter() throws Exception {
    fileSystem.makeFile(new File("root/SuiteDemo.md"), """
      {
        "type": "suite",
        "tags": ["smoke", "slow"],
        "help": "the suite",
        "prune": true
      }
      # Suite
      """);

    WikiPage page = root.getChildPage("SuiteDemo");

    assertEquals(MarkdownPage.class, page.getClass());
    PageData data = page.getData();
    assertEquals("# Suite\n", data.getContent());
    assertTrue(data.hasAttribute("Suite"));
    assertFalse(data.hasAttribute("Test"));
    assertEquals("smoke, slow", data.getAttribute("Suites"));
    assertEquals("the suite", data.getAttribute("Help"));
    assertTrue(data.hasAttribute("Prune"));
    assertEquals("SuiteDemo", page.getName());
    assertEquals("<h1 id=\"suite\">Suite</h1>\n", page.getHtml());
    fileSystem.makeFile(new File("root/FrontPage.md"), "there\n");
    fileSystem.makeFile(new File("root/Notes.md"), "See FrontPage.\n");
    assertTrue(root.getChildPage("Notes").getHtml().contains("<a href=\"FrontPage\">FrontPage</a>"));
    assertNull(page.getVariable("anything"));
    assertTrue(page.getVersions().isEmpty());
    try {
      page.getVersion("none");
      fail("versions are not kept");
    } catch (RuntimeException expected) {
      assertTrue(expected.getMessage().contains("not kept"));
    }
  }

  @Test
  public void aMissingFrontMatterInfersTheTypeFromTheName() throws Exception {
    fileSystem.makeFile(new File("root/TestDivision.md"), "# go\n");
    fileSystem.makeFile(new File("root/SuiteDemo.md"), "body\n");
    fileSystem.makeFile(new File("root/Notes.md"), "body\n");

    assertTrue(root.getChildPage("TestDivision").getData().hasAttribute("Test"));
    assertTrue(root.getChildPage("SuiteDemo").getData().hasAttribute("Suite"));
    PageData notes = root.getChildPage("Notes").getData();
    assertFalse(notes.hasAttribute("Test"));
    assertFalse(notes.hasAttribute("Suite"));
  }

  @Test
  public void anExplicitTypeOverridesTheName() throws Exception {
    fileSystem.makeFile(new File("root/TestDivision.md"), "{ \"type\": \"static\" }\nbody\n");

    PageData data = root.getChildPage("TestDivision").getData();
    assertFalse(data.hasAttribute("Test"));
    assertFalse(data.hasAttribute("Suite"));
    assertEquals("body\n", data.getContent());
  }

  @Test
  public void aVariableWithANewlineStaysInsideTheSlimCell() throws Exception {
    fileSystem.makeFile(new File("root/TestHost.md"), """
      ```agentnesse
      define SUT
      !path lib/*.jar
      ```
      | script | with content |
      | --- | --- |
      | given page | ${SUT} |
      """);
    String html = root.getChildPage("TestHost").getHtml();
    var scanner = new fitnesse.testsystems.slim.HtmlTableScanner(html);
    assertEquals("!path lib/*.jar", scanner.getTable(0).getCellContents(1, 1).trim());
    assertEquals(1, scanner.getTableCount());
  }

  @Test
  public void commitWritesTheFrontMatterAndTheBody() throws Exception {
    fileSystem.makeFile(new File("root/Notes.md"), "body\n");
    WikiPage page = root.getChildPage("Notes");
    PageData data = page.getData();
    data.setContent("changed\n");
    data.setAttribute("Test");
    data.setAttribute("Suites", "smoke, slow");
    data.setAttribute("Help", "helps");
    data.setAttribute("Prune");
    page.commit(data);

    FrontMatter matter = FrontMatter.read(fileSystem.getContent(new File("root/Notes.md")));
    assertEquals("test", matter.type());
    assertEquals(List.of("smoke", "slow"), matter.tags());
    assertEquals("helps", matter.help());
    assertTrue(matter.prune());
    assertEquals("changed\n", matter.body());
    assertEquals("changed\n", page.getData().getContent());
    assertTrue(page.getData().hasAttribute("Test"));
  }

  @Test
  public void aNamedChildHasNoFileUntilSomethingWritesIt() throws Exception {
    fileSystem.makeFile(new File("root/Notes.md"), "body\n");
    WikiPage page = root.getChildPage("Notes");

    assertTrue(page.getChildren().isEmpty());
    WikiPage child = page.addChildPage("Child");
    assertEquals("Child", child.getName());
    assertEquals("", child.getData().getContent());
    PageData childData = child.getData();
    childData.setContent("kid\n");
    child.commit(childData);
    assertEquals("kid\n", FrontMatter.read(fileSystem.getContent(new File("root/Notes/Child.md"))).body());
    page.removeChildPage("Child");
    page.remove();
    assertFalse(fileSystem.exists(new File("root/Notes.md")));
  }

  @Test
  public void childrenLiveBesideTheMarkdownFileAndADottedPathFindsThem() throws Exception {
    fileSystem.makeFile(new File("root/SuiteDemo.md"), "{ \"type\": \"suite\" }\n# parent\n");
    fileSystem.makeFile(new File("root/SuiteDemo/TestChild.md"), "{ \"type\": \"test\" }\n# child\n");
    fileSystem.makeFile(new File("root/SuiteDemo/OldChild/" + ("content" + ".txt")), "old");

    WikiPage suite = root.getPageCrawler().getPage(PathParser.parse("SuiteDemo"));
    WikiPage child = suite.getPageCrawler().getPage(PathParser.parse("TestChild"));

    assertEquals(MarkdownPage.class, child.getClass());
    assertEquals("# child\n", child.getData().getContent());
    assertTrue(child.getData().hasAttribute("Test"));
    WikiPage oldChild = suite.getChildPage("OldChild");
    assertEquals(MarkdownPage.class, oldChild.getClass());
    assertEquals("", oldChild.getData().getContent());
  }

  @Test
  public void contentsListsLinksToTheChildPages() throws Exception {
    fileSystem.makeFile(new File("root/SuiteDemo.md"), """
      { "type": "suite" }
      ```agentnesse
      contents
      ```
      """);
    fileSystem.makeFile(new File("root/SuiteDemo/Blocks.md"), "# b\n");
    fileSystem.makeFile(new File("root/SuiteDemo/SetUp.md"), "# s\n");

    String html = root.getChildPage("SuiteDemo").getHtml();

    assertTrue(html, html.contains("class=\"agentnesse-contents\""));
    assertTrue(html, html.contains("<a href=\"SuiteDemo.Blocks\">Blocks</a>"));
    assertTrue(html, html.contains("<a href=\"SuiteDemo.SetUp\">SetUp</a>"));
  }

  @Test
  public void theRootPageIsStoredAsRootMarkdown() throws Exception {
    fileSystem.makeFile(new File("wiki/_root.md"), "{ \"help\": \"home\" }\n# Home\n");
    WikiPage markdownRoot = new FileSystemPageFactory(fileSystem, new WikiFiles(fileSystem))
      .makePage(new File("wiki"), "wiki", null, new SystemVariableSource());

    assertEquals(MarkdownPage.class, markdownRoot.getClass());
    assertEquals("home", markdownRoot.getData().getAttribute("Help"));
    assertEquals("# Home\n", markdownRoot.getData().getContent());
    assertTrue(markdownRoot.getChildren().isEmpty());
  }

  @Test
  public void markdownBesideAnOldPageNamesBothFiles() throws Exception {
    fileSystem.makeFile(new File("root/Notes.md"), "new\n");
    fileSystem.makeFile(new File("root/Notes/" + ("content" + ".txt")), "old\n");
    fileSystem.makeFile(new File("root/Other.md"), "new\n");
    fileSystem.makeFile(new File("root/Other" + ("." + "wiki")), "old\n");

    assertBothFormats("Notes", "Notes.md", "content" + ".txt");
    assertBothFormats("Other", "Other.md", "Other" + ("." + "wiki"));
  }

  @Test
  public void aRootWithBothContentAndRootMarkdownNamesBothFiles() throws Exception {
    fileSystem.makeFile(new File("both/" + ("content" + ".txt")), "old");
    fileSystem.makeFile(new File("both/_root.md"), "# new\n");
    try {
      new FileSystemPageFactory(fileSystem, new WikiFiles(fileSystem))
        .makePage(new File("both"), "both", null, new SystemVariableSource());
      fail("both formats");
    } catch (WikiPageLoadException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("content" + ".txt"));
      assertTrue(e.getMessage(), e.getMessage().contains("_root.md"));
    }
  }

  private void assertBothFormats(String pageName, String markdown, String old) {
    try {
      root.getChildPage(pageName);
      fail(pageName);
    } catch (WikiPageLoadException e) {
      assertTrue(e.getMessage(), e.getMessage().contains(markdown));
      assertTrue(e.getMessage(), e.getMessage().contains(old));
    }
  }

  @Test
  public void aPrunedMarkdownPageHidesItsChildren() throws Exception {
    fileSystem.makeFile(new File("root/Pruned.md"), "{ \"prune\": true }\nsecret\n");
    fileSystem.makeFile(new File("root/Pruned/Hidden.md"), "nope\n");
    fileSystem.makeFile(new File("root/Kept.md"), "yes\n");
    List<String> names = new ArrayList<>();

    root.getPageCrawler().traverse(new TraversalListener<WikiPage>() {
      @Override
      public void process(WikiPage page) {
        if (!page.isRoot()) {
          names.add(page.getName());
        }
      }
    }, new PrunedPagePruningStrategy());

    assertFalse(names.contains("Pruned"));
    assertFalse(names.contains("Hidden"));
    assertTrue(names.contains("Kept"));
  }

  @Test
  public void aDefineIsAVariableHereAndOnAChild() throws Exception {
    fileSystem.makeFile(new File("root/SuiteDemo.md"), """
      { "type": "suite" }
      ```agentnesse
      define MAX
      3
      ```
      limit ${MAX}
      """);
    fileSystem.makeFile(new File("root/SuiteDemo/TestChild.md"), "child ${MAX} ${PAGE_NAME}\n");

    WikiPage parent = root.getChildPage("SuiteDemo");
    WikiPage child = parent.getChildPage("TestChild");

    assertEquals("3", parent.getVariable("MAX"));
    assertTrue(parent.getHtml().contains("limit 3"));
    assertEquals("3", child.getVariable("MAX"));
    assertEquals("TestChild", child.getVariable("PAGE_NAME"));
    String html = child.getHtml();
    assertTrue(html, html.contains("child 3"));
    assertTrue(html, html.contains(">TestChild</a>"));
  }

  @Test
  public void aWikiFileIsNotAPage() throws Exception {
    fileSystem.makeFile(new File("root/WikiPage" + ("." + "wiki")), "stuff");

    assertNull(root.getChildPage("WikiPage"));
    assertEquals(MarkdownPage.class, root.getClass());
  }
}
