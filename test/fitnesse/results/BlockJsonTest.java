package fitnesse.results;

import fitnesse.testsystems.slim.MarkdownTable;
import fitnesse.testsystems.slim.results.SlimExceptionResult;
import fitnesse.testsystems.slim.results.SlimTestResult;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BlockJsonTest {

  @Test
  public void aTableKeepsOneBasedCellsAndTheCheckpointSixStatuses() {
    MarkdownTable table = MarkdownTable.parse("| script | echo |\n| --- | --- |\n| check | ok |\n| note | skip |\n");
    table.locate("Suite.Page", "script");
    table.updateContent(1, 0, (SlimTestResult) SlimTestResult.pass("ok", "ok"));
    table.updateContent(1, 1, SlimTestResult.pass("$total<-[3]"));
    table.updateContent(0, 2, SlimTestResult.ignore());
    table.updateContent(1, 2, new SlimExceptionResult("id",
      "message:<<java.lang.RuntimeException: boom\n\tat Demo.main(Demo.java:1)>>"));

    JSONObject block = BlockJson.from(table);
    assertEquals("script", block.getString("name"));
    JSONObject pass = cell(block, 0, 1);
    assertEquals(1, pass.getInt("row"));
    assertEquals(2, pass.getInt("column"));
    assertEquals("pass", pass.getString("status"));
    assertEquals("ok", pass.getString("actual"));
    assertEquals("ok", pass.getString("expected"));

    JSONObject symbol = cell(block, 1, 1);
    assertEquals("symbol", symbol.getString("status"));
    assertEquals("total", symbol.getString("symbol"));
    assertEquals("3", symbol.getString("value"));

    assertEquals("ignore", cell(block, 2, 0).getString("status"));

    JSONObject error = cell(block, 2, 1).getJSONObject("error");
    assertEquals("EXCEPTION", error.getString("kind"));
    assertEquals("java.lang.RuntimeException", error.getString("type"));
    assertTrue(error.getString("stackTrace").contains("Demo.main"));
    assertEquals(3, error.getInt("row"));
    assertEquals(2, error.getInt("cell"));
    assertEquals("Suite.Page", error.getString("page"));
  }

  @Test
  public void anEmptyTableHasNoName() {
    JSONObject block = BlockJson.from(MarkdownTable.parse(""));
    assertEquals("", block.getString("name"));
    assertEquals(0, block.getJSONArray("rows").length());
  }

  private static JSONObject cell(JSONObject block, int row, int column) {
    return block.getJSONArray("rows").getJSONObject(row).getJSONArray("cells").getJSONObject(column);
  }
}
