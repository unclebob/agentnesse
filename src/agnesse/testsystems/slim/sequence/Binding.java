package agnesse.testsystems.slim.sequence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One assignment in {@code next}. Later assignments in the same cell see
 * earlier ones. {@code @a+@b} and {@code @a-@b} are whole numbers.
 */
public final class Binding {
  public final String name;
  private final Expr expr;

  private Binding(String name, Expr expr) {
    this.name = name;
    this.expr = expr;
  }

  public static List<Binding> list(String text) {
    String body = text == null ? "" : text.trim();
    if (body.isEmpty()) {
      return List.of();
    }
    List<Binding> bindings = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    for (String piece : body.split(";", -1)) {
      Binding binding = one(piece);
      if (!seen.add(binding.name)) {
        throw new IllegalArgumentException(binding.name + " is already set");
      }
      bindings.add(binding);
    }
    return List.copyOf(bindings);
  }

  public boolean literal() {
    return expr instanceof Lit;
  }

  public Set<String> refs() {
    return expr.refs();
  }

  public String eval(Map<String, String> state, Map<String, String> args) {
    return expr.eval(state, args);
  }

  private static Binding one(String piece) {
    String text = piece.trim();
    int mark = text.indexOf('=');
    if (mark <= 0 || text.indexOf('=', mark + 1) >= 0) {
      throw new IllegalArgumentException("state must be name=value");
    }
    String name = text.substring(0, mark).trim();
    String raw = text.substring(mark + 1).trim();
    if (!Clause.name(name) || raw.isEmpty()) {
      throw new IllegalArgumentException("state must be name=value");
    }
    return new Binding(name, expr(raw));
  }

  private static Expr expr(String raw) {
    int plus = raw.indexOf('+');
    int minus = raw.indexOf('-');
    if (plus >= 0 && minus >= 0) {
      throw new IllegalArgumentException("state must be name=value");
    }
    if (plus >= 0) {
      return op(raw, plus, '+');
    }
    if (minus >= 0) {
      return op(raw, minus, '-');
    }
    if (raw.startsWith("@")) {
      return new Ref(ref(raw));
    }
    return new Lit(raw);
  }

  private static Expr op(String raw, int mark, char operator) {
    if (raw.indexOf(operator, mark + 1) >= 0 || raw.indexOf(' ') >= 0) {
      throw new IllegalArgumentException("state must be name=value");
    }
    return new Op(ref(raw.substring(0, mark)), operator, ref(raw.substring(mark + 1)));
  }

  private static String ref(String raw) {
    String text = raw.trim();
    if (!text.startsWith("@") || !Clause.name(text.substring(1))) {
      throw new IllegalArgumentException("unknown reference " + text);
    }
    return text.substring(1);
  }

  private sealed interface Expr permits Lit, Ref, Op {
    String eval(Map<String, String> state, Map<String, String> args);

    Set<String> refs();
  }

  private record Lit(String text) implements Expr {
    @Override
    public String eval(Map<String, String> state, Map<String, String> args) {
      return text;
    }

    @Override
    public Set<String> refs() {
      return Set.of();
    }
  }

  private record Ref(String name) implements Expr {
    @Override
    public String eval(Map<String, String> state, Map<String, String> args) {
      return lookup(name, state, args);
    }

    @Override
    public Set<String> refs() {
      return Set.of(name);
    }
  }

  private record Op(String left, char operator, String right) implements Expr {
    @Override
    public String eval(Map<String, String> state, Map<String, String> args) {
      long a = whole(left, lookup(left, state, args));
      long b = whole(right, lookup(right, state, args));
      long result = operator == '+' ? a + b : a - b;
      if (result != (int) result) {
        throw new IllegalArgumentException(left + " is not a whole number");
      }
      return Long.toString(result);
    }

    @Override
    public Set<String> refs() {
      Set<String> names = new LinkedHashSet<>();
      names.add(left);
      names.add(right);
      return names;
    }
  }

  private static String lookup(String name, Map<String, String> state, Map<String, String> args) {
    if (args.containsKey(name)) {
      return args.get(name);
    }
    if (state.containsKey(name)) {
      return state.get(name);
    }
    throw new IllegalArgumentException("unknown reference " + name);
  }

  private static long whole(String name, String text) {
    if (text == null || !text.equals(canonical(text))) {
      throw new IllegalArgumentException(name + " is not a whole number");
    }
    try {
      return Long.parseLong(text);
    } catch (NumberFormatException bad) {
      throw new IllegalArgumentException(name + " is not a whole number");
    }
  }

  private static String canonical(String text) {
    try {
      return Long.toString(Long.parseLong(text));
    } catch (NumberFormatException bad) {
      return "";
    }
  }
}
