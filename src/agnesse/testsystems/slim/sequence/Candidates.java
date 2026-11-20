package agnesse.testsystems.slim.sequence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * The model states still consistent with what has happened. A command row is
 * drawn only when its precondition holds in every state. After the fixture
 * answers, every row of that command whose precondition held and whose
 * expectations match is kept. The trial fails when none remain.
 */
public final class Candidates {
  public static final int CAP = 64;
  public static final String AMBIGUOUS = "model too ambiguous";

  private final List<Map<String, String>> states;

  private Candidates(List<Map<String, String>> states) {
    this.states = List.copyOf(states);
  }

  public static Candidates start(Map<String, String> initial) {
    return new Candidates(List.of(new LinkedHashMap<>(initial)));
  }

  public int size() {
    return states.size();
  }

  /** A stable name for this set, so a search can skip a state it has already tried. */
  public String mark() {
    StringJoiner joiner = new StringJoiner(";");
    for (Map<String, String> state : states) {
      joiner.add(key(state));
    }
    return joiner.toString();
  }

  public boolean has(String name, String value) {
    for (Map<String, String> state : states) {
      if (value.equals(state.get(name))) {
        return true;
      }
    }
    return false;
  }

  /** The row's precondition holds in every candidate, so the call is safe in all of them. */
  public boolean allows(Plan plan, int command) {
    if (states.isEmpty()) {
      return false;
    }
    for (Map<String, String> state : states) {
      if (!plan.allows(command, state)) {
        return false;
      }
    }
    return true;
  }

  public List<Integer> allowed(Plan plan) {
    List<Integer> indexes = new ArrayList<>();
    for (int index = 0; index < plan.commands.size(); index++) {
      if (allows(plan, index)) {
        indexes.add(index);
      }
    }
    return indexes;
  }

  /**
   * Every row of this command's name whose precondition holds in some candidate.
   * The drawn arguments are the ones on {@code step}.
   */
  public Candidates image(Plan plan, Step step) {
    List<Map<String, String>> posts = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    String name = plan.commands.get(step.command).name;
    Map<String, String> args = plan.commands.get(step.command).args(step.values());
    for (Map<String, String> state : states) {
      addPosts(plan, name, args, state, posts, seen);
    }
    if (posts.size() > CAP) {
      throw new IllegalArgumentException(AMBIGUOUS);
    }
    return new Candidates(posts);
  }

  public boolean accepts(Plan plan, Trace trace) {
    try {
      return replay(plan, trace);
    } catch (IllegalArgumentException e) {
      if (AMBIGUOUS.equals(e.getMessage())) {
        return false;
      }
      throw e;
    }
  }

  public Advance take(Plan plan, Step step, Reading reading) {
    List<Integer> open = open(plan, step);
    Map<String, String> args = plan.commands.get(step.command).args(step.values());
    List<Integer> fitting = new ArrayList<>();
    List<Map<String, String>> posts = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    collect(plan, step, args, reading, open, fitting, posts, seen);
    int shown = show(step.command, fitting);
    if (posts.isEmpty()) {
      return miss(plan, open, args, reading, shown);
    }
    if (posts.size() > CAP) {
      throw new IllegalArgumentException(AMBIGUOUS);
    }
    Advance broken = invariants(plan, posts, args, reading, shown);
    if (broken != null) {
      return broken;
    }
    return Advance.ok(new Candidates(posts), shown);
  }

  /** Query methods to call for this step: command checks, then invariant queries. */
  public List<String> queries(Plan plan, Step step) {
    List<String> names = new ArrayList<>();
    expects(plan, step, names);
    guards(plan, step, names);
    return names;
  }

  Map<String, String> firstMatch(Plan plan, int index) {
    for (Map<String, String> state : states) {
      if (plan.allows(index, state)) {
        return state;
      }
    }
    return states.get(0);
  }

  private boolean replay(Plan plan, Trace trace) {
    Candidates cast = this;
    for (Step step : trace.steps) {
      if (!cast.allows(plan, step.command)) {
        return false;
      }
      cast = cast.image(plan, step);
    }
    return true;
  }

