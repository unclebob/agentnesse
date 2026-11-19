package agnesse.slim.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class BoundTest {
  @Test
  public void belowIsTrueUnderFiftyAndTheCapThrowsAboveEighty() {
    Bound bound = new Bound();
    assertEquals(0, bound.n());
    assertTrue(bound.below());
    assertTrue(bound.belowCap());
    bound.setN(50);
    assertFalse(bound.below());
    assertFalse(bound.belowCap());
    bound.setN(100);
    assertThrows(IllegalStateException.class, bound::belowCap);
    bound.setN(10);
    assertTrue(bound.haltAtSixty());
    bound.setN(60);
    assertThrows(Bound.StopTestAtSixty.class, bound::haltAtSixty);
    bound.setN(50);
    assertFalse(bound.haltAtSixty());
  }
}
