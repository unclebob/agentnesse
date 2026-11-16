package agnesse.results;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SchemaFreezeTest {
  @Test
  public void resultsSchemaIsFrozenAtVersionOne() throws Exception {
    try (var in = RunDocument.class.getResourceAsStream("results.schema.json")) {
      JSONObject schema = new JSONObject(new JSONTokener(in));
      assertEquals(1, schema.getJSONObject("properties").getJSONObject("schemaVersion").getInt("const"));
      assertEquals("https://agentnesse.dev/schemas/results-1.json", schema.getString("$id"));
      assertTrue(schema.getString("description").contains("965aad43b"));
      assertTrue(schema.getString("description").contains("Frozen"));
    }
  }
}
