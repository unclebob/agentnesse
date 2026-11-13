package fitnesse.testsystems.slim;

import fitnesse.testrunner.WikiTestPage;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.slim.tables.SlimTable;
import fitnesse.testsystems.slim.tables.SlimTableFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SlimPage {

  public static SlimPage Make(TestPage testPage, SlimTestContext testContext, SlimTableFactory slimTableFactory) {
    SlimPage page = new SlimPage(slimTableFactory);
    page.createSlimTables(testPage, testContext);
    return page;
  }

  public List<SlimTable> getTables() { return tables; }
  public MarkdownTableScanner getTableScanner() { return tableScanner; }
  public List<String> pageErrors() { return pageErrors; }

  private SlimPage(SlimTableFactory slimTableFactory) {
    this.slimTableFactory = slimTableFactory;
  }

  private void createSlimTables(TestPage testPage, SlimTestContext testContext) {
    if (testPage instanceof WikiTestPage wiki) {
      SlimSources sources = SlimSources.collect(wiki);
      pageErrors = sources.errors();
      tableScanner = new MarkdownTableScanner(sources.tables());
      tables = createFromSources(sources, testContext);
    } else {
      pageErrors = Collections.emptyList();
      tableScanner = new MarkdownTableScanner(testPage.getContent());
      tables = createSlimTables(tableScanner, testContext);
    }
  }

  private List<SlimTable> createFromSources(SlimSources sources, SlimTestContext testContext) {
    List<SlimTable> allTables = new LinkedList<>();
    for (SlimSources.Item item : sources.items()) {
      if (item.alias != null) {
        slimTableFactory.addAlias(item.alias, item.target);
      } else if (item.table != null) {
        createSlimTable(allTables, item.table, testContext);
      }
    }
    return allTables;
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
      if (table instanceof MarkdownTable markdown && testContext.getPageToTest() != null) {
        markdown.locate(testContext.getPageToTest().getName(), markdown.getCellContents(0, 0));
      }
      allTables.add(slimTable);
    }
  }

  private final SlimTableFactory slimTableFactory;

  private MarkdownTableScanner tableScanner;
  private List<SlimTable> tables;
  private List<String> pageErrors = Collections.emptyList();
}
