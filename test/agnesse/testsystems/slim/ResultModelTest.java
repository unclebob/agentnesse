package agnesse.testsystems.slim;

import agnesse.results.BlockJson;
import agnesse.testrunner.WikiPageDescriptor;
import agnesse.testrunner.WikiTestPage;
import agnesse.testsystems.UnableToStartException;
import agnesse.testsystems.slim.results.CellRecord;
import agnesse.testsystems.slim.results.SlimFailure;
import agnesse.testsystems.slim.tables.SlimTableFactory;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.InMemoryPage;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResultModelTest {

  @Test
  public void eachTableTypeRecordsTheCell() throws Exception {
    CellRecord decision = show("DECISION", "| decision:agnesse.slim.test.TestSlim |\n| returnInt? |\n| 7 |\n", 2, 0);
    assertEquals("pass", decision.status());
    assertEquals("7", decision.actual());
    show("DYNAMIC", "| dynamic decision:agnesse.slim.test.AddUpChange |\n| 1c | total cents? |\n| 2 | 2 |\n", 2, 1);
    show("QUERY", "| query:agnesse.slim.test.TestQuery | 1 |\n| n | 2n |\n| 1 | 2 |\n", 2, 1);
    show("SUBSET", "| subset query:agnesse.slim.test.TestQuery | 1 |\n| n | 2n |\n| 1 | 2 |\n", 2, 1);
    show("ORDERED", "| ordered query:agnesse.slim.test.TestQuery | 1 |\n| n | 2n |\n| 1 | 2 |\n", 2, 1);
    show("SCRIPT", "| script | agnesse.slim.test.TestSlim |\n| check | echo string | hi | hi |\n", 1, 3);
    show("SCENARIO", "| scenario | double | value |\n| check | echo string | @value | @value |\n", 0, 1);
    show("TABLE", "| table:agnesse.slim.test.TestSlim |\n| a |\n", 0, 0);
    show("IMPORT", "| import |\n| agnesse.slim.test |\n", 1, 0);
    show("LIBRARY", "| library |\n| agnesse.slim.test.TestSlim |\n", 1, 0);
    show("BASELINE", "| baseline:agnesse.slim.test.Division |\n| numerator | denominator | quotient? |\n| 10 | 2 | 5 |\n", 2, 2);
    CellRecord missing = show("MISSING_FIXTURE", "| script | NoSuchClass |\n", 0, 1);
    assertEquals("MISSING_FIXTURE", missing.failure().kind().name());
    assertEquals(Integer.valueOf(1), missing.failure().row());
    assertEquals(Integer.valueOf(2), missing.failure().cell());
    showAll("MISSING_METHOD", "| script | agnesse.slim.test.TestSlim |\n| check | no such method | x | x |\n");
    showAll("EXCEPTION", "| script | agnesse.slim.test.TestSlim |\n| check | throw normal | true |\n");
  }

  @Test
  public void defineTableTypePageCountsItsTableResult() throws Exception {
    String markdown = """
      | import |
      | agnesse.fixtures |

      | Table Or Decision Fixture |
      | --- |
      | parsing as? |
      | Parsed as Decision fixture |

      ```agentnesse
      alias Table or Decision Fixture
      table:
      ```

      | Table Or Decision Fixture | |
      | --- | --- |
      | parsing as? | Parsed as Table fixture |

      ```agentnesse
      alias Table or Decision Fixture
      script:
      ```

      | Table Or Decision Fixture | | |
      | --- | --- | --- |
      | check | parsing as | Parsed as Decision fixture |
      """;
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("Example"), markdown);
    HtmlSlimTestSystem system = start(page);
    try {
      system.runTests(new WikiTestPage(page));
    } finally {
      system.bye();
    }
    int right = 0;
    for (MarkdownTable table : system.executedPage().getTableScanner()) {
      right += BlockJson.from(table).getJSONObject("counts").getInt("right");
    }
    assertEquals(3, right);
  }

  @Test
  public void aSymbolAssignmentIsItsOwnStatus() throws Exception {
    CellRecord cell = run("| script | agnesse.slim.test.TestSlim |\n| $V= | echo string | Bob |\n", 1, 0);
    assertEquals("symbol", cell.status());
    assertEquals("V", cell.symbol());
    assertEquals("Bob", cell.value());
    System.out.println("SYMBOL " + cell.dump());
  }

  @Test
  public void aCellThatDoesNotRunIsIgnore() throws Exception {
    MarkdownTable table = table("| script | agnesse.slim.test.ConstructorThrows | stop test |\n| check | echo string | hi | hi |\n");
    CellRecord cell = table.cellRecord(1, 3);
    assertEquals("ignore", cell.status());
    assertEquals("ignore", table.rowRecord(1).status());
    assertFalse(cell.assertion());
    System.out.println("IGNORE " + cell.dump());
  }

  @Test
  public void aConstructorThatThrowsIsAnExceptionFromAClassThatExists() throws Exception {
    CellRecord cell = run("| script | agnesse.slim.test.ConstructorThrows | boom |\n", 0, 1);
    assertEquals(SlimFailure.Kind.EXCEPTION, cell.failure().kind());
    assertFalse(cell.failure().type().contains("NO_CLASS"));
    assertFalse(cell.failure().type().contains("COULD_NOT_INVOKE"));
    assertTrue(cell.failure().message(), cell.failure().message().contains("boom"));
    assertTrue(cell.dump(), cell.dump().contains("\"stackTrace\":\""));
    assertFalse(cell.failure().stack().isEmpty());
    System.out.println("CONSTRUCTOR " + cell.dump());
  }

  @Test
  public void aServerThatCannotConnectIsALaunchFailureOffAnyCell() throws Exception {
    SlimClient refused = new SlimClient() {
      public void start() throws IOException {
        throw new java.net.ConnectException("Connection refused");
      }
      public Map<String, Object> invokeAndGetResponse(List<agnesse.slim.instructions.Instruction> statements) {
        return Map.of();
      }
      public void connect() {
      }
      public void bye() {
      }
      public void kill() {
      }
    };
    HtmlSlimTestSystem system = new HtmlSlimTestSystem("slim", refused, new SlimTableFactory());
    try {
      system.start();
    } catch (UnableToStartException expected) {
      SlimFailure failure = system.server().failure();
      assertEquals(SlimFailure.Kind.LAUNCH_FAILURE, failure.kind());
      assertNull(failure.page());
      assertNull(failure.block());
      assertNull(failure.row());
      assertNull(failure.cell());
      System.out.println("LAUNCH " + system.server().dump());
      return;
    }
    throw new AssertionError("the server should have failed to start");
  }

  private static void showAll(String label, String markdown) throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT" + label);
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("Example"), markdown);
    HtmlSlimTestSystem system = start(page);
    try {
      system.runTests(new WikiTestPage(page));
    } finally {
      system.bye();
    }
    MarkdownTable table = system.executedPage().getTableScanner().getTable(0);
    for (int row = 0; row < table.getRowCount(); row++) {
      for (int column = 0; column < table.getColumnCountInRow(row); column++) {
        CellRecord cell = table.cellRecord(row, column);
        if (cell.status() != null || cell.failure() != null) {
          System.out.println(label + " r" + row + "c" + column + " " + cell.dump());
        }
      }
    }
  }

  private static HtmlSlimTestSystem start(WikiPage page) throws Exception {
    SlimClient client = new InProcessSlimClientBuilder(
      new WikiPageDescriptor(page, true, false), ResultModelTest.class.getClassLoader()).build();
    HtmlSlimTestSystem system = new HtmlSlimTestSystem("slim", client, new SlimTableFactory());
    system.start();
    return system;
  }

  private static CellRecord show(String label, String markdown, int row, int column) throws Exception {
    CellRecord cell = run(markdown, row, column);
    System.out.println(label + " " + cell.dump());
    return cell;
  }

  private static MarkdownTable table(String markdown) throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("Example"), markdown);
    HtmlSlimTestSystem system = start(page);
    try {
      system.runTests(new WikiTestPage(page));
    } finally {
      system.bye();
    }
    return system.executedPage().getTableScanner().getTable(0);
  }

  private static CellRecord run(String markdown, int row, int column) throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("Example"), markdown);
    HtmlSlimTestSystem system = start(page);
    try {
      system.runTests(new WikiTestPage(page));
    } finally {
      system.bye();
    }
    return system.executedPage().getTableScanner().getTable(0).cellRecord(row, column);
  }
}
