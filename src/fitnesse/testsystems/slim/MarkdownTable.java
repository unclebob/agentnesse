package fitnesse.testsystems.slim;

import fitnesse.html.HtmlUtil;
import fitnesse.markdown.SlimMarkdownTable;
import fitnesse.markdown.TableSeparators;
import fitnesse.testsystems.ExecutionResult;
import fitnesse.testsystems.TestResult;
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
  private final List<Table> children = new ArrayList<>();
  private boolean tearDown;

  public static MarkdownTable parse(String markdown) {
    SlimMarkdownTable source = SlimMarkdownTable.parse(TableSeparators.supply(markdown));
    MarkdownTable table = new MarkdownTable();
    for (int row = 1; row <= source.rowCount(); row++) {
      table.rows.add(cells(withoutTrailingEmpty(source.row(row))));
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
    return rows.size() - 1;
  }

  @Override
  public void addColumnToRow(int row, String content) {
    rows.get(row).add(new Cell(content));
  }

  @Override
  public void appendChildTable(int row, Table table) {
    children.add(table);
  }

  @Override
  public void updateContent(int row, TestResult testResult) {
    for (Cell cell : rows.get(row)) {
      cell.rowResult = testResult.getExecutionResult() == null ? null : testResult.getExecutionResult().toString();
    }
  }

  @Override
  public void updateContent(int column, int row, SlimTestResult testResult) {
    Cell cell = rows.get(row).get(column);
    cell.result = testResult;
  }

  @Override
  public void updateContent(int column, int row, SlimExceptionResult exceptionResult) {
    if (column < 0) {
      rows.add(row + 1, List.of(new Cell(exceptionResult.getException())));
      return;
    }
    rows.get(row).get(column).exception = exceptionResult;
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
    for (List<Cell> row : rows) {
      html.append("<tr>");
      for (Cell cell : row) {
        html.append("<td>").append(cell.html()).append("</td>");
      }
      html.append("</tr>\n");
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
        String message = exception.getMessage() != null ? exception.getMessage() : exception.getException();
        String rendered = HtmlUtil.isValidTableCellContent(message) ? message : escape(message);
        String cls = exception.getExecutionResult() == null ? "error" : exception.getExecutionResult().toString();
        return escape(content) + " <span class=\"" + cls + "\">" + rendered + "</span>";
      }
      if (result != null && result.getExecutionResult() != null) {
        return resultHtml(result);
      }
      if (rowResult != null) {
        return "<span class=\"" + rowResult + "\">" + escape(content) + "</span>";
      }
      return HtmlUtil.isValidTableCellContent(content) ? content : escape(content);
    }

    private String resultHtml(SlimTestResult result) {
      ExecutionResult status = result.getExecutionResult();
      if (status == ExecutionResult.FAIL && result.hasExpected()) {
        String actual = result.hasActual() ? result.getActual() : "";
        return "[" + escape(actual) + "] <span class=\"fail\">expected [" + escape(result.getExpected()) + "]</span>";
      }
      String body = result.hasExpected() ? result.getExpected() : content;
      if (result.hasMessage() && status != ExecutionResult.PASS) {
        body = result.getMessage();
      }
      return "<span class=\"" + status + "\">" + (HtmlUtil.isValidTableCellContent(body) ? body : escape(body)) + "</span>";
    }

    private static String escape(String text) {
      return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
  }
}
