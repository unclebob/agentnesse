package fitnesse.markdown;

import fitnesse.components.TraversalListener;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.PrunedPagePruningStrategy;
import fitnesse.wiki.SystemVariableSource;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.fs.FileSystemPage;
import fitnesse.wiki.fs.FileSystemPageFactory;
import fitnesse.wiki.fs.MemoryFileSystem;
import fitnesse.wiki.fs.WikiFilePage;
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
    fileSystem.makeFile(new File("root/content.txt"), "");
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
    assertEquals("<h1>Suite</h1>\n", page.getHtml());
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
    fileSystem.makeFile(new File("root/SuiteDemo/OldChild/content.txt"), "old");

    WikiPage suite = root.getPageCrawler().getPage(PathParser.parse("SuiteDemo"));
    WikiPage child = suite.getPageCrawler().getPage(PathParser.parse("TestChild"));

    assertEquals(MarkdownPage.class, child.getClass());
    assertEquals("# child\n", child.getData().getContent());
    assertTrue(child.getData().hasAttribute("Test"));
    assertEquals(FileSystemPage.class, suite.getChildPage("OldChild").getClass());
    assertEquals("old", suite.getChildPage("OldChild").getData().getContent());
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
  public void anOldRootWinsWhenBothFormatsArePresent() throws Exception {
    fileSystem.makeFile(new File("both/content.txt"), "old");
    fileSystem.makeFile(new File("both/_root.md"), "# new\n");
    WikiPage both = new FileSystemPageFactory(fileSystem, new WikiFiles(fileSystem))
      .makePage(new File("both"), "both", null, new SystemVariableSource());

    assertEquals(FileSystemPage.class, both.getClass());
    assertEquals("old", both.getData().getContent());
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
  public void aWikiFileStaysAWikiFile() throws Exception {
    fileSystem.makeFile(new File("root/WikiPage.wiki"), "stuff");

    assertEquals(WikiFilePage.class, root.getChildPage("WikiPage").getClass());
    assertEquals(FileSystemPage.class, root.getClass());
  }
}
