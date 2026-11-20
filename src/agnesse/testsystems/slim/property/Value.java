package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;

/**
 * One generated input, already in the form a Slim fixture reads from a cell.
 * Lists use the same text Java's list converter accepts: {@code [1, 2]}.
 */
public sealed interface Value permits TextValue, ListValue, RecordValue {
  String cell();

  /** Compact JSON. Text is a JSON string. Lists and records stay structured. */
  String json();
}

record TextValue(String text) implements Value {
  @Override
  public String cell() {
    return text;
  }

  @Override
  public String json() {
    return JsonText.quote(text);
  }
}

record ListValue(List<Value> items) implements Value {
  ListValue {
    items = List.copyOf(items);
  }

  @Override
  public String cell() {
    StringBuilder out = new StringBuilder("[");
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) {
        out.append(", ");
      }
      out.append(items.get(i).cell());
    }
    out.append(']');
    return out.toString();
  }

  @Override
  public String json() {
    return JsonText.array(items);
  }

  ListValue replace(int index, Value candidate) {
    List<Value> copy = new ArrayList<>(items);
    copy.set(index, candidate);
    return new ListValue(copy);
  }

  ListValue prefix(int length) {
    return new ListValue(items.subList(0, length));
  }
}
