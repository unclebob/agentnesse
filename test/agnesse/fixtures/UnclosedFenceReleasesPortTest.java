package agnesse.fixtures;

import agnesse.testrunner.WikiPageDescriptor;
import agnesse.testrunner.WikiTestPage;
import agnesse.testsystems.TestSystemListener;
import agnesse.testsystems.slim.HtmlSlimTestSystem;
import agnesse.testsystems.slim.InProcessSlimClientBuilder;
import agnesse.testsystems.slim.SlimClient;
import agnesse.testsystems.slim.SlimSources;
import agnesse.testsystems.slim.tables.SlimTableFactory;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.InMemoryPage;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * An unclosed fence must not hide TearDown. TearDown is what stops the wiki
 * server SetUp started, so a broken page must not leave that server running.
 */
public class UnclosedFenceReleasesPortTest {

  @After
  public void releasePort() throws Exception {
    if (AgNesseFixtureContext.context != null && AgNesseFixtureContext.context.agNesse != null) {
      AgNesseFixtureContext.context.agNesse.stop();
    }
    AgNesseFixtureContext.context = null;
  }

  @Test
  public void anAliasBlockIsRegisteredInDocumentOrder() {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPageUtil.addPage(root, PathParser.parse("Aliased"), """
      ```agnesse
      alias add change
      add up change
      ```

      | ddt: add change |
      | --- |
      | total? |
      | 1 |
      """);
    SlimSources sources = SlimSources.collect(new WikiTestPage(root.getChildPage("Aliased")));
    assertTrue(sources.items().stream().anyMatch(item -> "add change".equals(item.alias()) && "add up change".equals(item.target())));
    assertTrue(sources.tables().stream().anyMatch(table -> table.getCellContents(0, 0).startsWith("ddt:")));
  }

  @Test
  public void aScenariosBlockLoadsTheNamedPageBeforeTheFollowingTables() {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPageUtil.addPage(root, PathParser.parse("Greetings"), """
      | scenario | greet | name |
      | check | echo | hello @name | hello @name |
      """);
    WikiPageUtil.addPage(root, PathParser.parse("Speaker"), """
      ```agnesse
      scenarios Greetings
      ```

      | script |
      | greet | world |
      """);
    SlimSources sources = SlimSources.collect(new WikiTestPage(root.getChildPage("Speaker")));
    List<String> heads = sources.tables().stream().map(table -> table.getCellContents(0, 0)).toList();

    assertEquals(List.of("scenario", "script"), heads);
  }

  @Test
  public void tearDownIsASeparateDocumentAfterAnUnclosedFence() {
    WikiPage root = wiki();
    SlimSources sources = SlimSources.collect(new WikiTestPage(root.getChildPage("Broken")));

    assertTrue(sources.errors().toString(), sources.errors().stream().anyMatch(error -> error.contains("Unclosed code fence")));
    assertTrue(sources.tables().stream().anyMatch(table ->
      table.isTearDown() && "tear down".equals(table.getCellContents(0, 0))));
  }

  @Test
  public void tearDownStopsTheServerAfterAnUnclosedFence() throws Exception {
    WikiPage root = wiki();
    WikiPage broken = root.getChildPage("Broken");
    SlimClient client = new InProcessSlimClientBuilder(
      new WikiPageDescriptor(broken, true, false), getClass().getClassLoader()).build();
    HtmlSlimTestSystem system = new HtmlSlimTestSystem("slim", client, new SlimTableFactory());
    StringBuilder output = new StringBuilder();
    system.addTestSystemListener(new TestSystemListener() {
      @Override
      public void testOutputChunk(agnesse.testsystems.TestPage testPage, String chunk) {
        output.append(chunk);
      }

      @Override
      public void testExceptionOccurred(agnesse.testsystems.Assertion assertion, agnesse.testsystems.ExceptionResult exceptionResult) {
        output.append(exceptionResult.getMessage()).append('\n');
      }
    });
    system.start();
    try {
      system.runTests(new WikiTestPage(broken));
    } finally {
      system.bye();
    }

    assertTrue(output.toString(), output.toString().contains("Unclosed code fence"));
    assertNull(output.toString(), AgNesseFixtureContext.context);
  }

  private static WikiPage wiki() {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPageUtil.addPage(root, PathParser.parse("SetUp"), """
      | Import |
      | --- |
      | agnesse.fixtures |

      | SetUp |
      | --- |
      """);
    WikiPageUtil.addPage(root, PathParser.parse("TearDown"), """
      | tear down |
      | --- |
      """);
    String test = """
      ```agnesse
      define LEFT_OPEN
      this fence is not closed
      | script | echo fixture |
      | --- | --- |
      | check | echo | hi | hi |
      """;
    WikiPage broken = WikiPageUtil.addPage(root, PathParser.parse("Broken"), test);
    markTest(broken);
    return root;
  }

  private static void markTest(WikiPage page) {
    PageData data = page.getData();
    data.setAttribute("Test");
    page.commit(data);
  }
}
