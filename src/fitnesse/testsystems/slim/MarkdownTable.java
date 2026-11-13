package fitnesse.testsystems.slim;

import fitnesse.html.HtmlUtil;
import fitnesse.markdown.SlimMarkdownTable;
import fitnesse.markdown.TableSeparators;
import fitnesse.testsystems.TestResult;
import fitnesse.testsystems.slim.results.CellRecord;
import fitnesse.testsystems.slim.results.SlimExceptionResult;
import fitnesse.testsystems.slim.results.SlimTestResult;
import fitnesse.testsystems.slim.tables.SyntaxError;

import java.util.ArrayList;
import java.util.List;

/**
 * A Slim {@link Table} whose cells are a GFM Markdown table. FitNesse row 1 is
 * the header ({@code type:Name} plus constructor arguments). The first body
 * row is the column names. Trailing empty cells are not cells.
 */
public final class MarkdownTable implements Table {
  private final List<List<Cell>> rows = new ArrayList<>();
  private final List<CellRecord> rowRecords = new ArrayList<>();
  private final List<PlacedChild> children = new ArrayList<>();
  private CellRecord tableRecord = CellRecord.status(null);
  private String pageName = "";
  private String blockName = "";
  private boolean tearDown;

  public static MarkdownTable parse(String markdown) {
    SlimMarkdownTable source = SlimMarkdownTable.parse(TableSeparators.supply(markdown));
    MarkdownTable table = new MarkdownTable();
    for (int row = 1; row <= source.rowCount(); row++) {
      table.rows.add(cells(withoutTrailingEmpty(source.row(row))));
      table.rowRecords.add(CellRecord.status(null));
    }
    return table;
  }

  private MarkdownTable() {
  }

  @Override
  public boolean isTearDown() {
    return tearDown;
  }

  public void setTearDown(boolean tearDown) {
    this.tearDown = tearDown;
  }

  public void locate(String page, String block) {
    this.pageName = page == null ? "" : page;
    this.blockName = block == null ? "" : block;
  }

  public CellRecord tableRecord() {
    return tableRecord;
  }

  public CellRecord rowRecord(int row) {
    return rowRecords.get(row);
  }

  public CellRecord cellRecord(int row, int column) {
    CellRecord recorded = rows.get(row).get(column).record;
    return recorded == null ? CellRecord.status(null) : recorded;
  }

  @Override
  public String getCellContents(int column, int row) {
    return rows.get(row).get(column).shown();
  }

  @Override
  public int getRowCount() {
    return rows.size();
  }

  @Override
  public int getColumnCountInRow(int row) {
    return rows.get(row).size();
  }

  @Override
  public void substitute(int column, int row, String content) {
    rows.get(row).get(column).content = content;
  }

  @Override
  public int addRow(List<String> list) {
    rows.add(cells(list));
    rowRecords.add(CellRecord.status(null));
    return rows.size() - 1;
  }

  @Override
  public void addColumnToRow(int row, String content) {
    rows.get(row).add(new Cell(content));
  }

  @Override
  public void appendChildTable(int row, Table table) {
    if (table instanceof MarkdownTable child) {
      children.add(new PlacedChild(row, child));
    }
  }

  @Override
  public void updateContent(int row, TestResult testResult) {
    CellRecord recorded = CellRecord.from(testResult);
    rowRecords.set(row, recorded);
    for (Cell cell : rows.get(row)) {
      cell.rowResult = recorded.status();
    }
    noteTable(recorded);
  }

  @Override
  public void updateContent(int column, int row, SlimTestResult testResult) {
    Cell cell = rows.get(row).get(column);
    cell.result = testResult;
    cell.record = CellRecord.from(testResult, cell.content);
    noteTable(cell.record);
  }

  @Override
  public void updateContent(int column, int row, SlimExceptionResult exceptionResult) {
    if (exceptionResult.failure().page() == null) {
      exceptionResult.failure().at(pageName, blockName, row, column < 0 ? null : column);
    }
    CellRecord recorded = CellRecord.error(exceptionResult);
    if (column < 0) {
      Cell cell = new Cell(exceptionResult.getException());
      cell.record = recorded;
      rows.add(row + 1, List.of(cell));
      rowRecords.add(row + 1, recorded);
      noteTable(recorded);
      return;
    }
    Cell cell = rows.get(row).get(column);
    cell.exception = exceptionResult;
    cell.record = recorded;
    noteTable(recorded);
  }

