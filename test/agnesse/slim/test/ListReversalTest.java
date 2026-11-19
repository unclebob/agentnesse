package agnesse.slim.test;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ListReversalTest {
  @Test
  public void reversingTwiceReturnsTheSameList() {
    ListReversal reversal = new ListReversal();
    assertTrue(reversal.reversedTwice());
    assertTrue(reversal.stillEmpty());
    reversal.setItems(null);
    assertTrue(reversal.stillEmpty());
    reversal.setItems(List.of(1, 2, 3));
    assertTrue(reversal.reversedTwice());
    assertFalse(reversal.stillEmpty());
    reversal.setItems(List.of());
    assertTrue(reversal.stillEmpty());
  }
}
