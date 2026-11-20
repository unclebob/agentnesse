package agnesse.slim.test;

/** The last seat, booked under the lock so two callers cannot both take it. */
public class LastSeatSafe {
  private int seats = 1;

  public synchronized String book() {
    if (seats <= 0) {
      return "full";
    }
    seats = seats - 1;
    return "booked";
  }
}
