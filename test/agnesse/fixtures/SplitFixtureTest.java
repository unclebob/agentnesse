package agnesse.fixtures;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SplitFixtureTest {
  @Test
  public void querySplitsLinesOnSemicolonsAndWordsOnCommas() {
    List<Object> table = new SplitFixture("a,b;c").query();

    assertEquals(2, table.size());
    assertEquals(Arrays.asList(Arrays.asList("1", "a"), Arrays.asList("2", "b")), table.get(0));
    assertEquals(Arrays.asList(Arrays.asList("1", "c")), table.get(1));
  }
}
