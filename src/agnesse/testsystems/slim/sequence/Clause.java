package agnesse.testsystems.slim.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One comparison against the model. {@code name=value} and {@code name!=value}
 * are fixed. A model clause may also name another variable with {@code @}.
 */
public final class Clause {
  public final String name;
  final String value;
  final boolean equal;
  final boolean reference;

  private Clause(String name, String value, boolean equal, boolean reference) {
    this.name = name;
    this.value = value;
    this.equal = equal;
    this.reference = reference;
  }

  /** Preconditions. The right-hand side is a literal. */
  public static List<Clause> when(String text) {
    return parsed(text, false);
  }

  /** Invariant clauses. The right-hand side may be {@code @name}. */
  public static List<Clause> model(String text) {
    return parsed(text, true);
  }

  public boolean holds(Map<String, String> state) {
    return holds(state, Map.of());
  }

  public boolean holds(Map<String, String> state, Map<String, String> args) {
    String actual = state.get(name);
    String expected = expected(state, args);
    if (actual == null || expected == null) {
      return false;
    }
    return equal == actual.equals(expected);
  }

  /** Every clause holds. An empty list holds. */
  public static boolean holds(List<Clause> clauses, Map<String, String> state) {
    for (Clause clause : clauses) {
      if (!clause.holds(state)) {
        return false;
      }
    }
    return true;
  }

  public String expected(Map<String, String> state, Map<String, String> args) {
    if (!reference) {
      return value;
    }
    if (args.containsKey(value)) {
      return args.get(value);
    }
    return state.get(value);
  }

  static boolean name(String text) {
    return text.matches("[A-Za-z][A-Za-z0-9_]*");
  }

  /** Empty is allowed. A missing name is rejected by the caller, not here. */
  public static boolean spelled(String name) {
    return name.matches("|[A-Za-z][A-Za-z0-9_]*");
  }

  private static List<Clause> parsed(String text, boolean allowReference) {
    String body = text == null ? "" : text.trim();
    if (body.isEmpty()) {
      return List.of();
    }
    List<Clause> clauses = new ArrayList<>();
    for (String piece : body.split(";", -1)) {
      clauses.add(one(piece, allowReference));
    }
    contradict(clauses, body);
    return List.copyOf(clauses);
  }

  private static Clause one(String piece, boolean allowReference) {
    String text = piece.trim();
    boolean unequal = text.contains("!=");
    String[] parts = unequal ? splitUnequal(text) : splitEqual(text);
    String name = parts[0].trim();
    String raw = parts[1].trim();
    if (!name(name) || raw.isEmpty() || raw.indexOf(' ') != -1) {
      throw new IllegalArgumentException("state must be name=value");
    }
    if (raw.startsWith("@")) {
      return reference(name, raw, !unequal, allowReference);
    }
    return new Clause(name, raw, !unequal, false);
  }

  private static Clause reference(String name, String raw, boolean equal, boolean allowReference) {
    if (!allowReference) {
      throw new IllegalArgumentException("a precondition cannot name a drawn value");
    }
    String other = raw.substring(1);
    if (!name(other)) {
      throw new IllegalArgumentException("unknown reference " + other);
    }
    return new Clause(name, other, equal, true);
  }

  private static String[] splitEqual(String text) {
    String[] parts = text.split("=", -1);
    if (parts.length != 2) {
      throw new IllegalArgumentException("state must be name=value");
    }
    return parts;
  }

  private static String[] splitUnequal(String text) {
    int mark = text.indexOf("!=");
    if (text.indexOf("!=", mark + 1) != -1) {
      throw new IllegalArgumentException("state must be name=value");
    }
    return new String[] {text.substring(0, mark), text.substring(mark + 2)};
  }

  private static void contradict(List<Clause> clauses, String text) {
    for (int i = 0; i < clauses.size(); i++) {
      Clause left = clauses.get(i);
      int j = i;
      j++;
      for (; j < clauses.size(); j++) {
        if (clash(left, clauses.get(j))) {
          throw new IllegalArgumentException("unsatisfiable precondition: " + text);
        }
      }
    }
  }

  private static boolean clash(Clause left, Clause right) {
    if (!left.name.equals(right.name) || left.reference || right.reference) {
      return false;
    }
    if (left.equal && right.equal) {
      return !left.value.equals(right.value);
    }
    if (left.equal != right.equal) {
      return left.value.equals(right.value);
    }
    return false;
  }
}
