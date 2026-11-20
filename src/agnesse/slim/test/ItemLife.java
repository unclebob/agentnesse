package agnesse.slim.test;

/** An item that can be opened, blocked, and released. A blocked item stays blocked. */
public class ItemLife {
  protected String phase = "new";

  public String open() {
    return move("new", "open");
  }

  public String block() {
    return move("open", "blocked");
  }

  public String release() {
    if ("blocked".equals(phase)) {
      return "refused";
    }
    return move("open", "released");
  }

  public String ping() {
    return "ok";
  }

  public String phase() {
    return phase;
  }

  public String released() {
    return "released".equals(phase) ? "true" : "false";
  }

  protected void forceReleased() {
    phase = "released";
  }

  private String move(String need, String next) {
    if (!need.equals(phase)) {
      return "refused";
    }
    phase = next;
    return "ok";
  }
}
