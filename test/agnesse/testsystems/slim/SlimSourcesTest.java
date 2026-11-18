package agnesse.testsystems.slim;

import agnesse.testrunner.WikiTestPage;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.InMemoryPage;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SlimSourcesTest {

  @Test
  public void setupIsNotTearDownAndTearDownIs() {
    SlimSources sources = collect(wiki(
      "SetUp", table("set up"),
      "Sample", table("script"),
      "TearDown", table("tear down")
    ).getChildPage("Sample"));

    assertFalse(table(sources, "set up").isTearDown());
    assertEquals("SetUp", table(sources, "set up").origin());
    assertEquals("SetUp", table(sources, "set up").originPath());
    assertFalse(table(sources, "script").isTearDown());
    assertEquals("Sample", table(sources, "script").origin());
    assertTrue(table(sources, "tear down").isTearDown());
    assertEquals("TearDown", table(sources, "tear down").origin());
  }

  @Test
  public void anIncludedTableRecordsThePagePath() {
    WikiPage root = wiki(
      "Parent.SetUp", table("set up"),
      "Parent.Child", table("script")
    );

    MarkdownTable setup = table(collect(root.getPageCrawler().getPage(PathParser.parse("Parent.Child"))), "set up");

    assertEquals("SetUp", setup.origin());
    assertEquals("Parent.SetUp", setup.originPath());
  }

  @Test
  public void aScenarioLibraryIsNotTearDown() {
    SlimSources sources = collect(wiki(
      "ScenarioLibrary", table("scenario"),
      "Sample", table("script")
    ).getChildPage("Sample"));

    assertEquals(List.of("scenario", "script"), heads(sources));
    assertFalse(table(sources, "scenario").isTearDown());
    assertEquals("ScenarioLibrary", table(sources, "scenario").origin());
  }

  @Test
  public void suiteSetUpAndTearDownAreSeparatePages() {
    SlimSources sources = collect(wiki(
      "SuiteSetUp", table("suite setup"),
      "Sample", table("script"),
      "SuiteTearDown", table("suite teardown")
    ).getChildPage("Sample"));

    assertEquals(List.of("script"), heads(sources));
  }

  @Test
  public void anIncludeTakesTheFirstWordOfTheFirstLine() {
    WikiPage root = wiki(
      "Shared", table("shared"),
      "Sample", agnesse("include Shared extra\nignored") + table("script")
    );

    assertEquals(List.of("shared", "script"), heads(collect(root.getChildPage("Sample"))));
  }

  @Test
  public void aScenariosBlockLoadsTheNamedPage() {
    WikiPage root = wiki(
      "Greetings", table("scenario"),
      "Sample", agnesse("scenarios Greetings") + table("script")
    );

    assertEquals(List.of("scenario", "script"), heads(collect(root.getChildPage("Sample"))));
  }

  @Test
  public void anIncludeInsideTearDownStaysTearDown() {
    SlimSources sources = collect(wiki(
      "Helper", table("helper"),
      "Sample", table("script"),
      "TearDown", agnesse("include Helper")
    ).getChildPage("Sample"));

    assertTrue(table(sources, "helper").isTearDown());
  }

  @Test
  public void aMissingOrInvalidIncludeLeavesTheHostTables() {
    WikiPage root = wiki("Sample",
      agnesse("include MissingPage") + agnesse("include ???") + table("script"));

    assertEquals(List.of("script"), heads(collect(root.getChildPage("Sample"))));
  }

  @Test
  public void aSelfIncludeDoesNotDuplicateThePage() {
    WikiPage root = wiki("Sample", agnesse("include Sample") + table("script"));

    assertEquals(List.of("script"), heads(collect(root.getChildPage("Sample"))));
  }

  @Test
  public void aCyclicIncludeStopsAndKeepsBothPages() {
    WikiPage root = wiki(
      "Ping", agnesse("include Pong") + table("ping"),
      "Pong", agnesse("include Ping") + table("pong"),
      "Sample", agnesse("include Ping") + table("script")
    );

    assertEquals(List.of("pong", "ping", "script"), heads(collect(root.getChildPage("Sample"))));
  }

  @Test
  public void aJavaFenceDoesNotLoadAnInclude() {
    WikiPage root = wiki(
      "Shared", table("shared"),
      "Sample", "```java\ninclude Shared\n```\n" + table("script")
    );

    assertEquals(List.of("script"), heads(collect(root.getChildPage("Sample"))));
  }

  @Test
  public void aBareFenceDoesNotHideTheFollowingTable() {
    WikiPage root = wiki("Sample", "```\ncode\n```\n" + table("script"));

    assertEquals(List.of("script"), heads(collect(root.getChildPage("Sample"))));
  }

  @Test
  public void aDefineFenceIsNotATableDocument() {
    WikiPage root = wiki("Sample", agnesse("define X\n1") + table("script"));

    assertEquals(List.of("script"), heads(collect(root.getChildPage("Sample"))));
  }

  @Test
  public void anUnknownAgnesseCommandIsSkipped() {
    WikiPage root = wiki("Sample", agnesse("nope") + table("script"));

    assertEquals(List.of("script"), heads(collect(root.getChildPage("Sample"))));
  }

  @Test
  public void twoAliasFencesAreBothRegistered() {
    WikiPage root = wiki("Sample",
      agnesse("alias first\nFirstTarget") + agnesse("alias second\nSecondTarget") + table("script"));
    SlimSources sources = collect(root.getChildPage("Sample"));

    assertEquals("FirstTarget", alias(sources, "first"));
    assertEquals("SecondTarget", alias(sources, "second"));
  }

  @Test
  public void anAliasKeepsTheRestOfAMultilineTarget() {
    WikiPage root = wiki("Sample", agnesse("alias MyAlias\nline1\nline2") + table("script"));

    assertEquals("line1\nline2", alias(collect(root.getChildPage("Sample")), "MyAlias"));
  }

  @Test
  public void anAliasNeedsANameAndATarget() {
    WikiPage root = wiki("Sample",
      agnesse("alias onlyName")
        + agnesse("alias\n\nthe target")
        + agnesse("alias MyAlias\n   ")
        + table("script"));
    SlimSources sources = collect(root.getChildPage("Sample"));

    assertTrue(sources.items().stream().noneMatch(item -> item.alias() != null));
    assertEquals(List.of("script"), heads(sources));
  }

  @Test
  public void anUnclosedFenceIsAnErrorAndAFinishedPageIsNot() {
    assertEquals(List.of(), collect(wiki("Sample", table("script")).getChildPage("Sample")).errors());

    List<String> errors = collect(wiki("Sample", "```\ncode\n").getChildPage("Sample")).errors();
    assertEquals(1, errors.size());
    assertTrue(errors.get(0), errors.get(0).contains("Unclosed code fence"));
    assertTrue(errors.get(0), errors.get(0).contains("Sample"));
  }

  @Test
  public void aTableAfterAFenceIsANewDocument() {
    WikiPage root = wiki("Sample", table("first") + "```java\ncode\n```\n" + table("second"));

    assertEquals(List.of("first", "second"), heads(collect(root.getChildPage("Sample"))));
  }

  private static WikiPage wiki(String... nameAndBody) {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    for (int i = 0; i < nameAndBody.length; i += 2) {
      WikiPageUtil.addPage(root, PathParser.parse(nameAndBody[i]), nameAndBody[i + 1]);
    }
    return root;
  }

  private static SlimSources collect(WikiPage page) {
    return SlimSources.collect(new WikiTestPage(page));
  }

  private static String table(String head) {
    return "| " + head + " |\n| --- |\n";
  }

  private static String agnesse(String body) {
    return "```agnesse\n" + body + "\n```\n";
  }

  private static List<String> heads(SlimSources sources) {
    return sources.tables().stream().map(table -> table.getCellContents(0, 0)).toList();
  }

  private static MarkdownTable table(SlimSources sources, String head) {
    return sources.tables().stream()
      .filter(table -> head.equals(table.getCellContents(0, 0)))
      .findFirst()
      .orElseThrow();
  }

  private static String alias(SlimSources sources, String name) {
    return sources.items().stream()
      .filter(item -> name.equals(item.alias()))
      .map(SlimSources.Item::target)
      .findFirst()
      .orElseThrow();
  }
}
