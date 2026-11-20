package agnesse.slim.test;

/** Reports every item as released, including one that is only blocked. */
public class ItemLifeQueryLeak extends ItemLife {
  @Override
  public String released() {
    return "true";
  }
}
