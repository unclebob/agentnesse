package fitnesse.testsystems.slim;

import fitnesse.slim.SlimError;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.slim.tables.SlimTable;
import fitnesse.testsystems.slim.tables.SlimTableFactory;
import org.htmlparser.Parser;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import java.util.LinkedList;
import java.util.List;

public class SlimPage {

  public static SlimPage Make(TestPage testPage, SlimTestContext testContext, SlimTableFactory slimTableFactory) {
    SlimPage page = new SlimPage(slimTableFactory);
    page.createSlimTables(testPage, testContext);
    return page;
  }

  public List<SlimTable> getTables() { return tables; }
  public HtmlTableScanner getTableScanner() { return tableScanner; }

  private SlimPage(SlimTableFactory slimTableFactory) {
    this.slimTableFactory = slimTableFactory;
  }

  private void createSlimTables(TestPage testPage, SlimTestContext testContext) {
    NodeList nodeList = makeNodeList(testPage);
    tableScanner = new HtmlTableScanner(nodeList);
    tables =  createSlimTables(tableScanner, testContext);
  }

  private NodeList makeNodeList(TestPage pageToTest) {
    String html = pageToTest.getHtml();
    Parser parser = new Parser(new Lexer(new Page(html)));
    try {
      return parser.parse(null);
    } catch (ParserException e) {
      throw new SlimError(e);
    }
  }

  private List<SlimTable> createSlimTables(TableScanner<? extends Table> tableScanner, SlimTestContext testContext) {
    List<SlimTable> allTables = new LinkedList<>();
    for (Table table : tableScanner)
      createSlimTable(allTables, table, testContext);

    return allTables;
  }

  private void createSlimTable(List<SlimTable> allTables, Table table, SlimTestContext testContext) {
    String tableId = "" + allTables.size();
    SlimTable slimTable = slimTableFactory.makeSlimTable(table, tableId, testContext);
    if (slimTable != null) {
      allTables.add(slimTable);
    }
  }

  private final SlimTableFactory slimTableFactory;

  private HtmlTableScanner tableScanner;
  private List<SlimTable> tables;
}
