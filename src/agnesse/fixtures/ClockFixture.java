package agnesse.fixtures;

import agnesse.util.Clock;
import agnesse.util.DateAlteringClock;
import agnesse.util.DateTimeUtil;

import java.text.ParseException;
import java.util.TimeZone;

public class ClockFixture {
  public void freezeClockAt(String dateTime) throws ParseException {
    System.out.println("Freezing time at " + dateTime);
    new DateAlteringClock(DateTimeUtil.getDateFromString(dateTime), TimeZone.getDefault()).freeze();
  }

  public String simulationDate() {
    return DateTimeUtil.formatDate(Clock.currentDate());
  }
}
