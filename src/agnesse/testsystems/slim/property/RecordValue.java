package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;

/** A generated object. The cell is compact JSON in field order. */
record RecordValue(List<String> names, List<Value> fields) implements Value {
  RecordValue {
    names = List.copyOf(names);
    fields = List.copyOf(fields);
  }

  @Override
  public String cell() {
    return json();
  }

  @Override
  public String json() {
    return JsonText.object(names, fields);
  }

  RecordValue replace(int index, Value candidate) {
    List<Value> copy = new ArrayList<>(fields);
    copy.set(index, candidate);
    return new RecordValue(names, copy);
  }
}
