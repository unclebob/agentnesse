package agnesse.testsystems.slim.tables;

import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.MarkdownTableScanner;
import agnesse.testsystems.slim.SlimTestContextImpl;
import agnesse.testsystems.slim.Table;
import agnesse.testrunner.WikiTestPage;
import agnesse.wiki.WikiPageDummy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CommentTableRunTest {
  @Test
  public void aCommentTableIsSkippedAndNotCounted() throws Exception {
    MarkdownTableScanner scanner = new MarkdownTableScanner("""
      | Comment |
      | --- |
      | SuiteWidgetTests | this is not a test |

      | script | Echo |
      | --- |
      | check | echo | hi | hi |
      """);
    SlimTableFactory factory = new SlimTableFactory();
    SlimTestContextImpl context = new SlimTestContextImpl(new WikiTestPage(new WikiPageDummy()));
    int ran = 0;
    SlimTable script = null;
    for (Table table : scanner) {
      SlimTable slim = factory.makeSlimTable(table, String.valueOf(ran), context);
      if (slim != null) {
        ran++;
        script = slim;
      }
    }
    assertEquals(1, ran);
    assertEquals(ScriptTable.class, script.getClass());
    int counted = script.getAssertions().size();
    int scriptAlone = assertions("""
      | script | Echo |
      | --- |
      | check | echo | hi | hi |
      """);
    assertEquals(scriptAlone, counted);
    MarkdownTable comment = scanner.getTable(0);
    assertEquals("Comment", comment.getCellContents(0, 0));
    assertNull(factory.makeSlimTable(comment, "x", context));
  }

  private static int assertions(String markdown) throws Exception {
    SlimTableFactory factory = new SlimTableFactory();
    SlimTestContextImpl context = new SlimTestContextImpl(new WikiTestPage(new WikiPageDummy()));
    int total = 0;
    for (Table table : new MarkdownTableScanner(markdown)) {
      SlimTable slim = factory.makeSlimTable(table, "0", context);
      if (slim != null) {
        total += slim.getAssertions().size();
      }
    }
    return total;
  }
}
