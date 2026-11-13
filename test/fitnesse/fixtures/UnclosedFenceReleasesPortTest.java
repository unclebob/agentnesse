package fitnesse.fixtures;

import fitnesse.testrunner.WikiPageDescriptor;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.testsystems.TestSystemListener;
import fitnesse.testsystems.slim.HtmlSlimTestSystem;
import fitnesse.testsystems.slim.InProcessSlimClientBuilder;
import fitnesse.testsystems.slim.MarkdownTable;
import fitnesse.testsystems.slim.SlimClient;
import fitnesse.testsystems.slim.SlimSources;
import fitnesse.testsystems.slim.tables.SlimTableFactory;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import fitnesse.wiki.fs.InMemoryPage;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * An unclosed fence must not hide TearDown. TearDown is what releases port 9123,
 * and the next test in the suite has to be able to bind that port again.
 */
public class UnclosedFenceReleasesPortTest {

  @After
  public void releasePort() throws Exception {
    if (FitnesseFixtureContext.context != null && FitnesseFixtureContext.context.fitNesse != null) {
      FitnesseFixtureContext.context.fitNesse.stop();
    }
    FitnesseFixtureContext.context = null;
  }

  @Test
  public void anAliasBlockIsRegisteredInDocumentOrder() {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPageUtil.addPage(root, PathParser.parse("Aliased"), """
      ```agentnesse
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
  public void tearDownIsASeparateDocumentAfterAnUnclosedFence() {
    WikiPage root = wiki();
    SlimSources sources = SlimSources.collect(new WikiTestPage(root.getChildPage("Broken")));

    assertTrue(sources.errors().toString(), sources.errors().stream().anyMatch(error -> error.contains("Unclosed code fence")));
    assertTrue(sources.tables().stream().anyMatch(table ->
      table.isTearDown() && "tear down".equals(table.getCellContents(0, 0))));
  }

  @Test
  public void theNextTestInTheSuiteCanBindPort9123() throws Exception {
    WikiPage root = wiki();
    WikiPage broken = root.getChildPage("Broken");
    WikiPage next = root.getChildPage("Next");
    SlimClient client = new InProcessSlimClientBuilder(
      new WikiPageDescriptor(broken, true, false), getClass().getClassLoader()).build();
    HtmlSlimTestSystem system = new HtmlSlimTestSystem("slim", client, new SlimTableFactory());
    StringBuilder output = new StringBuilder();
    system.addTestSystemListener(new TestSystemListener() {
      @Override
      public void testOutputChunk(fitnesse.testsystems.TestPage testPage, String chunk) {
        output.append(chunk);
      }

      @Override
      public void testExceptionOccurred(fitnesse.testsystems.Assertion assertion, fitnesse.testsystems.ExceptionResult exceptionResult) {
        output.append(exceptionResult.getMessage()).append('\n');
      }
    });
    system.start();
    try {
      system.runTests(new WikiTestPage(broken));
      system.runTests(new WikiTestPage(next));
    } finally {
      system.bye();
    }

    assertTrue(output.toString(), output.toString().contains("Unclosed code fence"));
    assertFalse(output.toString(), output.toString().contains("BindException"));
    assertTrue(sourcesRanTearDown(next));
  }

  private static boolean sourcesRanTearDown(WikiPage next) {
    return SlimSources.collect(new WikiTestPage(next)).tables().stream()
      .anyMatch(MarkdownTable::isTearDown);
  }

  private static WikiPage wiki() {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPageUtil.addPage(root, PathParser.parse("SetUp"), """
      | Import |
      | --- |
      | fitnesse.fixtures |

      | SetUp |
      | --- |
      """);
    WikiPageUtil.addPage(root, PathParser.parse("TearDown"), """
      | tear down |
      | --- |
      """);
    String test = """
      ```agentnesse
      define LEFT_OPEN
      this fence is not closed
      | script | echo fixture |
      | --- | --- |
      | check | echo | hi | hi |
      """;
    WikiPage broken = WikiPageUtil.addPage(root, PathParser.parse("Broken"), test);
    WikiPage next = WikiPageUtil.addPage(root, PathParser.parse("Next"), "The next test.\n");
    markTest(broken);
    markTest(next);
    return root;
  }

  private static void markTest(WikiPage page) {
    PageData data = page.getData();
    data.setAttribute("Test");
    page.commit(data);
  }
}
