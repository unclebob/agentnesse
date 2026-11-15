// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.testsystems.slim;

import java.util.List;

import fitnesse.html.HtmlUtil;
import fitnesse.results.BlockJson;
import fitnesse.wiki.PageData;
import org.json.JSONArray;
import org.json.JSONObject;
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

  SlimPage executedPage() {
    return slimPage;
  }

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
          if (theTable.getTable() instanceof MarkdownTable markdown) {
            publishTree(pageToTest, markdown);
          }
        } catch (SyntaxError e) {
          String tableName = theTable.getTable().getCellContents(0, 0);
          theTable.getTable().updateContent(0, 0, SlimTestResult.error(tableName + ": Bad table! " + e.getMessage()));
          getTestContext().incrementErroredTestsCount();
          if (theTable.getTable() instanceof MarkdownTable markdown) {
            publishTree(pageToTest, markdown);
          }
        }

        String html = createHtmlResults(startWithTable, nextTable);
        testOutputChunk(pageToTest, html);
      }
    }
  }

  private void publishTree(TestPage page, MarkdownTable table) {
    publishTable(page, table);
    for (MarkdownTable child : table.childTables()) {
      publishTree(page, child);
    }
  }

  private String createHtmlResults(SlimTable startWithTable, SlimTable stopBeforeTable) {
    return htmlFrom(startWithTable, stopBeforeTable);
  }

  private String htmlFrom(SlimTable startWithTable, SlimTable stopBeforeTable) {
    StringBuilder html = new StringBuilder();
    SectionBuffer section = new SectionBuffer();
    boolean started = startWithTable == null;
    String testPage = pageName();
    for (MarkdownTable table : slimPage.getTableScanner()) {
      if (!started) {
        started = table == startWithTable.getTable();
      }
      if (stopBeforeTable != null && table == stopBeforeTable.getTable()) {
        break;
      }
      if (started) {
        section.add(html, testPage, table);
      }
    }
    section.flush(html);
    return html.toString();
  }

  private String pageName() {
    return getTestContext().getPageToTest() == null ? "" : getTestContext().getPageToTest().getName();
  }

  private static final class SectionBuffer {
    private final StringBuilder html = new StringBuilder();
    private final JSONArray blocks = new JSONArray();
    private String name = "";

    void add(StringBuilder out, String testPage, MarkdownTable table) {
      String next = ResultSections.section(table.origin(), testPage, table.getCellContents(0, 0));
      if (!next.equals(name)) {
        flush(out);
        name = next;
      }
      JSONObject block = BlockJson.from(table);
      String rendered = ResultHtml.block(block);
      if (next.isEmpty()) {
        out.append(rendered);
      } else {
        html.append(rendered);
        blocks.put(block);
      }
    }

    void flush(StringBuilder out) {
      if (!name.isEmpty() && html.length() > 0) {
        out.append(ResultSections.details(name, html.toString(), ResultSections.shouldOpen(blocks)));
      }
      html.setLength(0);
      while (blocks.length() > 0) {
        blocks.remove(0);
      }
    }
  }
}