  private void addPosts(Plan plan, String name, Map<String, String> args, Map<String, String> state,
                        List<Map<String, String>> posts, Set<String> seen) {
    for (int index = 0; index < plan.commands.size(); index++) {
      Command row = plan.commands.get(index);
      if (!row.name.equals(name) || !plan.allows(index, state)) {
        continue;
      }
      Map<String, String> post = plan.apply(state, index, args);
      if (seen.add(key(post))) {
        posts.add(post);
      }
    }
  }

  private void collect(Plan plan, Step step, Map<String, String> args, Reading reading, List<Integer> open,
                       List<Integer> fitting, List<Map<String, String>> posts, Set<String> seen) {
    for (Map<String, String> state : states) {
      for (int index : open) {
        if (!plan.allows(index, state)) {
          continue;
        }
        Map<String, String> post = plan.apply(state, index, args);
        if (!fits(plan, index, post, args, reading)) {
          continue;
        }
        if (seen.add(key(post))) {
          fitting.add(index);
          posts.add(post);
        }
      }
    }
  }

  private List<Integer> open(Plan plan, Step step) {
    List<Integer> indexes = new ArrayList<>();
    String name = plan.commands.get(step.command).name;
    for (int index = 0; index < plan.commands.size(); index++) {
      Command row = plan.commands.get(index);
      if (row.name.equals(name) && matchesAny(plan, index)) {
        indexes.add(index);
      }
    }
    return indexes;
  }

  private boolean matchesAny(Plan plan, int index) {
    for (Map<String, String> state : states) {
      if (plan.allows(index, state)) {
        return true;
      }
    }
    return false;
  }

  private Advance miss(Plan plan, List<Integer> open, Map<String, String> args, Reading reading, int shown) {
    if (open.isEmpty()) {
      return Advance.empty(shown, 0, text(reading.returned()), "no row matched",
          "saw " + text(reading.returned()) + "; no row matched");
    }
    if (states.size() == 1 && open.size() == 1) {
      return single(plan, open.get(0), args, reading, shown);
    }
    return many(plan, open, args, reading, shown);
  }

  private Advance single(Plan plan, int index, Map<String, String> args, Reading reading, int shown) {
    Map<String, String> post = plan.apply(states.get(0), index, args);
    String[] parts = mismatch(plan, plan.commands.get(index), post, args, reading);
    return Advance.cell(shown, Integer.parseInt(parts[0]), parts[1], parts[2]);
  }

  private Advance many(Plan plan, List<Integer> open, Map<String, String> args, Reading reading, int shown) {
    String actual = seen(plan, open, reading);
    String expected = joined(plan, open, args);
    return Advance.empty(shown, column(plan, open), actual, expected, "saw " + actual + "; " + expected);
  }

  private Advance invariants(Plan plan, List<Map<String, String>> posts, Map<String, String> args,
                             Reading reading, int shown) {
    for (int index = 0; index < plan.guards.size(); index++) {
      Advance failed = guard(plan, plan.guards.get(index), index, posts, args, reading, shown);
      if (failed != null) {
        return failed;
      }
    }
    return null;
  }

  private Advance guard(Plan plan, Guard guard, int index, List<Map<String, String>> posts,
                        Map<String, String> args, Reading reading, int shown) {
    for (Map<String, String> post : posts) {
      if (!Clause.holds(guard.when, post)) {
        continue;
      }
      Advance model = model(guard, index, post, args, shown);
      if (model != null) {
        return model;
      }
      Advance query = queried(plan, guard, index, post, args, reading, shown);
      if (query != null) {
        return query;
      }
    }
    return null;
  }

  private Advance model(Guard guard, int index, Map<String, String> post, Map<String, String> args, int shown) {
    for (Clause check : guard.model) {
      if (check.holds(post, args)) {
        continue;
      }
      return Advance.guard(shown, index, check.name, text(post.get(check.name)), text(check.expected(post, args)), true);
    }
    return null;
  }

  private Advance queried(Plan plan, Guard guard, int index, Map<String, String> post,
                          Map<String, String> args, Reading reading, int shown) {
    if (guard.query == null) {
      return null;
    }
    String expected = plan.substitute(guard.expect, post, args);
    String actual = reading.queries().get(guard.query);
    if (expected.equals(text(actual))) {
      return null;
    }
    return Advance.guard(shown, index, guard.query, text(actual), expected, false);
  }

