package agnesse.slim.test;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AmountTest {
  @Test
  public void aPriceUnderOneDollarIsAffordable() {
    Amount amount = new Amount();
    assertTrue(amount.affordable());
    amount.setPrice("0.99");
    assertTrue(amount.affordable());
    amount.setPrice("1.00");
    assertFalse(amount.affordable());
    amount.setPrice("2.43");
    assertFalse(amount.affordable());
  }
}
