package fitnesse.slim.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DivisionTest {
  @Test
  public void dividesNumeratorByDenominator() {
    Division division = new Division();
    division.setNumerator(10);
    division.setDenominator(2);
    assertEquals(5.0, division.quotient(), 0.0);
  }
}