  private void noteTable(CellRecord recorded) {
    if (recorded.failure() != null || worse(recorded.status(), tableRecord.status())) {
      tableRecord = recorded;
    }
  }

  private static boolean worse(String next, String current) {
    return rank(next) > rank(current);
  }

  private static int rank(String status) {
    if ("error".equals(status)) return 4;
    if ("fail".equals(status)) return 3;
    if ("ignore".equals(status)) return 2;
    if ("pass".equals(status)) return 1;
    return 0;
  }

  @Override
  public Table asTemplate(CellContentSubstitution substitution) throws SyntaxError {
    MarkdownTable copy = new MarkdownTable();
    for (List<Cell> row : rows) {
      List<Cell> copied = new ArrayList<>();
      for (Cell cell : row) {
        copied.add(new Cell(substitution.substitute(cell.content)));
      }
      copy.rows.add(copied);
      copy.rowRecords.add(CellRecord.status(null));
    }
    return copy;
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder("[");
    for (int row = 0; row < rows.size(); row++) {
      if (row > 0) {
        out.append(", ");
      }
      out.append(rows.get(row).stream().map(Cell::shown).toList());
    }
    return out.append(']').toString();
  }

  public String toHtml() {
    StringBuilder html = new StringBuilder("<table class=\"agentnesse-table\">\n");
    for (int row = 0; row < rows.size(); row++) {
      html.append("<tr>");
      for (Cell cell : rows.get(row)) {
        html.append("<td>");
        html.append(cell.html());
        html.append("</td>");
      }
      html.append("</tr>\n");
      for (PlacedChild child : children) {
        if (child.row == row) {
          html.append("<tr class=\"scenario closed\"></tr><tr class=\"scenario-detail closed-detail\"><td colspan=\"")
            .append(rows.get(row).size())
            .append("\">")
            .append(child.table.toHtml())
            .append("</td></tr>\n");
        }
      }
    }
    return html.append("</table>\n").toString();
  }

  private static List<Cell> cells(List<String> values) {
    List<Cell> cells = new ArrayList<>();
    for (String value : values) {
      cells.add(new Cell(value));
    }
    return cells;
  }

  private static List<String> withoutTrailingEmpty(List<String> row) {
    int last = row.size() - 1;
    while (last > 0 && row.get(last).isEmpty()) {
      last--;
    }
    return row.subList(0, last + 1);
  }

  private static final class Cell {
    private String content;
    private SlimTestResult result;
    private SlimExceptionResult exception;
    private CellRecord record;
    private String rowResult;

    private Cell(String content) {
      this.content = content == null ? "" : content;
    }

    private String shown() {
      if (result != null) {
        return result.toString(content);
      }
      if (exception != null && exception.getMessage() != null) {
        return content + " " + exception.getMessage();
      }
      return content;
    }

    private String html() {
      if (exception != null) {
        return exceptionHtml();
      }
      if (result != null) {
        return resultHtml();
      }
      return span(rowResult, content);
    }

    private String exceptionHtml() {
      String message = exception.getMessage() != null ? exception.getMessage() : exception.getException();
      String rendered = HtmlUtil.isValidTableCellContent(message) ? message : escape(message);
      String cls = exception.getExecutionResult() == null ? "error" : exception.getExecutionResult().toString();
      return escape(content) + " <span class=\"" + cls + "\">" + rendered + "</span>";
    }

    private String resultHtml() {
      if (result.getExecutionResult() == null) {
        return result.hasMessage() ? escape(result.getMessage()) : escape(content);
      }
      String rendered = CellMarkup.format(content, result);
      return rendered == null ? escape(content) : rendered;
    }

    private String span(String status, String body) {
      String rendered = HtmlUtil.isValidTableCellContent(body) ? body : escape(body);
      return status == null ? rendered : "<span class=\"" + status + "\">" + rendered + "</span>";
    }

    private static String escape(String text) {
      return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
  }

  private static final class PlacedChild {
    private final int row;
    private final MarkdownTable table;

    private PlacedChild(int row, MarkdownTable table) {
      this.row = row;
      this.table = table;
    }
  }
}
