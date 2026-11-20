package agnesse.slim.test;

/** A reserve that holds the slot, waitlists it, or returns the given answer. */
public class Waitlist {
  private final String answer;
  private String phase = "open";

  public Waitlist() {
    this("wait");
  }

  public Waitlist(String answer) {
    this.answer = answer;
  }

  public String reserve() {
    if ("held".equals(answer)) {
      phase = "held";
      return "held";
    }
    if ("wait".equals(answer)) {
      phase = "wait";
      return "wait";
    }
    return answer;
  }

  public String phase() {
    return phase;
  }

  public String ping() {
    return "ok";
  }
}
