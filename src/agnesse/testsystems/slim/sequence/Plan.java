package agnesse.testsystems.slim.sequence;

import agnesse.testsystems.slim.property.Generator;
import agnesse.testsystems.slim.property.Links;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A validated command machine. Preconditions are literals, so drawing and
 * shrinking are a function of the seed and do not call Slim.
 */
public final class Plan {
  public final Map<String, String> initial;
  public final int stepsMin;
  public final int stepsMax;
  public final List<Command> commands;
  public final List<Guard> guards;

  private Plan(Map<String, String> initial, int stepsMin, int stepsMax,
               List<Command> commands, List<Guard> guards) {
    this.initial = Collections.unmodifiableMap(new LinkedHashMap<>(initial));
    this.stepsMin = stepsMin;
    this.stepsMax = stepsMax;
    this.commands = List.copyOf(commands);
    this.guards = List.copyOf(guards);
  }

  public static Plan create(Map<String, String> initial, int stepsMin, int stepsMax,
                            List<Command> commands, List<Guard> guards) {
    if (commands.isEmpty()) {
      throw new IllegalArgumentException("a sequence needs a command");
    }
    steps(stepsMin, stepsMax);
    arguments(commands);
    checks(commands, guards);
    refs(initial, commands);
    names(initial, commands, guards);
    List<String> controls = controls(commands, guards);
    initial(initial, controls);
    literals(commands, controls);
    boolean[] hit = new boolean[commands.size()];
    List<Map<String, String>> states = new ArrayList<>();
    int terminal = walk(initial, commands, controls, hit, states);
    demand(commands, hit, guards, states, terminal, stepsMin);
    return new Plan(initial, stepsMin, stepsMax, commands, guards);
  }

  /** A machine that skips validation. Tests use it to shrink an argument that breaks a precondition. */
  static Plan raw(Map<String, String> initial, int stepsMin, int stepsMax,
                  List<Command> commands, List<Guard> guards) {
    return new Plan(initial, stepsMin, stepsMax, commands, guards);
  }

  public boolean allows(int command, Map<String, String> state) {
    return Clause.holds(commands.get(command).when, state);
  }

  public boolean legal(Trace trace) {
    Map<String, String> state = new LinkedHashMap<>(initial);
    for (Step step : trace.steps) {
      if (!allows(step.command, state)) {
        return false;
      }
      state = apply(state, step);
    }
    return true;
  }

  public Map<String, String> apply(Map<String, String> state, Step step) {
    Command command = commands.get(step.command);
    Map<String, String> next = new LinkedHashMap<>(state);
    for (Binding binding : command.next) {
      next.put(binding.name, binding.eval(next, command.args(step.values())));
    }
    return next;
  }

  public String substitute(String template, Map<String, String> state, Map<String, String> args) {
    String text = template;
    for (String name : longest(state, args)) {
      text = text.replace("@" + name, value(state, args, name));
    }
    return text;
  }

  private static void steps(int low, int high) {
    if (low < 1 || high < low || high > Course.CAP) {
      throw new IllegalArgumentException("steps must be low..high from 1 through " + Course.CAP);
    }
  }

  private static void arguments(List<Command> commands) {
    for (Command command : commands) {
      argument(command);
    }
  }

  private static void argument(Command command) {
    Set<String> seen = new LinkedHashSet<>();
    for (Command.Argument arg : command.arguments) {
      if (!seen.add(arg.name)) {
        throw new IllegalArgumentException(arg.name + " is already set");
      }
      refuse(arg.generator);
    }
    for (Command.Expect expect : command.expects) {
      blank(expect.template);
    }
    bind(command);
  }

  private static void blank(String template) {
    if (template == null || template.isBlank()) {
      throw new IllegalArgumentException("a check needs an expected value");
    }
  }

  private static void refuse(Generator generator) {
    if (generator == null) {
      return;
    }
    if (generator.domain() != null) {
      throw new IllegalArgumentException("all of replaces sampling");
    }
    if (generator.timed()) {
      throw new IllegalArgumentException("a sequence argument cannot draw a time");
    }
  }

  private static void bind(Command command) {
    List<String> names = new ArrayList<>();
    List<Generator> generators = new ArrayList<>();
    for (Command.Argument arg : command.arguments) {
      if (arg.generator != null) {
        names.add(arg.name);
        generators.add(arg.generator);
      }
    }
    Links.bind(names, generators);
  }

  private static void checks(List<Command> commands, List<Guard> guards) {
    if (!guards.isEmpty() || hasExpect(commands)) {
      return;
    }
    throw new IllegalArgumentException("a sequence needs a check");
  }

