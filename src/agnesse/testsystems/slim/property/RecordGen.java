package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Nested fields drawn left to right. A field may refer to an earlier field or column. */
final class RecordGen extends Generator {
  private final List<String> names;
  private final List<Generator> fields;

  RecordGen(List<String> names, List<Generator> fields) {
    this.names = List.copyOf(names);
    this.fields = List.copyOf(fields);
  }

  @Override
  String family() {
    return "record";
  }

  @Override
  public boolean timed() {
    for (Generator field : fields) {
      if (field.timed()) {
        return true;
      }
    }
    return false;
  }

  @Override
  void link(Scope scope) {
    for (int i = 0; i < names.size(); i++) {
      fields.get(i).link(scope.fields(names, fields, i));
    }
  }

  @Override
  public Value draw(Random random, Row row) {
    Row cursor = row;
    List<Value> values = new ArrayList<>();
    for (int i = 0; i < fields.size(); i++) {
      Value value = fields.get(i).draw(random, cursor);
      values.add(value);
      cursor = cursor.with(names.get(i), value);
    }
    return new RecordValue(names, values);
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    RecordValue record = (RecordValue) value;
    List<Value> out = new ArrayList<>();
    Row cursor = row;
    for (int i = 0; i < fields.size(); i++) {
      for (Value candidate : fields.get(i).simpler(record.fields().get(i), cursor)) {
        RecordValue next = record.replace(i, candidate);
        if (holds(next, row)) {
          out.add(next);
        }
      }
      cursor = cursor.with(names.get(i), record.fields().get(i));
    }
    return out;
  }

  @Override
  public boolean holds(Value value, Row row) {
    if (!(value instanceof RecordValue record) || record.fields().size() != fields.size()) {
      return false;
    }
    Row cursor = row;
    for (int i = 0; i < fields.size(); i++) {
      if (!fields.get(i).holds(record.fields().get(i), cursor)) {
        return false;
      }
      cursor = cursor.with(names.get(i), record.fields().get(i));
    }
    return true;
  }
}
