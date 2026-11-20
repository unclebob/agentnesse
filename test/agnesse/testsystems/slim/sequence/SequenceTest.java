package agnesse.testsystems.slim.sequence;

import agnesse.testsystems.slim.property.CellLanguage;
import agnesse.testsystems.slim.property.Clock;
import agnesse.testsystems.slim.property.Seed;
import agnesse.testsystems.slim.property.Value;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SequenceTest {
  private static final Clock CLOCK = new Clock(ZoneId.of("UTC"), Instant.parse("2020-01-01T00:00:00Z"));

  @Test
  public void clausesCompareLiteralsAndReferences() {
    assertTrue(Clause.when("phase=open").get(0).holds(state("phase", "open")));
    assertFalse(Clause.when("phase=open").get(0).holds(state("phase", "blocked")));
    assertFalse(Clause.when("phase=open").get(0).holds(Map.of()));
    assertTrue(Clause.when("phase!=open").get(0).holds(state("phase", "blocked")));
    assertFalse(Clause.when("phase!=open").get(0).holds(state("phase", "open")));
    Clause same = Clause.model("phase=@amount").get(0);
    assertTrue(same.holds(state("phase", "open", "amount", "shut"), state("amount", "open")));
    assertFalse(same.holds(state("phase", "open"), state("amount", "shut")));
    assertFalse(same.holds(state("phase", "open"), Map.of()));
    Clause.when("phase=open; phase=open");
    Clause.when("phase!=open; phase!=blocked");
    Clause.when("phase!=open; phase!=open");
    Clause.when("phase=open; phase!=blocked");
    Clause.model("phase=@a; phase=@b");
    Clause.model("phase=open; phase=@b");
    Clause.when("phase=open; blocked=no");
    Clause.when(null);
    assertTrue(Clause.model("phase!=@other").get(0).holds(state("phase", "open", "other", "shut")));
    assertFalse(Clause.name("1"));
    assertTrue(Clause.name("a"));
  }

  @Test
  public void badClausesSayWhatIsWrong() {
    assertEquals("unsatisfiable precondition: phase=open; phase=blocked",
        message(() -> Clause.when("phase=open; phase=blocked")));
    assertEquals("unsatisfiable precondition: phase=open; phase!=open",
        message(() -> Clause.when("phase=open; phase!=open")));
    assertEquals("a precondition cannot name a drawn value", message(() -> Clause.when("phase=@x")));
    assertEquals("state must be name=value", message(() -> Clause.when("phase")));
    assertEquals("state must be name=value", message(() -> Clause.when("phase=open door")));
    assertEquals("state must be name=value", message(() -> Clause.when("a!=b!=c")));
    assertEquals("state must be name=value", message(() -> Clause.when("1phase=open")));
    assertEquals("state must be name=value", message(() -> Clause.when("phase=open;")));
    assertEquals("unknown reference ", message(() -> Clause.model("phase=@")));
    assertEquals("unknown reference 1", message(() -> Clause.model("phase=@1")));
    assertEquals("state must be name=value", message(() -> Clause.when("phase=")));
  }

  @Test
  public void assignmentsRunLeftToRightAndRefuseBadNumbers() {
    Binding sum = Binding.list("value=@a+@b").get(0);
    assertFalse(sum.literal());
    assertEquals(Set.of("a", "b"), sum.refs());
    assertEquals("5", sum.eval(state("a", "2", "b", "3"), Map.of()));
    assertEquals("5", sum.eval(state("a", "9", "b", "9"), state("a", "4", "b", "1")));
    assertEquals("-3", Binding.list("value=@a-@b").get(0).eval(state("a", "2", "b", "5"), Map.of()));
    assertTrue(Binding.list("phase=open").get(0).literal());
    assertEquals(Set.of(), Binding.list("phase=open").get(0).refs());
    assertEquals("value is already set", message(() -> Binding.list("value=1; value=2")));
    assertEquals("state must be name=value", message(() -> Binding.list("nope")));
    assertEquals("state must be name=value", message(() -> Binding.list("value=@a+@b-@c")));
    assertEquals("state must be name=value", message(() -> Binding.list("value=@a + @b")));
    assertEquals("a is not a whole number", message(() -> sum.eval(state("a", "", "b", "1"), Map.of())));
    assertEquals("a is not a whole number", message(() -> sum.eval(state("a", "01", "b", "1"), Map.of())));
    assertEquals("a is not a whole number",
        message(() -> sum.eval(state("a", "2147483647", "b", "2147483647"), Map.of())));
    assertEquals("unknown reference b", message(() -> sum.eval(state("a", "1"), Map.of())));
    assertEquals(List.of(), Binding.list(null));
    assertEquals("state must be name=value", message(() -> Binding.list("a=b=c")));
    assertEquals("state must be name=value", message(() -> Binding.list("1a=b")));
    assertEquals("state must be name=value", message(() -> Binding.list("value=")));
    assertEquals("state must be name=value", message(() -> Binding.list("value=@a+@b+@c")));
    assertEquals("unknown reference @", message(() -> Binding.list("value=@")));
    assertEquals("unknown reference a", message(() -> Binding.list("value=a+@b")));
    assertEquals("unknown reference @1", message(() -> Binding.list("value=@1")));
    Map<String, String> broken = new LinkedHashMap<>();
    broken.put("a", null);
    broken.put("b", "1");
    assertEquals("a is not a whole number", message(() -> sum.eval(broken, Map.of())));
  }

  @Test
  public void guardsAndCallsKeepOnlyTheCellsTheyWereGiven() {
    assertEquals("an invariant needs a model or a query", message(() -> new Guard(null, null, null, null)));
    assertEquals("an invariant needs a model or a query", message(() -> new Guard("", "", null, "")));
    assertEquals("an invariant needs a model or a query", message(() -> new Guard(" ", " ", " ", "false")));
    assertEquals("a check needs an expected value", message(() -> new Guard("", "", "released?", " ")));
    Guard guard = new Guard(" phase=locked ", " value=@locked ", " value? ", " @value ");
    assertEquals("phase=locked", guard.whenText);
    assertEquals("value?", guard.query);
    assertEquals("@value", guard.expect);
    Command set = command("set", " phase=new ", " phase=open ", literal("amount", "5"), omitted("note"));
    assertEquals("phase=new", set.whenText);
    assertEquals("phase=open", set.nextText);
    List<Value> values = new ArrayList<>();
    values.add(Value.text("5"));
    values.add(null);
    assertEquals(List.of("5"), set.call(values));
    assertEquals("5", set.args(values).get("amount"));
    assertFalse(set.args(values).containsKey("note"));
    Step step = new Step(1, values);
    values.set(0, Value.text("9"));
    assertEquals("5", step.value(0).cell());
    step.values().set(0, Value.text("8"));
    assertEquals("5", step.value(0).cell());
    assertEquals(2, step.size());
    List<Step> steps = new ArrayList<>();
    steps.add(step);
    Trace trace = new Trace(steps);
    assertThrows(UnsupportedOperationException.class, () -> trace.steps.add(step));
  }

  @Test
  public void aLifecycleDrawsOnlyLegalSequencesFromTheSeed() {
    Plan plan = life(1, 4);
    List<Trace> first = Course.draw(plan, new Seed(7).random(), 10);
    List<Trace> again = Course.draw(plan, new Seed(7).random(), 10);
    assertEquals(render(first), render(again));
    assertFalse(render(first).equals(render(Course.draw(plan, new Seed(8).random(), 10))));
    assertEquals("new", plan.initial.get("phase"));
    for (Trace trace : first) {
      assertTrue(plan.legal(trace));
      assertEquals(0, trace.steps.get(0).command);
      assertTrue(trace.steps.size() >= 1 && trace.steps.size() <= 4);
      holds(plan, trace);
    }
    Map<String, String> before = new LinkedHashMap<>(plan.initial);
    Map<String, String> after = plan.apply(before, new Step(0, List.of()));
    assertEquals("new", before.get("phase"));
    assertEquals("open", after.get("phase"));
    assertEquals("22=1 and 9", plan.substitute("@ab=@a and @amount",
        state("a", "1", "ab", "22", "amount", "3"), state("amount", "9")));
  }

  @Test
  public void eachTrialStartsFromTheInitialState() {
    Plan plan = Plan.create(state("phase", "new"), 1, 1, List.of(
        command("open", "phase=new", "phase=open")), List.of());
    List<Trace> traces = Course.draw(plan, new Rolls(0, 0, 0, 0), 2);
    assertEquals(1, traces.get(0).steps.size());
    assertEquals(1, traces.get(1).steps.size());
    assertEquals("new", plan.initial.get("phase"));
  }

  @Test
  public void aDrawStopsWhenNoCommandRemains() {
    Plan plan = Plan.create(state("phase", "new"), 1, 3, List.of(
        command("finish", "phase=new", "phase=done")), List.of());
    Trace trace = Course.drawOne(plan, new Rolls(2, 0));
    assertEquals(1, trace.steps.size());
  }

  @Test
  public void aLoopCanFillTheRequestedLength() {
    Plan plan = Plan.create(state("phase", "open"), 8, 8, List.of(
        command("loop", "phase=open", "phase=open")), List.of());
    assertEquals(8, Course.drawOne(plan, new Rolls(0, 0, 0, 0, 0, 0, 0, 0, 0)).steps.size());
  }

  @Test
  public void theEnabledCommandIsChosenInIndexOrder() {
    Plan plan = Plan.create(Map.of(), 1, 1, List.of(
        command("first", "", ""),
        command("second", "", "")), List.of());
    assertEquals(1, Course.drawOne(plan, new Rolls(0, 1)).steps.get(0).command);
  }

  @Test
  public void drawnArgumentsRespectLiteralsOmissionsAndEarlierColumns() {
    Command set = command("set", "", "", omitted("note"), literal("mark", "hi"), generated("amount", "int:1..1"),
        generated("end", "int after amount:0..9"));
    Plan plan = Plan.create(Map.of(), 1, 1, List.of(set), List.of());
    for (Trace trace : Course.draw(plan, new Seed(3).random(), 15)) {
      Step step = trace.steps.get(0);
      assertNull(step.value(0));
      assertEquals("hi", step.value(1).cell());
      int amount = Integer.parseInt(step.value(2).cell());
      int end = Integer.parseInt(step.value(3).cell());
      assertEquals(1, amount);
      assertTrue(end > amount);
    }
    Plan shrink = Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "", omitted("note"), generated("amount", "int:5..9"))), List.of());
    Trace trace = new Trace(List.of(new Step(0, java.util.Arrays.asList(null, Value.text("8")))));
    assertTrue(Course.simpler(shrink, trace, 0, 0).isEmpty());
    assertEquals("7", Course.simpler(shrink, trace, 0, 1).get(0).steps.get(0).value(1).cell());
    Plan after = Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "", generated("amount", "int:1..1"), generated("end", "int after amount:0..9"))), List.of());
    Trace wide = new Trace(List.of(new Step(0, List.of(Value.text("1"), Value.text("9")))));
    assertEquals(List.of("4", "8"), cellsAt(Course.simpler(after, wide, 0, 1), 1));
  }

  @Test
  public void shrinkingDropsCommandsThatAreNotNeeded() {
    Plan plan = life(1, 4);
    Trace trace = new Trace(List.of(new Step(0, List.of()), new Step(1, List.of()), new Step(3, List.of())));
    assertTrue(plan.legal(trace));
    assertFalse(plan.legal(new Trace(List.of(new Step(1, List.of())))));
    assertFalse(plan.legal(new Trace(List.of(new Step(0, List.of()), new Step(0, List.of())))));
    assertEquals(0, Course.shorter(plan, trace, 0).size());
    assertEquals(0, Course.shorter(plan, trace, 3).size());
    List<Trace> singles = Course.shorter(plan, trace, 1);
    assertEquals(1, singles.size());
    assertEquals(0, singles.get(0).steps.get(0).command);
    List<Trace> pairs = Course.shorter(plan, trace, 2);
    assertEquals(1, pairs.size());
    assertEquals(0, pairs.get(0).steps.get(0).command);
    assertEquals(1, pairs.get(0).steps.get(1).command);
  }

  @Test
  public void wideSubsequencesAreSkippedAndNarrowOnesStayInOrder() {
    Plan dozen = always(12);
    Trace twelve = indexed(12);
    assertEquals(0, Course.shorter(dozen, twelve, 2).size());
    assertEquals(12, Course.shorter(dozen, twelve, 1).size());
    assertEquals(12, Course.shorter(dozen, twelve, 11).size());
    List<Trace> pairs = Course.shorter(always(11), indexed(11), 2);
    assertEquals(55, pairs.size());
    assertEquals(0, pairs.get(0).steps.get(0).command);
    assertEquals(1, pairs.get(0).steps.get(1).command);
    assertEquals(0, Course.shorter(always(8), indexed(8), 4).size());
    assertEquals(56, Course.shorter(always(8), indexed(8), 3).size());
  }

  @Test
  public void argumentsShrinkTowardTheFloorAndSkipIllegalValues() {
    Plan plan = Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "value=@amount", generated("amount", "int:5..20"))), List.of());
    Trace trace = new Trace(List.of(new Step(0, List.of(Value.text("12")))));
    assertEquals(List.of("6", "11"), cells(Course.simpler(plan, trace, 0, 0)));
    assertTrue(Course.simpler(plan, new Trace(List.of(new Step(0, List.of(Value.text("5"))))), 0, 0).isEmpty());
    Command set = bare("set", "", "phase=@flag", generated("flag", "one of:shut,open,wide"));
    Command go = bare("go", "phase!=shut", "");
    Plan raw = Plan.raw(state("phase", "shut"), 1, 2, List.of(set, go), List.of());
    Trace failing = new Trace(List.of(
        new Step(0, List.of(Value.text("wide"))), new Step(1, List.of())));
    assertEquals(List.of("open"), cells(Course.simpler(raw, failing, 0, 0)));
    Command only = bare("set", "", "phase=@flag", generated("flag", "one of:shut,mid"));
    Plan blocked = Plan.raw(state("phase", "shut"), 1, 2, List.of(
        only, bare("go", "phase=open", "")), List.of());
    Trace mid = new Trace(List.of(new Step(0, List.of(Value.text("mid"))), new Step(1, List.of())));
    assertTrue(Course.simpler(blocked, mid, 0, 0).isEmpty());
    Plan literal = Plan.create(Map.of(), 1, 1, List.of(command("set", "", "", literal("note", "hi"))), List.of());
    assertTrue(Course.simpler(literal, new Trace(List.of(new Step(0, List.of(Value.text("hi"))))), 0, 0).isEmpty());
  }

  @Test
  public void laterAssignmentsSeeEarlierOnesAndArgumentsShadowState() {
    Command set = command("set", "", "value=@amount; doubled=@value+@value", generated("amount", "int:4..4"));
    Plan plan = Plan.create(state("amount", "1"), 1, 1, List.of(set), List.of());
    Map<String, String> next = plan.apply(plan.initial, new Step(0, List.of(Value.text("4"))));
    assertEquals("4", next.get("value"));
    assertEquals("8", next.get("doubled"));
    assertEquals("1", plan.initial.get("amount"));
    Map<String, String> given = state("phase", "new", "blocked", "no");
    Plan life = Plan.create(given, 1, 4, List.of(
        command("open", "phase=new", "phase=open"),
        command("block", "phase=open", "phase=blocked; blocked=yes"),
        command("release", "phase=open", "phase=released"),
        command("release", "phase=blocked", "")
    ), List.of(new Guard("blocked=yes", "phase!=released", null, "")));
    given.put("phase", "gone");
    assertEquals("new", life.initial.get("phase"));
    assertThrows(UnsupportedOperationException.class, () -> life.initial.put("extra", "no"));
  }

  @Test
  public void malformedMachinesSayWhatIsWrong() {
    assertEquals("a sequence needs a command",
        message(() -> Plan.create(Map.of(), 0, 0, List.of(), List.of())));
    assertEquals("a sequence needs a check",
        message(() -> Plan.create(Map.of(), 1, 1, List.of(bare("go", "", "")), List.of())));
    Plan.create(Map.of(), 1, 1, List.of(bare("a", "", ""), command("b", "", "")), List.of());
    assertEquals("steps must be low..high from 1 through 32",
        message(() -> Plan.create(Map.of(), 0, 1, List.of(command("go", "", "")), List.of())));
    assertEquals("steps must be low..high from 1 through 32",
        message(() -> Plan.create(Map.of(), 2, 1, List.of(command("go", "", "")), List.of())));
    assertEquals("steps must be low..high from 1 through 32",
        message(() -> Plan.create(Map.of(), 1, 33, List.of(command("go", "", "")), List.of())));
    Plan.create(Map.of(), 1, 32, List.of(command("go", "", "")), List.of());
    assertEquals("amount is already set", message(() -> Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "", generated("amount", "int:0..1"), generated("amount", "int:0..1"))), List.of())));
    assertEquals("all of replaces sampling", message(() -> Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "", generated("color", "all of:red,blue"))), List.of())));
    assertEquals("a sequence argument cannot draw a time", message(() -> Plan.create(Map.of(), 1, 1, List.of(
        new Command("set", "", "", List.of(new Command.Argument("when", 0, CellLanguage.parse("date", CLOCK), null)),
            List.of(ok()))), List.of())));
    assertEquals("a check needs an expected value", message(() -> Plan.create(Map.of(), 1, 1, List.of(
        new Command("go", "", "", List.of(), List.of(new Command.Expect(true, null, " ", 0)))), List.of())));
    assertEquals("a check needs an expected value", message(() -> Plan.create(Map.of(), 1, 1, List.of(
        new Command("go", "", "", List.of(), List.of(new Command.Expect(true, null, null, 0)))), List.of())));
    assertEquals("unknown reference nope", message(() -> Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "value=@nope")), List.of())));
    assertEquals("unknown reference amount", message(() -> Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "value=@amount", omitted("amount"))), List.of())));
    assertEquals("unknown reference value", message(() -> Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "doubled=@value; value=1")), List.of())));
    Plan.create(Map.of(), 1, 1, List.of(command("set", "", "value=1; doubled=@value")), List.of());
    Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "value=1"),
        command("show", "", "doubled=@value")), List.of());
    assertEquals("unknown reference missing", message(() -> Plan.create(state("phase", "open"), 1, 1, List.of(
        new Command("go", "", "", List.of(), List.of(new Command.Expect(true, null, "@missing", 0)))), List.of())));
    assertEquals("nope has no initial value", message(() -> Plan.create(state("phase", "open"), 1, 1, List.of(
        command("go", "phase=open", "")), List.of(new Guard("", "nope!=x", null, "")))));
    assertEquals("a precondition on phase cannot follow a drawn value", message(() -> Plan.create(
        state("phase", "new"), 1, 1, List.of(
            command("set", "", "phase=@flag", generated("flag", "one of:shut,open")),
            command("go", "phase=open", "")), List.of())));
    assertEquals("blocked has no initial value", message(() -> Plan.create(state("phase", "new"), 1, 1, List.of(
        command("open", "phase=new", "phase=open")), List.of(new Guard("blocked=yes", "phase!=released", null, "")))));
    assertEquals("no command is possible at the start", message(() -> Plan.create(state("phase", "done"), 1, 1, List.of(
        command("open", "phase=new", "phase=open")), List.of())));
    assertEquals("unsatisfiable precondition: phase=missing", message(() -> Plan.create(
        state("phase", "new", "missing", "no"), 1, 1, List.of(
            command("open", "phase=new", "phase=open"),
            command("nope", "phase=missing", "")), List.of())));
    assertEquals("unsatisfiable precondition: phase=missing", message(() -> Plan.create(
        state("phase", "new", "missing", "no"), 1, 2, List.of(
            command("open", "phase=new", "phase=open")),
        List.of(new Guard("phase=missing", "phase!=open", null, "")))));
    assertEquals("a sequence stops before 2 steps", message(() -> Plan.create(state("phase", "new"), 2, 3, List.of(
        command("stop", "phase=new", "phase=done"),
        command("open", "phase=new", "phase=open"),
        command("release", "phase=open", "phase=released")), List.of())));
    assertTrue(message(() -> Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "", generated("amount", "int after note:0..5"), generated("note", "int:0..5"))),
        List.of())).contains("later column"));
  }

  @Test
  public void namesInChecksFollowTheSameRulesAsState() {
    Plan.create(state("Phase", "open", "phase2", "open", "phase_b", "open"), 1, 1, List.of(
        new Command("go", "", "", List.of(), List.of(
            new Command.Expect(true, null, "@Phase @phase2 @phase_b", 0)))), List.of());
    assertEquals("unknown reference Phase", message(() -> Plan.create(Map.of(), 1, 1, List.of(
        new Command("go", "", "", List.of(), List.of(new Command.Expect(true, null, "@Phase", 0)))), List.of())));
    assertEquals("unknown reference Z", message(() -> expect("@Z")));
    assertEquals("unknown reference A", message(() -> expect("@A")));
    assertEquals("unknown reference z", message(() -> expect("@z")));
    assertEquals("unknown reference a", message(() -> expect("@a")));
    assertEquals("unknown reference a0", message(() -> expect("@a0")));
    assertEquals("unknown reference a9", message(() -> expect("@a9")));
    assertEquals("unknown reference a_b", message(() -> expect("@a_b")));
    assertEquals("unknown reference missing", message(() -> expect("@@missing")));
    Plan.create(Map.of(), 1, 1, List.of(
        new Command("go", "", "", List.of(), List.of(new Command.Expect(true, null, "see @1", 0)))), List.of());
    Plan.create(Map.of(), 1, 1, List.of(
        new Command("go", "", "", List.of(), List.of(new Command.Expect(true, null, "see @", 0)))), List.of());
    Plan.create(Map.of(), 1, 1, List.of(
        new Command("go", "", "", List.of(), List.of(new Command.Expect(true, null, "@[", 0)))), List.of());
    Plan.create(Map.of(), 1, 1, List.of(
        new Command("go", "", "", List.of(), List.of(new Command.Expect(true, null, "@{", 0)))), List.of());
    assertEquals("unknown reference a", message(() -> expect("@a!")));
    Plan.create(Map.of(), 1, 1, List.of(
        new Command("set", "", "", List.of(generated("amount", "int:0..1")),
            List.of(new Command.Expect(true, null, "@amount", 0)))), List.of());
    Plan.create(Map.of(), 1, 1, List.of(
        new Command("set", "", "audited=yes", List.of(),
            List.of(new Command.Expect(true, null, "@audited", 0)))), List.of());
    Plan.create(Map.of(), 1, 1, List.of(new Command("go", null, null, List.of(), List.of(ok()))),
        List.of(new Guard(null, null, "released?", "false")));
    assertEquals("unknown reference missing", message(() -> Plan.create(state("value", "1"), 1, 1, List.of(
        command("go", "", "")), List.of(new Guard("", "value=@missing", null, "")))));
    Plan.create(state("phase", "open", "value", "0"), 1, 2, List.of(
        command("lock", "phase=open", "phase=locked; audited=yes")),
        List.of(new Guard("phase=locked", "audited!=no", "value?", "@value")));
    Plan.create(Map.of(), 1, 1, List.of(
        command("set", "", "", generated("row", "record:n int:1..2"), generated("note", "pattern:AUD-[0-9]{2}"))
    ), List.of());
    List<Command> commands = new ArrayList<>();
    commands.add(command("go", "", ""));
    Plan plan = Plan.create(Map.of(), 1, 1, commands, List.of());
    commands.clear();
    assertEquals(1, plan.commands.size());
    assertEquals(0, Course.draw(plan, new Seed(1).random(), 0).size());
  }

  @Test
  public void anEndToEndModelKeepsALockedValueUntilAnAuditedChange() {
    Command set = command("set", "phase=open", "value=@amount", generated("amount", "int:0..3"));
    Command lock = command("lock", "phase=open", "phase=locked; lockedValue=@value");
    Command adjust = new Command("adjust", "phase=locked", "value=@value+@amount; audited=yes",
        List.of(generated("amount", "int:1..2"), generated("note", "pattern:A[0-9]{1}")),
        List.of(new Command.Expect(false, "value?", "@value", 3)));
    Plan plan = Plan.create(state("phase", "open", "value", "0", "audited", "no"), 1, 3, List.of(set, lock, adjust),
        List.of(new Guard("phase=locked; audited=no", "value=@lockedValue", null, "")));
    for (Trace trace : Course.draw(plan, new Seed(4).random(), 12)) {
      assertTrue(plan.legal(trace));
      holds(plan, trace);
    }
  }

  @Test
  public void walkDropsCommandsBeforeArgumentsAndStopsAtTheCap() {
    Walk dropped = new Walk(life(1, 4), new Trace(List.of(
        new Step(1, List.of()), new Step(3, List.of()))));
    assertTrue(dropped.next().isEmpty());
    assertFalse(dropped.capped());

    Walk prefix = new Walk(life(1, 4), new Trace(List.of(
        new Step(0, List.of()), new Step(1, List.of()), new Step(3, List.of()))));
    List<Trace> singles = prefix.next();
    assertEquals(0, singles.get(0).steps.get(0).command);
    prefix.accept(singles.get(0));
    assertEquals(1, prefix.trace().steps.size());
    assertTrue(prefix.next().isEmpty());

    Plan ints = Plan.create(state("n", "0"), 1, 1, List.of(
        command("set", "", "n=@amount", literal("flag", "on"), generated("amount", "int:1..100"))), List.of());
    Walk shrink = new Walk(ints, new Trace(List.of(
        new Step(0, List.of(Value.text("on"), Value.text("40"))))));
    List<Trace> first = shrink.next();
    assertEquals("20", first.get(0).steps.get(0).value(1).cell());
    for (int i = 0; i < Course.CAP; i++) {
      List<Trace> batch = i == 0 ? first : shrink.next();
      assertFalse(batch.isEmpty());
      shrink.accept(batch.get(batch.size() - 1));
    }
    assertTrue(shrink.next().isEmpty());
    assertTrue(shrink.capped());
    assertEquals("8", shrink.trace().steps.get(0).value(1).cell());
  }

  private static void expect(String template) {
    Plan.create(Map.of(), 1, 1, List.of(
        new Command("go", "", "", List.of(), List.of(new Command.Expect(true, null, template, 0)))), List.of());
  }

  private static void holds(Plan plan, Trace trace) {
    Map<String, String> state = new LinkedHashMap<>(plan.initial);
    for (Step step : trace.steps) {
      state = plan.apply(state, step);
      for (Guard guard : plan.guards) {
        if (matches(guard, state)) {
          for (Clause clause : guard.model) {
            assertTrue(guard.modelText, clause.holds(state, plan.commands.get(step.command).args(step.values())));
          }
        }
      }
    }
  }

  private static boolean matches(Guard guard, Map<String, String> state) {
    for (Clause clause : guard.when) {
      if (!clause.holds(state)) {
        return false;
      }
    }
    return true;
  }

  private static Plan life(int low, int high) {
    return Plan.create(state("phase", "new", "blocked", "no"), low, high, List.of(
        command("open", "phase=new", "phase=open"),
        command("block", "phase=open", "phase=blocked; blocked=yes"),
        command("release", "phase=open", "phase=released"),
        command("release", "phase=blocked", "")
    ), List.of(new Guard("blocked=yes", "phase!=released", null, "")));
  }

  private static Plan always(int count) {
    List<Command> commands = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      commands.add(command("c" + i, "", ""));
    }
    return Plan.create(Map.of(), 1, 1, commands, List.of());
  }

  private static Trace indexed(int count) {
    List<Step> steps = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      steps.add(new Step(i, List.of()));
    }
    return new Trace(steps);
  }

  private static List<String> cells(List<Trace> traces) {
    return cellsAt(traces, 0);
  }

  private static List<String> cellsAt(List<Trace> traces, int arg) {
    List<String> out = new ArrayList<>();
    for (Trace trace : traces) {
      out.add(trace.steps.get(0).value(arg).cell());
    }
    return out;
  }

  private static String render(List<Trace> traces) {
    StringBuilder out = new StringBuilder();
    for (Trace trace : traces) {
      for (Step step : trace.steps) {
        out.append(step.command).append(':');
        for (Value value : step.values()) {
          out.append(value == null ? "-" : value.cell()).append(',');
        }
        out.append(';');
      }
      out.append('|');
    }
    return out.toString();
  }

  private static Command command(String name, String when, String next, Command.Argument... args) {
    return new Command(name, when, next, List.of(args), List.of(ok()));
  }

  private static Command bare(String name, String when, String next, Command.Argument... args) {
    return new Command(name, when, next, List.of(args), List.of());
  }

  private static Command.Expect ok() {
    return new Command.Expect(true, null, "ok", 0);
  }

  private static Command.Argument generated(String name, String spec) {
    return new Command.Argument(name, 0, CellLanguage.parse(spec), null);
  }

  private static Command.Argument literal(String name, String text) {
    return new Command.Argument(name, 0, null, text);
  }

  private static Command.Argument omitted(String name) {
    return new Command.Argument(name, 0, null, null);
  }

  private static Map<String, String> state(String... pairs) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put(pairs[i], pairs[i + 1]);
    }
    return map;
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
