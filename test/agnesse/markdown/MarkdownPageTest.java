package agnesse.markdown;

import agnesse.components.TraversalListener;
import agnesse.wiki.PageData;
import agnesse.wiki.search.FrontMatterFinder;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPageLoadException;
import agnesse.wiki.PrunedPagePruningStrategy;
import agnesse.wiki.SystemVariableSource;
import agnesse.wiki.WikiPage;
import agnesse.wiki.fs.FileSystem;
import agnesse.wiki.fs.FileSystemPageFactory;
import agnesse.wiki.fs.MemoryFileSystem;
import agnesse.wiki.fs.WikiFiles;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
      ```agnesse
      define SUT
      !path lib/*.jar
      ```
      | script | with content |
      | --- | --- |
      | given page | ${SUT} |
      """);
    String html = root.getChildPage("TestHost").getHtml();
    assertEquals("!path lib/*.jar", agnesse.testsystems.slim.HtmlCells.cell(html, 0, 1, 1).trim());
    assertEquals(1, agnesse.testsystems.slim.HtmlCells.tables(html));
  }

  @Test
  public void aTagOnAMarkdownFileIsFound() {
    fileSystem.makeFile(new File("root/Kept.md"), FrontMatter.write("test", List.of("ready"), null, false, "body\n"));
    fileSystem.makeFile(new File("root/Other.md"), FrontMatter.write("test", List.of("later"), null, false, "body\n"));
    List<String> found = new ArrayList<>();
    new FrontMatterFinder(page -> found.add(page.getName()), "test", "ready", null, null).search(root);

    assertEquals(List.of("Kept"), found);
  }

  @Test
  public void anUntouchedFrontMatterStaysByteForByte() throws Exception {
    String original = "{ \"type\" : \"test\", \"tags\" : [ \"keep\" ] }\nOLD";
    fileSystem.makeFile(new File("root/Notes.md"), original);
    WikiPage page = root.getChildPage("Notes");
    PageData data = page.getData();
    data.setContent("NEW");
    data.setPreserveFrontMatter(true);
    page.commit(data);

    assertEquals("{ \"type\" : \"test\", \"tags\" : [ \"keep\" ] }\nNEW", fileSystem.getContent(new File("root/Notes.md")));
  }

  @Test
  public void preservingFrontMatterWithNoContentWritesOnlyThePrefix() throws Exception {
    fileSystem.makeFile(new File("root/Notes.md"), "{ \"type\": \"test\" }\nbody\n");
    WikiPage page = root.getChildPage("Notes");
    PageData data = page.getData();
    data.setContent(null);
    data.setPreserveFrontMatter(true);
    page.commit(data);

    assertEquals("{ \"type\": \"test\" }\n", fileSystem.getContent(new File("root/Notes.md")));
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
    page.removeChildPage("NoSuch");
    page.removeChildPage("Child");
    assertFalse(fileSystem.exists(new File("root/Notes/Child.md")));
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
    put("SuiteDemo.md", """
      { "type": "suite" }
      ```agnesse
      contents
      ```
      """);
    put("SuiteDemo/Blocks.md", "# b\n");
    put("SuiteDemo/SetUp.md", "# s\n");

    String html = html("SuiteDemo");

    assertTrue(html, html.contains("class=\"agnesse-contents\""));
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
  public void markdownBesideAnOldContentFileNamesBothFiles() throws Exception {
    fileSystem.makeFile(new File("root/Notes.md"), "new\n");
    fileSystem.makeFile(new File("root/Notes/" + ("content" + ".txt")), "old\n");

    assertBothFormats("Notes", "Notes.md", "content" + ".txt");
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
      ```agnesse
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

  @Test
  public void aFailedRemoveNamesThePage() throws Exception {
    WikiPage page = wiki(filesThatCannot("delete")).getChildPage("Notes");
    try {
      page.remove();
      fail("remove");
    } catch (WikiPageLoadException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Could not remove"));
      assertTrue(e.getMessage(), e.getMessage().contains("Notes"));
    }
  }

  @Test
  public void aFailedWriteNamesTheFile() throws Exception {
    WikiPage page = wiki(filesThatCannot("write")).getChildPage("Notes");
    try {
      page.commit(page.getData());
      fail("commit");
    } catch (WikiPageLoadException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Could not write"));
      assertTrue(e.getMessage(), e.getMessage().contains("Notes.md"));
    }
  }

  @Test
  public void aFailedReadNamesTheFile() throws Exception {
    WikiPage page = wiki(filesThatCannot("read")).getChildPage("Notes");
    try {
      page.getData();
      fail("read");
    } catch (WikiPageLoadException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Could not read"));
      assertTrue(e.getMessage(), e.getMessage().contains("Notes.md"));
    }
  }

  @Test
  public void slimSeesTheHostPageNameAndEscapesPipesInValues() {
    put("Host.md", """
      ```agnesse
      define CELL
      a|b
      ```
      ${PAGE_NAME} ${CELL}
      """);
    put("Guest.md", "guest\n");
    MarkdownPage host = (MarkdownPage) page("Host");

    assertTrue(host.expandedForSlim(null).contains("Host a\\|b"));
    assertTrue(host.expandedForSlim(page("Guest")).contains("Guest a\\|b"));
    assertTrue(host.getHtmlWithin(page("Guest")).contains("Guest"));
  }

  @Test
  public void htmlKeepsPipesInsideAVariable() {
    String html = html("Notes", """
      ```agnesse
      define CELL
      a|b
      ```
      value ${CELL}
      """);
    assertTrue(html, html.contains("a|b"));
    assertFalse(html, html.contains("a\\|b"));
  }

  @Test
  public void anIncludeInlinesAnotherPageAndIgnoresAMissingOrSelfTarget() {
    put("Child.md", "child-body\n");
    String html = html("Notes", """
      ```agnesse
      include Child
      ```
      """);
    assertTrue(html, html.contains("child-body"));
    assertTrue(html, html.contains("Child"));

    String missing = html("Missing", """
      ```agnesse
      include NoSuchPage
      ```
      visible
      """);
    assertTrue(missing, missing.contains("visible"));
    assertTrue(missing, missing.contains("NoSuchPage"));

    String self = html("Loop", """
      ```agnesse
      include Loop
      ```
      visible
      """);
    assertTrue(self, self.contains("visible"));
    assertEquals(1, self.split("agnesse-include", -1).length - 1);
  }

  @Test
  public void anIncludeNameStopsAtTheFirstLine() {
    put("Child.md", "child-body\n");
    assertTrue(html("Notes", """
      ```agnesse
      include Child
      NotAPage
      ```
      """).contains("child-body"));
    assertFalse(html("Blank", """
      ```agnesse
      include

      Child
      ```
      """).contains("child-body"));
  }

  @Test
  public void aWikiPrefixOutsideAFenceBecomesALink() {
    put("FrontPage.md", "there\n");
    assertTrue(html("Notes", "<FrontPage\n").contains("href=\"FrontPage\""));
  }

  @Test
  public void aWikiPrefixInsideAFenceOrTableStaysLiteral() {
    put("FrontPage.md", "there\n");
    String fenced = html("Fenced", """
      ```
      <FrontPage
      ```
      """);
    assertTrue(fenced, fenced.contains("<pre>"));
    assertTrue(fenced, fenced.contains("&lt;FrontPage"));
    assertFalse(fenced, fenced.contains("\\&lt;FrontPage"));

    String table = html("Tabular", """
      | x | <FrontPage |
      | --- | --- |
      """);
    assertTrue(table, table.contains("<table"));
    assertFalse(table, table.contains("\\&lt;FrontPage") || table.contains("\\<FrontPage"));
  }

  @Test
  public void aBangDefineUnwrapsBracesAndParens() {
    WikiPage notes = notes("""
      !define BRACED {hello}
      !define PARENS (world)
      !define PLAIN keep
      !define OPEN {hello
      !define OPENP (hello
      !define EMPTY
      !define BRACEPAIR {}
      """);
    assertEquals("hello", notes.getVariable("BRACED"));
    assertEquals("world", notes.getVariable("PARENS"));
    assertEquals("keep", notes.getVariable("PLAIN"));
    assertEquals("{hello", notes.getVariable("OPEN"));
    assertEquals("(hello", notes.getVariable("OPENP"));
    assertEquals("", notes.getVariable("EMPTY"));
    assertEquals("", notes.getVariable("BRACEPAIR"));
  }

  @Test
  public void anIncludeBringsDefinesFromTheTarget() {
    put("Helper.md", """
      ```agnesse
      define FROM_BLOCK
      block
      ```
      !define FROM_BANG {bang}
      """);
    WikiPage notes = notes("""
      ```agnesse
      include Helper extra
      ```
      !include Helper
      """);
    assertEquals("block", notes.getVariable("FROM_BLOCK"));
    assertEquals("bang", notes.getVariable("FROM_BANG"));
  }

  @Test
  public void aDefineWithNoValueIsEmpty() {
    assertEquals("", notes("""
      ```agnesse
      define ONLY_NAME
      ```
      """).getVariable("ONLY_NAME"));
  }

  private void put(String relative, String content) {
    fileSystem.makeFile(new File("root/" + relative), content);
  }

  private WikiPage page(String name) {
    return root.getChildPage(name);
  }

  private WikiPage notes(String content) {
    put("Notes.md", content);
    return page("Notes");
  }

  private String html(String name) {
    return page(name).getHtml();
  }

  private String html(String name, String content) {
    put(name + ".md", content);
    return html(name);
  }

  private WikiPage wiki(FileSystem files) throws IOException {
    files.makeFile(new File("root/_root.md"), "");
    files.makeFile(new File("root/Notes.md"), "body\n");
    return new FileSystemPageFactory(files, new WikiFiles(files))
      .makePage(new File("root"), "root", null, new SystemVariableSource());
  }

  private static FileSystem filesThatCannot(String op) {
    return new FileSystem() {
      private final MemoryFileSystem inner = new MemoryFileSystem();

      @Override
      public void makeFile(File file, String content) {
        inner.makeFile(file, content);
      }

      @Override
      public void makeFile(File file, InputStream content) throws IOException {
        if ("write".equals(op)) {
          throw new IOException("unwritable");
        }
        inner.makeFile(file, content);
      }

      @Override
      public void makeDirectory(File path) {
        inner.makeDirectory(path);
      }

      @Override
      public boolean exists(File file) {
        return inner.exists(file);
      }

      @Override
      public String[] list(File path) {
        return inner.list(path);
      }

      @Override
      public String getContent(File file) throws IOException {
        if ("read".equals(op)) {
          throw new IOException("unreadable");
        }
        return inner.getContent(file);
      }

      @Override
      public InputStream getInputStream(File file) throws IOException {
        return inner.getInputStream(file);
      }

      @Override
      public void delete(File path) throws IOException {
        if ("delete".equals(op)) {
          throw new IOException("undeletable");
        }
        inner.delete(path);
      }

      @Override
      public long lastModified(File file) {
        return inner.lastModified(file);
      }

      @Override
      public void rename(File file, File originalFile) {
        inner.rename(file, originalFile);
      }

      @Override
      public boolean isDirectory(File file) {
        return inner.isDirectory(file);
      }
    };
  }
}
