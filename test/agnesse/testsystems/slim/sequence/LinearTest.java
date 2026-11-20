package agnesse.testsystems.slim.sequence;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LinearTest {
  @Test
  public void oneHeldAndOneFullAreLinearizable() {
    Plan desk = desk();
    Trace reserve = once(0);
    List<Trace> branches = List.of(reserve, reserve);
    List<List<Reading>> readings = List.of(List.of(Reading.returned("held")), List.of(Reading.returned("full")));
    assertEquals(Linear.Outcome.PASS, Linear.search(desk, new Trace(List.of()), List.of(), branches, readings));
  }

  @Test
  public void twoHeldBookingsAreNotLinearizable() {
    Plan desk = desk();
    Trace reserve = once(0);
    List<Trace> branches = List.of(reserve, reserve);
    List<List<Reading>> readings = List.of(List.of(Reading.returned("held")), List.of(Reading.returned("held")));
    assertEquals(Linear.Outcome.FAIL, Linear.search(desk, new Trace(List.of()), List.of(), branches, readings, Linear.BOUND));
  }

  @Test
  public void aPrefixThatMissesFailsBeforeTheBranches() {
    Plan desk = desk();
    Trace reserve = once(0);
    assertEquals(Linear.Outcome.FAIL, Linear.search(desk, reserve, List.of(Reading.returned("full")),
        List.of(), List.of()));
    assertEquals(Linear.Outcome.PASS, Linear.search(desk, reserve, List.of(Reading.returned("held")),
        List.of(new Trace(List.of()), reserve),
        List.of(List.of(), List.of(Reading.returned("full")))));
  }

  @Test
  public void theSearchStopsAtTheBoundAndAnEmptyHistoryPasses() {
    Plan meter = meter();
    List<Trace> branches = List.of(twice(0), twice(0));
    List<List<Reading>> readings = List.of(oks(2), oks(2));
    assertEquals(Linear.Outcome.BOUND, Linear.search(meter, new Trace(List.of()), List.of(), branches, readings, 1));
    assertEquals(Linear.Outcome.FAIL, Linear.search(meter, new Trace(List.of()), List.of(), branches, readings, 64));
    assertEquals(Linear.Outcome.PASS, Linear.search(desk(), new Trace(List.of()), List.of(), List.of(), List.of()));
    assertEquals(Linear.Outcome.BOUND, Linear.search(desk(), new Trace(List.of()), List.of(), List.of(), List.of(), 0));
  }

  private static Plan desk() {
    Command held = new Command("reserve", "slots=1", "slots=0", List.of(), List.of(result("held")));
    Command full = new Command("reserve", "slots=0", "slots=0", List.of(), List.of(result("full")));
    return Plan.create(state("slots", "1"), 1, 1, List.of(held, full), List.of());
  }

  private static Plan meter() {
    Command inc = new Command("inc", "", "n=@n+@one", List.of(), List.of(result("ok")));
    return Plan.raw(state("n", "0", "one", "1"), 1, 1, List.of(inc),
        List.of(new Guard("n=4", "n=0", null, "")));
  }

  private static Command.Expect result(String text) {
    return new Command.Expect(true, null, text, 3);
  }

  private static Trace once(int command) {
    return new Trace(List.of(new Step(command, List.of())));
  }

  private static Trace twice(int command) {
    return new Trace(List.of(new Step(command, List.of()), new Step(command, List.of())));
  }

  private static List<Reading> oks(int count) {
    List<Reading> readings = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      readings.add(Reading.returned("ok"));
    }
    return readings;
  }

  private static Map<String, String> state(String... pairs) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put(pairs[i], pairs[i + 1]);
    }
    return map;
  }
}
