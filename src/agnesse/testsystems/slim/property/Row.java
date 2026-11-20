package agnesse.testsystems.slim.property;

import java.util.LinkedHashMap;
import java.util.Map;

/** Values already drawn for earlier columns, or earlier fields of a record. */
public final class Row {
  public static final Row EMPTY = new Row(Map.of());

  private final Map<String, Value> values;

  private Row(Map<String, Value> values) {
    this.values = values;
  }

  public Row with(String name, Value value) {
    Map<String, Value> copy = new LinkedHashMap<>(values);
    copy.put(name, value);
    return new Row(Map.copyOf(copy));
  }

  Value required(String name) {
    Value value = values.get(name);
    if (value == null) {
      throw new IllegalArgumentException(name + " is not drawn");
    }
    return value;
  }
}