  private static boolean hasExpect(List<Command> commands) {
    for (Command command : commands) {
      if (!command.expects.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static void refs(Map<String, String> initial, List<Command> commands) {
    for (Command command : commands) {
      bound(initial, commands, command);
    }
  }

  private static void bound(Map<String, String> initial, List<Command> commands, Command command) {
    Set<String> known = known(initial, commands, command);
    for (Binding binding : command.next) {
      for (String ref : binding.refs()) {
        if (!known.contains(ref)) {
          throw new IllegalArgumentException("unknown reference " + ref);
        }
      }
      known.add(binding.name);
    }
  }

  private static Set<String> known(Map<String, String> initial, List<Command> commands, Command command) {
    Set<String> known = new LinkedHashSet<>(initial.keySet());
    for (Command other : commands) {
      if (other != command) {
        addBindings(known, other);
      }
    }
    for (Command.Argument argument : command.arguments) {
      if (!argument.omitted()) {
        known.add(argument.name);
      }
    }
    return known;
  }

  private static void names(Map<String, String> initial, List<Command> commands, List<Guard> guards) {
    Set<String> assigned = assigned(commands);
    Set<String> drawn = drawn(commands);
    modelNames(initial, assigned, drawn, guards);
    expectNames(initial, assigned, drawn, commands);
  }

  private static Set<String> assigned(List<Command> commands) {
    Set<String> names = new LinkedHashSet<>();
    for (Command command : commands) {
      addBindings(names, command);
    }
    return names;
  }

  private static void addBindings(Set<String> names, Command command) {
    for (Binding binding : command.next) {
      names.add(binding.name);
    }
  }

  private static Set<String> drawn(List<Command> commands) {
    Set<String> names = new LinkedHashSet<>();
    for (Command command : commands) {
      for (Command.Argument argument : command.arguments) {
        if (!argument.omitted()) {
          names.add(argument.name);
        }
      }
    }
    return names;
  }

  private static void modelNames(Map<String, String> initial, Set<String> assigned, Set<String> drawn,
                                 List<Guard> guards) {
    for (Guard guard : guards) {
      for (Clause clause : guard.model) {
        requireState(initial, assigned, clause.name);
        if (clause.reference && !known(initial, assigned, drawn, clause.value)) {
          throw new IllegalArgumentException("unknown reference " + clause.value);
        }
      }
    }
  }

  private static void expectNames(Map<String, String> initial, Set<String> assigned, Set<String> drawn,
                                  List<Command> commands) {
    for (Command command : commands) {
      for (Command.Expect expect : command.expects) {
        mentioned(initial, assigned, drawn, expect.template);
      }
    }
  }

  private static void mentioned(Map<String, String> initial, Set<String> assigned, Set<String> drawn, String template) {
    for (String name : mentions(template)) {
      if (!known(initial, assigned, drawn, name)) {
        throw new IllegalArgumentException("unknown reference " + name);
      }
    }
  }

  private static void requireState(Map<String, String> initial, Set<String> assigned, String name) {
    if (!initial.containsKey(name) && !assigned.contains(name)) {
      throw new IllegalArgumentException(name + " has no initial value");
    }
  }

  private static boolean known(Map<String, String> initial, Set<String> assigned, Set<String> drawn, String name) {
    return initial.containsKey(name) || assigned.contains(name) || drawn.contains(name);
  }

  private static List<String> mentions(String template) {
    List<String> names = new ArrayList<>();
    int index = 0;
    while (index < template.length()) {
      if (template.charAt(index) != '@') {
        index++;
        continue;
      }
      int end = readName(template, index + 1);
      if (end > index + 1) {
        names.add(template.substring(index + 1, end));
      }
      index = end;
    }
    return names;
  }

  private static int readName(String template, int start) {
    int end = start;
    if (end >= template.length() || !nameStart(template.charAt(end))) {
      return start;
    }
    end++;
    while (end < template.length() && namePart(template.charAt(end))) {
      end++;
    }
    return end;
  }

  private static boolean nameStart(char c) {
    return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
  }

  private static boolean namePart(char c) {
    return nameStart(c) || (c >= '0' && c <= '9') || c == '_';
  }

  private static List<String> controls(List<Command> commands, List<Guard> guards) {
    List<String> names = new ArrayList<>();
    for (Command command : commands) {
      addClauses(names, command.when);
    }
    for (Guard guard : guards) {
      addClauses(names, guard.when);
    }
    return names;
  }

  private static void addClauses(List<String> names, List<Clause> clauses) {
    for (Clause clause : clauses) {
      if (!names.contains(clause.name)) {
        names.add(clause.name);
      }
    }
  }

  private static void initial(Map<String, String> state, List<String> controls) {
    for (String name : controls) {
      if (!state.containsKey(name)) {
        throw new IllegalArgumentException(name + " has no initial value");
      }
    }
  }

  private static void literals(List<Command> commands, List<String> controls) {
    for (Command command : commands) {
      literal(command, controls);
    }
  }

  private static void literal(Command command, List<String> controls) {
    for (Binding binding : command.next) {
      if (controls.contains(binding.name) && !binding.literal()) {
        throw new IllegalArgumentException(
            "a precondition on " + binding.name + " cannot follow a drawn value");
      }
    }
  }

  private static int walk(Map<String, String> initial, List<Command> commands, List<String> controls,
                          boolean[] hit, List<Map<String, String>> states) {
    Map<String, Map<String, String>> visited = new LinkedHashMap<>();
    ArrayDeque<Node> queue = new ArrayDeque<>();
    enqueue(queue, visited, states, project(initial, controls), 0);
    int terminal = Integer.MAX_VALUE;
    while (!queue.isEmpty()) {
      Node node = queue.remove();
      List<Integer> enabled = enabled(commands, node.state);
      terminal = stopped(terminal, enabled, node.depth);
      fan(commands, controls, hit, visited, states, queue, node, enabled);
    }
    if (!any(hit)) {
      throw new IllegalArgumentException("no command is possible at the start");
    }
    return terminal;
  }

  private static void enqueue(ArrayDeque<Node> queue, Map<String, Map<String, String>> visited,
                              List<Map<String, String>> states, Map<String, String> state, int depth) {
    if (visited.putIfAbsent(key(state), state) == null) {
      states.add(state);
      queue.add(new Node(state, depth));
    }
  }

  private static int stopped(int terminal, List<Integer> enabled, int depth) {
    if (enabled.isEmpty()) {
      return Math.min(terminal, depth);
    }
    return terminal;
  }

  private static void fan(List<Command> commands, List<String> controls, boolean[] hit,
                          Map<String, Map<String, String>> visited, List<Map<String, String>> states,
                          ArrayDeque<Node> queue, Node node, List<Integer> enabled) {
    for (int index : enabled) {
      hit[index] = true;
      enqueue(queue, visited, states, move(node.state, commands.get(index), controls), node.depth + 1);
    }
  }

  private static boolean any(boolean[] hit) {
    for (boolean seen : hit) {
      if (seen) {
        return true;
      }
    }
    return false;
  }

  private static void demand(List<Command> commands, boolean[] hit, List<Guard> guards,
                             List<Map<String, String>> states, int terminal, int stepsMin) {
    unseen(commands, hit);
    unguarded(guards, states);
    if (terminal < stepsMin) {
      throw new IllegalArgumentException("a sequence stops before " + stepsMin + " steps");
    }
  }

  private static void unseen(List<Command> commands, boolean[] hit) {
    for (int i = 0; i < commands.size(); i++) {
      if (!hit[i]) {
        throw new IllegalArgumentException("unsatisfiable precondition: " + commands.get(i).whenText);
      }
    }
  }

  private static void unguarded(List<Guard> guards, List<Map<String, String>> states) {
    for (Guard guard : guards) {
      if (!matches(guard, states)) {
        throw new IllegalArgumentException("unsatisfiable precondition: " + guard.whenText);
      }
    }
  }

  private static boolean matches(Guard guard, List<Map<String, String>> states) {
    if (guard.when.isEmpty()) {
      return true;
    }
    for (Map<String, String> state : states) {
      if (Clause.holds(guard.when, state)) {
        return true;
      }
    }
    return false;
  }

  private static List<Integer> enabled(List<Command> commands, Map<String, String> state) {
    List<Integer> indexes = new ArrayList<>();
    for (int i = 0; i < commands.size(); i++) {
      if (Clause.holds(commands.get(i).when, state)) {
        indexes.add(i);
      }
    }
    return indexes;
  }

  private static Map<String, String> move(Map<String, String> state, Command command, List<String> controls) {
    Map<String, String> next = new LinkedHashMap<>(state);
    for (Binding binding : command.next) {
      if (controls.contains(binding.name)) {
        next.put(binding.name, binding.eval(next, Map.of()));
      }
    }
    return next;
  }

  private static Map<String, String> project(Map<String, String> state, List<String> controls) {
    Map<String, String> projected = new LinkedHashMap<>();
    for (String name : controls) {
      projected.put(name, state.get(name));
    }
    return projected;
  }

  private static String key(Map<String, String> state) {
    StringBuilder out = new StringBuilder();
    for (String value : state.values()) {
      out.append(value.length()).append(':').append(value).append('\u0001');
    }
    return out.toString();
  }

  private static List<String> longest(Map<String, String> state, Map<String, String> args) {
    Set<String> names = new LinkedHashSet<>(state.keySet());
    names.addAll(args.keySet());
    List<String> ordered = new ArrayList<>(names);
    ordered.sort((left, right) -> Integer.compare(right.length(), left.length()));
    return ordered;
  }

  private static String value(Map<String, String> state, Map<String, String> args, String name) {
    if (args.containsKey(name)) {
      return args.get(name);
    }
    return state.get(name);
  }

  private static final class Node {
    final Map<String, String> state;
    final int depth;

    Node(Map<String, String> state, int depth) {
      this.state = state;
      this.depth = depth;
    }
  }
}