  private void expects(Plan plan, Step step, List<String> names) {
    String name = plan.commands.get(step.command).name;
    for (int index = 0; index < plan.commands.size(); index++) {
      Command row = plan.commands.get(index);
      if (row.name.equals(name) && matchesAny(plan, index)) {
        row.collectQueries(names);
      }
    }
  }

  private void guards(Plan plan, Step step, List<String> names) {
    Candidates next = image(plan, step);
    for (Guard guard : plan.guards) {
      if (guard.query != null && !names.contains(guard.query) && next.matches(guard)) {
        names.add(guard.query);
      }
    }
  }

  private boolean matches(Guard guard) {
    for (Map<String, String> state : states) {
      if (Clause.holds(guard.when, state)) {
        return true;
      }
    }
    return false;
  }

  private boolean fits(Plan plan, int index, Map<String, String> post, Map<String, String> args, Reading reading) {
    return mismatch(plan, plan.commands.get(index), post, args, reading) == null;
  }

  private String[] mismatch(Plan plan, Command row, Map<String, String> post, Map<String, String> args, Reading reading) {
    String[] result = scan(plan, row, post, args, reading, true);
    if (result != null) {
      return result;
    }
    return scan(plan, row, post, args, reading, false);
  }

  private String[] scan(Plan plan, Command row, Map<String, String> post, Map<String, String> args,
                        Reading reading, boolean results) {
    for (Command.Expect expect : row.expects) {
      if (expect.result != results) {
        continue;
      }
      String expected = plan.substitute(expect.template, post, args);
      String actual = actual(expect, reading);
      if (!actual.equals(expected)) {
        return new String[] {Integer.toString(expect.column), actual, expected};
      }
    }
    return null;
  }

  private static String actual(Command.Expect expect, Reading reading) {
    String value = expect.result ? reading.returned() : reading.queries().get(expect.method);
    return text(value);
  }

  private static int show(int chosen, List<Integer> fitting) {
    if (fitting.contains(chosen) || fitting.isEmpty()) {
      return chosen;
    }
    return fitting.get(0);
  }

  private String seen(Plan plan, List<Integer> open, Reading reading) {
    StringJoiner joiner = new StringJoiner(", ");
    if (wantsResult(plan, open)) {
      joiner.add(text(reading.returned()));
    }
    for (String query : queryNames(plan, open)) {
      joiner.add(query + "=" + text(reading.queries().get(query)));
    }
    return joiner.toString();
  }

  private static boolean wantsResult(Plan plan, List<Integer> open) {
    for (int index : open) {
      for (Command.Expect expect : plan.commands.get(index).expects) {
        if (expect.result) {
          return true;
        }
      }
    }
    return false;
  }

  private static List<String> queryNames(Plan plan, List<Integer> open) {
    List<String> names = new ArrayList<>();
    for (int index : open) {
      plan.commands.get(index).collectQueries(names);
    }
    return names;
  }

  private String joined(Plan plan, List<Integer> open, Map<String, String> args) {
    StringJoiner joiner = new StringJoiner(" or ");
    for (int index : open) {
      joiner.add(rowExpected(plan, index, args));
    }
    return joiner.toString();
  }

  private String rowExpected(Plan plan, int index, Map<String, String> args) {
    Map<String, String> post = plan.apply(firstMatch(plan, index), index, args);
    StringJoiner joiner = new StringJoiner(", ");
    Command row = plan.commands.get(index);
    for (Command.Expect expect : row.expects) {
      joiner.add(plan.substitute(expect.template, post, args));
    }
    return joiner.toString();
  }

  private int column(Plan plan, List<Integer> open) {
    int query = -1;
    for (int index : open) {
      for (Command.Expect expect : plan.commands.get(index).expects) {
        if (expect.result) {
          return expect.column;
        }
        if (query < 0) {
          query = expect.column;
        }
      }
    }
    return query;
  }

  private static String key(Map<String, String> state) {
    StringBuilder out = new StringBuilder();
    for (Map.Entry<String, String> entry : state.entrySet()) {
      String value = entry.getValue();
      out.append(entry.getKey()).append('=').append(value.length()).append(':').append(value).append('\u0001');
    }
    return out.toString();
  }

  private static String text(String value) {
    return value == null ? "" : value;
  }
}
