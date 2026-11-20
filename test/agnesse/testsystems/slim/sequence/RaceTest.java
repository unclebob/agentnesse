package agnesse.testsystems.slim.sequence;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
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
    assertFalse(result.shrunk);
    assertFalse(result.capped);
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
    assertEquals("no result", Race.explain(seats(), schedule, History.broke(null), Linear.BOUND));
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
    assertFalse(methods.contains(null));
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
  public void aPrefixRunsOnTheFirstLaneBeforeTheBranches() {
    Schedule schedule = new Schedule(once(0), List.of(once(1), once(1)));
    List<Integer> lanes = new ArrayList<>();
    Desk desk = new Desk() {
      public void make(String instance, String fixture, String[] arguments) {
      }
      public String call(int lane, String instance, String method, java.util.List<String> arguments) {
        lanes.add(lane);
        return lanes.size() == 1 ? "booked" : "full";
      }
    };
    Race.Result result = Race.check(seats(), List.of(schedule), desk, "Seat", new String[0], Waves.DIRECT);
    assertTrue(result.passed);
    assertEquals(List.of(0, 0, 1), lanes);
  }

  @Test
  public void directRunsEveryTask() {
    List<String> seen = new ArrayList<>();
    Waves.direct(List.of(() -> seen.add("a"), () -> seen.add("b")));
    assertEquals(List.of("a", "b"), seen);
  }

  @Test
  public void togetherWaitsUntilEveryTaskHasFinished() {
    List<String> seen = Collections.synchronizedList(new ArrayList<>());
    Waves.together(List.of(() -> seen.add("a"), () -> seen.add("b")));
    assertEquals(2, seen.size());
    assertTrue(seen.contains("a"));
    assertTrue(seen.contains("b"));
  }

  @Test
  public void anInterruptDuringTheWaitIsKept() {
    Thread.currentThread().interrupt();
    Waves.together(List.of(() -> { }));
    assertTrue(Thread.interrupted());
  }

  @Test
  public void lanesCarryEveryBranchOnTheOneConnection() {
    Race.Result result = Race.check(queried(), List.of(pair(once(0), once(1))),
        laneDesk(false, false, false), "Seat", new String[0], Waves.DIRECT);
    assertTrue(result.why, result.passed);
  }

  @Test
  public void aMissingLaneStampIsNoResult() {
    Race.Result result = Race.check(seats(), List.of(pair(once(0), once(1))),
        laneDesk(true, false, false), "Seat", new String[0], Waves.DIRECT);
    assertTrue(result.broken);
    assertEquals("no result", result.why);
  }

  @Test
  public void anExceptionFromALaneBreaksTheTrial() {
    Race.Result result = Race.check(seats(), List.of(pair(once(0), once(1))),
        laneDesk(false, true, false), "Seat", new String[0], Waves.DIRECT);
    assertTrue(result.broken);
    assertTrue(result.why, result.why.contains("__EXCEPTION__"));
  }

  @Test
  public void aNullLaneResultIsNotAnException() {
    Race.Result result = Race.check(seats(), List.of(pair(once(0), once(1))),
        laneDesk(false, false, true), "Seat", new String[0], Waves.DIRECT);
    assertFalse(result.broken);
  }

  @Test
  public void aDeskThatDoesNotImplementLanesRefusesThem() {
    Desk desk = new Desk() {
      public void make(String instance, String fixture, String[] arguments) {
      }
      public String call(int lane, String instance, String method, List<String> arguments) {
        return "ok";
      }
    };
    CallBroke broke = assertThrows(CallBroke.class, () -> desk.runLanes(List.of()));
    assertEquals("lanes", broke.getMessage());
  }

  @Test
  public void aCallThatAlreadyFinishedHasToBeOrderedFirst() {
    Reading booked = new Reading("booked", Map.of(), 1L, 2L);
    Reading full = new Reading("full", Map.of(), 3L, 4L);
    assertEquals(null, Race.explain(seats(), pair(once(0), once(1)),
        History.ok(List.of(), List.of(List.of(booked), List.of(full))), Linear.BOUND));
    Reading lateBooked = new Reading("booked", Map.of(), 3L, 4L);
    Reading earlyFull = new Reading("full", Map.of(), 1L, 2L);
    String reversed = Race.explain(seats(), pair(once(0), once(1)),
        History.ok(List.of(), List.of(List.of(lateBooked), List.of(earlyFull))), Linear.BOUND);
    assertTrue(reversed, reversed.contains("no serial order"));
    Reading overlap = new Reading("booked", Map.of(), 1L, 4L);
    Reading during = new Reading("full", Map.of(), 2L, 3L);
    assertEquals(null, Race.explain(seats(), pair(once(0), once(1)),
        History.ok(List.of(), List.of(List.of(overlap), List.of(during))), Linear.BOUND));
    Reading unfinished = new Reading("booked", Map.of(), 5L, 0L);
    Reading after = new Reading("full", Map.of(), 6L, 7L);
    assertEquals(null, Race.explain(seats(), pair(once(0), once(1)),
        History.ok(List.of(), List.of(List.of(unfinished), List.of(after))), Linear.BOUND));
  }

  private static Desk laneDesk(boolean drop, boolean boom, boolean blank) {
    return new Desk() {
      public void make(String instance, String fixture, String[] arguments) {
      }
      public String call(int lane, String instance, String method, List<String> arguments) {
        return "ok";
      }
      public boolean lanes() {
        return true;
      }
      public List<Lane.Stamp> runLanes(List<Lane.Group> groups) {
        List<Lane.Stamp> hits = new ArrayList<>();
        long clock = 1L;
        for (int index = 0; index < groups.size(); index++) {
          assertEquals("t" + (index + 1), groups.get(index).tag());
        }
        for (Lane.Group group : groups) {
          for (Lane.Call call : group.calls()) {
            if (drop && "book".equals(call.method())) {
              continue;
            }
            hits.add(new Lane.Stamp(group.tag(), call.id(), clock, clock + 1, valueOf(call, boom, blank)));
            clock += 2;
          }
        }
        return hits;
      }
    };
  }

  private static String valueOf(Lane.Call call, boolean boom, boolean blank) {
    if (boom && "book".equals(call.method())) {
      return "__EXCEPTION__:message:<<down>>";
    }
    if (blank && "book".equals(call.method())) {
      return null;
    }
    if ("book".equals(call.method())) {
      return call.id().startsWith("l0") ? "booked" : "full";
    }
    return "seats".equals(call.method()) ? "0" : "1";
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
    Guard again = new Guard("seats=0", "", "left", "1");
    Guard modelOnly = new Guard("seats=0", "seats!=1", null, "");
    Command ping = new Command("ping", "", "", List.of(), List.of(result("ok")));
    return Plan.create(state("seats", "1"), 1, 2, List.of(open, shut, ping), List.of(guard, again, modelOnly));
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
