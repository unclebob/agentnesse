package agnesse.slim.test;

/** A blocked item that is released anyway. */
public class ItemLifeLeak extends ItemLife {
  @Override
  public String release() {
    forceReleased();
    return "ok";
  }
}
