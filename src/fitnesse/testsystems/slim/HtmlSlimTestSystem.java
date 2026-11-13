// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.testsystems.slim;

import java.util.List;

import fitnesse.html.HtmlUtil;
import fitnesse.wiki.PageData;
import fitnesse.testsystems.TestExecutionException;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.slim.results.SlimTestResult;
import fitnesse.testsystems.slim.tables.SlimTable;
import fitnesse.testsystems.slim.tables.SlimTableFactory;
import fitnesse.testsystems.slim.tables.SyntaxError;

public class HtmlSlimTestSystem extends SlimTestSystem {

  private static final SlimTable START_OF_TEST = null;
  private static final SlimTable END_OF_TEST = null;

  private final SlimTableFactory slimTableFactory;
  private SlimPage slimPage;

  public HtmlSlimTestSystem(String testSystemName, SlimClient slimClient,
                            SlimTableFactory slimTableFactory) {
    super(testSystemName, slimClient);
    this.slimTableFactory = slimTableFactory;
  }

  @Override
  protected void processAllTablesOnPage(TestPage pageToTest) throws TestExecutionException {
    slimPage = SlimPage.Make(pageToTest, getTestContext(), slimTableFactory);
    List<SlimTable> allTables = slimPage.getTables();
    boolean isSuiteTearDownPage = PageData.SUITE_TEARDOWN_NAME.equals(pageToTest.getName());
    for (String error : slimPage.pageErrors()) {
      getTestContext().incrementErroredTestsCount();
      testOutputChunk(pageToTest, "<span class=\"error\">" + HtmlUtil.escapeHTML(error) + "</span>\n");
    }

    if (allTables.isEmpty()) {
      String html = createHtmlResults(START_OF_TEST, END_OF_TEST);
      testOutputChunk(pageToTest, html);
    } else {
      for (int index = 0; index < allTables.size(); index++) {
        SlimTable theTable = allTables.get(index);
        SlimTable startWithTable = (index == 0) ? START_OF_TEST : theTable;
        SlimTable nextTable = (index + 1 < allTables.size()) ? allTables.get(index + 1) : END_OF_TEST;

        try {
          processTable(theTable, isSuiteTearDownPage);
        } catch (SyntaxError e) {
          String tableName = theTable.getTable().getCellContents(0, 0);
          theTable.getTable().updateContent(0, 0, SlimTestResult.error(String.format("<strong> %s: Bad table! %s</strong>", tableName, e.getMessage())));
          getTestContext().incrementErroredTestsCount();
        }

        String html = createHtmlResults(startWithTable, nextTable);
        testOutputChunk(pageToTest, html);
      }
    }
  }

  private String createHtmlResults(SlimTable startWithTable, SlimTable stopBeforeTable) {
    MarkdownTableScanner scanner = slimPage.getTableScanner();
    if (startWithTable == null && stopBeforeTable == null) {
      return scanner.toHtml();
    }
    StringBuilder html = new StringBuilder();
    boolean started = startWithTable == null;
    for (MarkdownTable table : scanner) {
      if (!started && startWithTable != null && table == startWithTable.getTable()) {
        started = true;
      }
      if (!started) {
        continue;
      }
      if (stopBeforeTable != null && table == stopBeforeTable.getTable()) {
        break;
      }
      html.append(table.toHtml());
    }
    return html.toString();
  }
}
