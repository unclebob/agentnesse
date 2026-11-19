package agnesse.slim.test;

import java.math.BigDecimal;

/** A price compared with one dollar, used to show decimal shrinking. */
public class Amount {
  private BigDecimal price = BigDecimal.ZERO;

  public void setPrice(String price) {
    this.price = new BigDecimal(price);
  }

  public boolean affordable() {
    return price.compareTo(BigDecimal.ONE) < 0;
  }
}
