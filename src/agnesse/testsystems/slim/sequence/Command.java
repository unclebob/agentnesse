package agnesse.testsystems.slim.sequence;

import agnesse.testsystems.slim.property.Generator;
import agnesse.testsystems.slim.property.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One command the sequence may draw. Blank arguments are omitted from the call. */
public final class Command {
  public final String name;
  public final String whenText;
  public final String nextText;
  public final List<Clause> when;
  public final List<Binding> next;
  public final List<Argument> arguments;
  public final List<Expect> expects;

  public Command(String name, String whenText, String nextText, List<Argument> arguments, List<Expect> expects) {
    this.name = name;
    this.whenText = whenText == null ? "" : whenText.trim();
    this.nextText = nextText == null ? "" : nextText.trim();
    this.when = Clause.when(this.whenText);
    this.next = Binding.list(this.nextText);
    this.arguments = List.copyOf(arguments);
    this.expects = List.copyOf(expects);
  }

  public Map<String, String> args(List<Value> values) {
    Map<String, String> args = new LinkedHashMap<>();
    for (int i = 0; i < arguments.size(); i++) {
      Value value = values.get(i);
      if (value != null) {
        args.put(arguments.get(i).name, value.cell());
      }
    }
    return args;
  }

  public void collectQueries(List<String> names) {
    for (Expect expect : expects) {
      if (!expect.result && !names.contains(expect.method)) {
        names.add(expect.method);
      }
    }
  }

  public List<String> call(List<Value> values) {
    List<String> cells = new ArrayList<>();
    for (Value value : values) {
      if (value != null) {
        cells.add(value.cell());
      }
    }
    return cells;
  }

  public static final class Argument {
    public final String name;
    public final int column;
    public final Generator generator;
    public final String literal;

    public Argument(String name, int column, Generator generator, String literal) {
      this.name = name;
      this.column = column;
      this.generator = generator;
      this.literal = literal;
    }

    public boolean omitted() {
      return generator == null && literal == null;
    }
  }

  public static final class Expect {
    public final boolean result;
    public final String method;
    public final String template;
    public final int column;

    public Expect(boolean result, String method, String template, int column) {
      this.result = result;
      this.method = method;
      this.template = template;
      this.column = column;
    }
  }
}
