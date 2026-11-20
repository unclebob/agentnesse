package agnesse.testsystems.slim.sequence;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RaceTest {
  @Test
  public void oneBookedAndOneFullPass() {
    Race.Result result = Race.check(seats(), List.of(pair(once(0), once(0))),
        bookedThenFull(), "Seat", new String[0], Waves.DIRECT);
    assertTrue(result.passed);
    assertEquals(1, result.trials);
    assertEquals(0, result.trial);
    assertFalse(result.broken);
  }

  @Test
  public void twoBookedSeatsFailAndNameWhatWasSeen() {
    Schedule schedule = pair(once(0), once(0));
    Race.Result result = Race.check(seats(), List.of(schedule), always("booked"), "Seat", new String[0], Waves.DIRECT);
    assertFalse(result.passed);
    assertEquals(1, result.trial);
    assertTrue(result.why, result.why.contains("no serial order"));
    assertTrue(result.why, result.why.contains("t1 booked"));
    assertTrue(result.why, result.why.contains("t2 booked"));
    assertFalse(result.shrunk);
    assertFalse(result.capped);
    assertEquals(schedule, result.drawn);
  }

  @Test
  public void aLongerBranchShrinksToTheTwoRacingBooks() {
    Schedule schedule = pair(twice(), twice());
    Race.Result result = Race.check(seats(), List.of(schedule), always("booked"), "Seat", new String[0], Waves.DIRECT);
    assertFalse(result.passed);
    assertTrue(result.shrunk);
    assertEquals(1, result.schedule.branches.get(0).steps.size());
    assertEquals(1, result.schedule.branches.get(1).steps.size());
    assertEquals(0, result.schedule.branches.get(0).steps.get(0).command);
    assertEquals(2, result.drawn.branches.get(0).steps.size());
    assertTrue(result.why, result.why.contains("no serial order"));
  }

  @Test
  public void aShrinkThatPassesIsLeftBehind() {
    Counting desk = new Counting();
    Schedule schedule = pair(twice(), twice());
    Race.Result result = Race.check(seats(), List.of(schedule), desk, "Seat", new String[0], Waves.DIRECT);
    assertFalse(result.shrunk);
    assertEquals(schedule, result.schedule);
    assertEquals(7, desk.makes);
  }

  @Test
  public void theSecondTrialIsTheOneThatFails() {
    Race.Result result = Race.check(seats(), List.of(pair(once(0), once(0)), pair(once(0), once(0))),
        new FailAfter(1), "Seat", new String[0], Waves.DIRECT);
    assertEquals(2, result.trial);
    assertEquals(2, result.trials);
    assertTrue(result.why, result.why.contains("no serial order"));
  }

  @Test
  public void aBrokenCallIsTheReason() {
    Race.Result result = Race.check(seats(), List.of(pair(once(0), once(0))),
        broken(), "Seat", new String[0], Waves.DIRECT);
    assertTrue(result.broken);
    assertEquals("boom", result.why);
  }

  @Test
  public void aMissingReturnIsNoResult() {
    Race.Result result = Race.check(seats(), List.of(pair(once(0), once(0))),
        always(null), "Seat", new String[0], Waves.DIRECT);
    assertTrue(result.broken);
    assertEquals("no result", result.why);
  }

  @Test
  public void theSearchBoundIsNamed() {
    Schedule schedule = pair(once(0), once(0));
    History history = History.ok(List.of(), List.of(
        List.of(Reading.returned("booked")), List.of(Reading.returned("booked"))));
    String why = Race.explain(seats(), schedule, history, 0);
    assertTrue(why, why.contains("linearizability search hit its bound of 0"));
    assertEquals("no result", Race.explain(seats(), schedule, History.broke("  "), Linear.BOUND));
    assertEquals("down", Race.explain(seats(), schedule, History.broke("down"), Linear.BOUND));
    assertEquals(null, Race.explain(seats(), pair(once(0), once(1)),
        History.ok(List.of(), List.of(List.of(Reading.returned("booked")), List.of(Reading.returned("full")))),
        Linear.BOUND));
  }

  @Test
  public void queriesAndInvariantsAreAskedAfterTheCommand() {
    List<String> methods = new ArrayList<>();
    Desk desk = new Desk() {
      private int books;
      public void make(String instance, String fixture, String[] arguments) {
      }
      public String call(int lane, String instance, String method, java.util.List<String> arguments) {
        methods.add(method);
        if ("book".equals(method)) {
          books++;
          return books == 1 ? "booked" : "full";
        }
        return "seats".equals(method) ? "0" : "1";
      }
    };
    Race.Result result = Race.check(queried(), List.of(pair(once(0), once(1))), desk, "Seat", new String[0], Waves.DIRECT);
    assertTrue(result.passed);
    assertTrue(methods.contains("seats"));
    assertTrue(methods.contains("left"));
    assertEquals(List.of("book", "seats", "left"), methods.subList(0, 3));
  }

  @Test
  public void theDrawnTextNamesThePrefixAndEachThread() {
    assertEquals("prefix [book], t1 [book], t2 [book]",
        Race.drew(seats(), new Schedule(once(0), List.of(once(0), once(0)))));
    assertEquals("prefix [], t1 [], t2 []",
        Race.drew(seats(), new Schedule(new Trace(List.of()), List.of(new Trace(List.of()), new Trace(List.of())))));
  }

  @Test
  public void directRunsEveryTask() {
    List<String> seen = new ArrayList<>();
    Waves.direct(List.of(() -> seen.add("a"), () -> seen.add("b")));
    assertEquals(List.of("a", "b"), seen);
  }

  private static Plan seats() {
    return Plan.create(state("seats", "1"), 1, 2, List.of(
        new Command("book", "seats=1", "seats=0", List.of(), List.of(result("booked"))),
        new Command("book", "seats=0", "", List.of(), List.of(result("full")))
    ), List.of());
  }

  private static Plan queried() {
    Command open = new Command("book", "seats=1", "seats=0", List.of(), List.of(
        result("booked"), new Command.Expect(false, "seats", "0", 4)));
    Command shut = new Command("book", "seats=0", "", List.of(), List.of(
        result("full"), new Command.Expect(false, "seats", "0", 4)));
    Guard guard = new Guard("", "", "left", "1");
    return Plan.create(state("seats", "1"), 1, 2, List.of(open, shut), List.of(guard));
  }

  private static Command.Expect result(String text) {
    return new Command.Expect(true, null, text, 3);
  }

  private static Trace once(int command) {
    return new Trace(List.of(new Step(command, List.of())));
  }

  private static Trace twice() {
    return new Trace(List.of(new Step(0, List.of()), new Step(1, List.of())));
  }

  private static Schedule pair(Trace left, Trace right) {
    return new Schedule(new Trace(List.of()), List.of(left, right));
  }

  private static Desk always(String value) {
    return new Desk() {
      public void make(String instance, String fixture, String[] arguments) {
      }
      public String call(int lane, String instance, String method, java.util.List<String> arguments) {
        return value;
      }
    };
  }

  private static Desk bookedThenFull() {
    return new Desk() {
      private int calls;
      public void make(String instance, String fixture, String[] arguments) {
        calls = 0;
      }
      public String call(int lane, String instance, String method, java.util.List<String> arguments) {
        calls++;
        return calls == 1 ? "booked" : "full";
      }
    };
  }

  private static Desk broken() {
    return new Desk() {
      public void make(String instance, String fixture, String[] arguments) {
        throw new CallBroke("boom");
      }
      public String call(int lane, String instance, String method, java.util.List<String> arguments) {
        return "booked";
      }
    };
  }

  private static final class Counting implements Desk {
    private int makes;
    private int calls;

    public void make(String instance, String fixture, String[] arguments) {
      makes++;
      calls = 0;
    }

    public String call(int lane, String instance, String method, java.util.List<String> arguments) {
      calls++;
      if (makes == 1) {
        return "booked";
      }
      return calls == 1 ? "booked" : "full";
    }
  }

  private static final class FailAfter implements Desk {
    private final int pass;
    private int makes;
    private int calls;

    private FailAfter(int pass) {
      this.pass = pass;
    }

    public void make(String instance, String fixture, String[] arguments) {
      makes++;
      calls = 0;
    }

    public String call(int lane, String instance, String method, java.util.List<String> arguments) {
      calls++;
      if (makes > pass) {
        return "booked";
      }
      return calls == 1 ? "booked" : "full";
    }
  }

  private static Map<String, String> state(String... pairs) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put(pairs[i], pairs[i + 1]);
    }
    return map;
  }
}
