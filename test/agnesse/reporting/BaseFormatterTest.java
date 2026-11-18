package agnesse.reporting;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BaseFormatterTest {
  @Test
  public void theDeprecatedOutputChunkAndErrorCountStayQuiet() {
    BaseFormatter formatter = new BaseFormatter() {
    };
    formatter.testOutputChunk("ignored");
    assertEquals(0, formatter.getErrorCount());
    assertNull(formatter.getPage());
  }
}
