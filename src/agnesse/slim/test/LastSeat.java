package agnesse.slim.test;

/**
 * The last seat, booked by check-then-act. Two overlapping calls can both
 * observe the seat as free. A call that arrives alone still books it; the
 * wait is only a window for a second caller.
 */
public class LastSeat {
  private int seats = 1;
  private int inside;
  private final Object gate = new Object();

  public String book() {
    int seen = seats;
    if (seen <= 0) {
      return "full";
    }
    meet();
    seats = seen - 1;
    return "booked";
  }

  private void meet() {
    synchronized (gate) {
      inside++;
      if (inside == 1) {
        waitForPeer();
      } else {
        gate.notifyAll();
      }
    }
  }

  private void waitForPeer() {
    try {
      gate.wait(200);
    } catch (InterruptedException caught) {
      Thread.currentThread().interrupt();
    }
  }
}
