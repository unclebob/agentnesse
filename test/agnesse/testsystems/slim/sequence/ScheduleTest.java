package agnesse.testsystems.slim.sequence;

import agnesse.testsystems.slim.property.CellLanguage;
import agnesse.testsystems.slim.property.Seed;
import agnesse.testsystems.slim.property.Value;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ScheduleTest {
  @Test
  public void aSeedDrawsTheSameBranchesAndSkipsACommandTheOtherRowForbids() {
    Plan plan = desk();
    Schedule drawn = Schedule.draw(plan, new Rolls(0, 0, 0, 0, 0, 0, 0), 2, 2, 1, 1);
    assertEquals(1, drawn.prefix.steps.size());
    assertEquals(0, drawn.prefix.steps.get(0).command);
    assertEquals(2, drawn.branches.size());
    assertEquals(3, drawn.branches.get(0).steps.get(0).command);
    assertEquals(3, drawn.branches.get(1).steps.get(0).command);
    assertTrue(drawn.legal(plan));
    assertFalse(new Schedule(drawn.prefix, List.of(step(2), step(3))).legal(plan));
    assertFalse(new Schedule(step(2), List.of(step(3), step(3))).legal(plan));
    assertFalse(new Schedule(new Trace(List.of()), List.of(step(3))).legal(plan));

    String once = render(Schedule.draw(plan, new Seed(4).random(), 2, 3, 0, 2));
    assertEquals(once, render(Schedule.draw(plan, new Seed(4).random(), 2, 3, 0, 2)));
    assertFalse(once.equals(render(Schedule.draw(plan, new Seed(5).random(), 2, 3, 0, 2))));
  }

  @Test
  public void aBranchStopsWhenThePrefixLeavesNoCommandAndBoundsAreRefused() {
    Plan finish = Plan.raw(state("phase", "new"), 2, 2, List.of(
        command("finish", "phase=new", "phase=done")), List.of());
    Schedule drawn = Schedule.draw(finish, new Rolls(0, 0, 0, 0, 0, 0), 2, 2, 0, 0);
    assertEquals(0, drawn.prefix.steps.size());
    assertEquals(1, drawn.branches.get(0).steps.size());
    assertEquals(1, drawn.branches.get(1).steps.size());
    assertTrue(drawn.legal(finish));

    Plan plan = desk();
    assertEquals(3, Schedule.draw(plan, new Random(1), 3, 3, 1, 1).branches.size());
    assertEquals(0, Schedule.draw(plan, new Random(1), 2, 2, 0, 0).prefix.steps.size());
    Schedule high = Schedule.draw(plan, new Random(1), 2, 31, 0, 31);
    assertTrue(high.branches.size() >= 2 && high.branches.size() <= 31);
    assertTrue(high.prefix.steps.size() <= 31);
    Schedule top = Schedule.draw(plan, new Random(1), 2, 32, 0, 32);
    assertTrue(top.branches.size() <= 32);
    assertTrue(top.prefix.steps.size() <= 32);
    assertEquals("threads must be low..high from 2 through 32",
        message(() -> Schedule.draw(plan, new Random(1), 1, 2, 0, 0)));
    assertEquals("threads must be low..high from 2 through 32",
        message(() -> Schedule.draw(plan, new Random(1), 3, 2, 0, 0)));
    assertEquals("threads must be low..high from 2 through 32",
        message(() -> Schedule.draw(plan, new Random(1), 2, 33, 0, 0)));
    assertEquals("prefix must be low..high from 0 through 32",
        message(() -> Schedule.draw(plan, new Random(1), 2, 2, -1, 0)));
    assertEquals("prefix must be low..high from 0 through 32",
        message(() -> Schedule.draw(plan, new Random(1), 2, 2, 1, 0)));
    assertEquals("prefix must be low..high from 0 through 32",
        message(() -> Schedule.draw(plan, new Random(1), 2, 2, 0, 33)));
  }

  @Test
  public void shrinkingDropsThreadsThenThePrefixThenBranchCommands() {
    Plan plan = pings();
    Trace ping = step(0);
    Schedule start = new Schedule(new Trace(List.of(ping.steps.get(0), ping.steps.get(0))),
        List.of(ping, ping, ping));
    Fold fold = new Fold(plan, start);
    List<Schedule> pairs = fold.next();
    assertEquals(3, pairs.size());
    assertEquals(2, pairs.get(0).branches.size());
    assertEquals(2, pairs.get(0).prefix.steps.size());
    List<Schedule> emptied = fold.next();
    assertEquals(1, emptied.size());
    assertEquals(0, emptied.get(0).prefix.steps.size());
    assertEquals(3, emptied.get(0).branches.size());
    List<Schedule> singles = fold.next();
    assertEquals(2, singles.size());
    assertEquals(1, singles.get(0).prefix.steps.size());

    Fold accepted = new Fold(plan, start);
    accepted.accept(accepted.next().get(0));
    List<Schedule> shorter = accepted.next();
    assertEquals(2, accepted.schedule().branches.size());
    assertEquals(0, shorter.get(0).prefix.steps.size());
    assertEquals(2, shorter.get(0).branches.size());
  }

  @Test
  public void anIllegalThreadIsDroppedAndAnIllegalBranchOffersNothing() {
    Plan plan = desk();
    Fold fold = new Fold(plan, new Schedule(new Trace(List.of()), List.of(step(3), step(2), step(3))));
    List<Schedule> kept = fold.next();
    assertEquals(1, kept.size());
    assertEquals(3, kept.get(0).branches.get(0).steps.get(0).command);
    assertEquals(3, kept.get(0).branches.get(1).steps.get(0).command);

    Fold none = new Fold(plan, new Schedule(new Trace(List.of()), List.of(
        new Trace(List.of(new Step(2, List.of()), new Step(2, List.of()))),
        new Trace(List.of(new Step(2, List.of()), new Step(2, List.of()))))));
    assertTrue(none.next().isEmpty());
  }

  @Test
  public void aWideThreadCountDropsOneThreadAtATime() {
    Trace ping = step(0);
    List<Trace> many = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      many.add(ping);
    }
    Fold fold = new Fold(pings(), new Schedule(new Trace(List.of()), many));
    List<Schedule> dropped = fold.next();
    assertEquals(12, dropped.size());
    assertEquals(11, dropped.get(0).branches.size());
    assertEquals(11, dropped.get(11).branches.size());
  }

  @Test
  public void acceptedShrinksStartAgainFromTheSmallerSchedule() {
    Plan plan = pings();
    Trace ping = step(0);
    List<Trace> five = List.of(ping, ping, ping, ping, ping);
    Fold threads = new Fold(plan, new Schedule(new Trace(List.of()), five));
    acceptFirst(threads, schedule -> schedule.branches.size() >= 3);
    assertEquals(3, threads.schedule().branches.size());
    assertEquals(2, threads.next().get(0).branches.size());

    Fold prefix = new Fold(plan, new Schedule(new Trace(List.of(
        ping.steps.get(0), ping.steps.get(0), ping.steps.get(0))), List.of(ping, ping)));
    acceptFirst(prefix, schedule -> schedule.prefix.steps.size() >= 2);
    assertEquals(2, prefix.schedule().prefix.steps.size());
    assertEquals(0, prefix.next().get(0).prefix.steps.size());

    Fold branch = new Fold(plan, new Schedule(new Trace(List.of()), List.of(
        new Trace(List.of(ping.steps.get(0), ping.steps.get(0), ping.steps.get(0))), ping)));
    acceptFirst(branch, schedule -> schedule.branches.get(0).steps.size() >= 2);
    assertEquals(2, branch.schedule().branches.get(0).steps.size());
    assertEquals(1, branch.next().get(0).branches.get(0).steps.size());
  }

  @Test
  public void aCommandOnlyThePrefixEnablesIsNotOfferedFromAShorterPrefix() {
    Plan plan = life();
    Schedule start = new Schedule(
        new Trace(List.of(new Step(0, List.of()), new Step(1, List.of()))),
        List.of(step(3), step(3)));
    Fold fold = new Fold(plan, start);
    assertTrue(fold.next().isEmpty());

    Schedule open = new Schedule(step(0), List.of(
        new Trace(List.of(new Step(1, List.of()), new Step(3, List.of()))), step(2)));
    Fold kept = new Fold(plan, open);
    List<Schedule> cuts = kept.next();
    assertEquals(1, cuts.size());
    assertEquals(1, cuts.get(0).prefix.steps.size());
    assertEquals(1, cuts.get(0).branches.get(0).steps.get(0).command);
    assertEquals(2, cuts.get(0).branches.get(1).steps.get(0).command);
    kept.accept(cuts.get(0));
    assertEquals(1, kept.schedule().branches.get(0).steps.size());
  }

  @Test
  public void argumentsShrinkOnlyWhileTheCommandStillFailsAndTheCapStops() {
    Plan plan = ints();
    Schedule start = new Schedule(new Trace(List.of()), List.of(amount("40"), step(1)));
    Fold fold = new Fold(plan, start);
    assertFalse(fold.capped());
    Schedule shrunk = shrink(plan, start, ScheduleTest::heavy);
    assertEquals(2, shrunk.branches.size());
    assertEquals(0, shrunk.branches.get(0).steps.get(0).command);
    assertEquals("10", shrunk.branches.get(0).steps.get(0).value(1).cell());
    assertEquals("on", shrunk.branches.get(0).steps.get(0).value(0).cell());

    List<Schedule> first = fold.next();
    assertEquals("20", first.get(0).branches.get(0).steps.get(0).value(1).cell());
    for (int i = 0; i < Course.CAP; i++) {
      List<Schedule> batch = i == 0 ? first : fold.next();
      assertFalse(batch.isEmpty());
      fold.accept(batch.get(batch.size() - 1));
    }
    assertTrue(fold.next().isEmpty());
    assertTrue(fold.capped());
    assertEquals("8", fold.schedule().branches.get(0).steps.get(0).value(1).cell());

    Schedule prefix = shrink(plan, new Schedule(amount("40"), List.of(step(1), step(1))), ScheduleTest::heavy);
    assertEquals(0, prefix.prefix.steps.get(0).command);
    assertEquals("10", prefix.prefix.steps.get(0).value(1).cell());
  }

  @Test
  public void aLaterRunKeepsTheFailureAndAPassingProposalDoesNot() {
    int[] calls = {0};
    assertFalse(Fold.anyFail(0, () -> true));
    assertTrue(Fold.anyFail(3, () -> {
      calls[0]++;
      return true;
    }));
    assertEquals(1, calls[0]);
    int[] later = {0};
    assertTrue(Fold.anyFail(Fold.RERUNS, () -> later[0]++ == 1));
    assertEquals(2, later[0]);
    int[] none = {0};
    assertFalse(Fold.anyFail(2, () -> {
      none[0]++;
      return false;
    }));
    assertEquals(2, none[0]);
    assertEquals(3, Fold.RERUNS);
  }

  private static void acceptFirst(Fold fold, Predicate<Schedule> fails) {
    while (true) {
      List<Schedule> batch = fold.next();
      for (Schedule candidate : batch) {
        if (fails.test(candidate)) {
          fold.accept(candidate);
          return;
        }
      }
      if (batch.isEmpty()) {
        return;
      }
    }
  }

  private static Schedule shrink(Plan plan, Schedule start, Predicate<Schedule> fails) {
    Fold fold = new Fold(plan, start);
    acceptFirst(fold, fails);
    while (true) {
      List<Schedule> batch = fold.next();
      Schedule kept = null;
      for (Schedule candidate : batch) {
        if (fails.test(candidate)) {
          kept = candidate;
          break;
        }
      }
      if (kept == null) {
        if (batch.isEmpty()) {
          return fold.schedule();
        }
        continue;
      }
      fold.accept(kept);
    }
  }

  private static boolean heavy(Schedule schedule) {
    if (schedule.branches.size() < 2) {
      return false;
    }
    return amount(schedule.prefix) >= 10 || amount(schedule.branches.get(0)) >= 10;
  }

  private static int amount(Trace trace) {
    for (Step step : trace.steps) {
      if (step.command == 0 && step.size() > 1) {
        return Integer.parseInt(step.value(1).cell());
      }
    }
    return 0;
  }

  private static Plan desk() {
    return Plan.create(state("phase", "open"), 1, 1, List.of(
        command("reserve", "phase=open", "phase=held"),
        command("reserve", "phase=open", "phase=wait"),
        command("confirm", "phase=held", "phase=done"),
        command("ping", "", "")
    ), List.of());
  }

  private static Plan pings() {
    return Plan.create(Map.of(), 1, 1, List.of(command("ping", "", "")), List.of());
  }

  private static Plan life() {
    return Plan.create(state("phase", "new"), 1, 4, List.of(
        command("open", "phase=new", "phase=open"),
        command("block", "phase=open", "phase=blocked"),
        command("release", "phase=open", "phase=released"),
        command("release", "phase=blocked", "phase=cleared")
    ), List.of());
  }

  private static Plan ints() {
    return Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "n=@amount", literal("flag", "on"), generated("amount", "int:1..100")),
        command("ping", "", "")
    ), List.of());
  }

  private static Trace amount(String text) {
    return new Trace(List.of(new Step(0, List.of(Value.text("on"), Value.text(text)))));
  }

  private static Trace step(int command) {
    return new Trace(List.of(new Step(command, List.of())));
  }

  private static Command command(String name, String when, String next, Command.Argument... args) {
    return new Command(name, when, next, List.of(args),
        List.of(new Command.Expect(true, null, "ok", 0)));
  }

  private static Command.Argument generated(String name, String spec) {
    return new Command.Argument(name, 0, CellLanguage.parse(spec), null);
  }

  private static Command.Argument literal(String name, String text) {
    return new Command.Argument(name, 0, null, text);
  }

  private static Map<String, String> state(String... pairs) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put(pairs[i], pairs[i + 1]);
    }
    return map;
  }

  private static String render(Schedule schedule) {
    StringBuilder out = new StringBuilder();
    render(out, schedule.prefix);
    for (Trace branch : schedule.branches) {
      out.append('/');
      render(out, branch);
    }
    return out.toString();
  }

  private static void render(StringBuilder out, Trace trace) {
    for (Step step : trace.steps) {
      out.append(step.command).append(':');
      for (Value value : step.values()) {
        out.append(value == null ? "-" : value.cell()).append(',');
      }
      out.append(';');
    }
  }

  private static String message(ThrowingRunnable action) {
    return assertThrows(IllegalArgumentException.class, action).getMessage();
  }

  private static final class Rolls extends Random {
    private final ArrayDeque<Integer> rolls = new ArrayDeque<>();

    Rolls(int... values) {
      for (int value : values) {
        rolls.add(value);
      }
    }

    @Override
    public int nextInt(int bound) {
      int roll = rolls.remove();
      if (roll < 0 || roll >= bound) {
        throw new AssertionError(roll + " is outside " + bound);
      }
      return roll;
    }
  }
}
