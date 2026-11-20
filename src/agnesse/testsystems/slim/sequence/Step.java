package agnesse.testsystems.slim.sequence;

import agnesse.testsystems.slim.property.Value;

import java.util.ArrayList;
import java.util.List;

/** One drawn command. A null value is an argument this command omits. */
public final class Step {
  public final int command;
  private final List<Value> values;

  public Step(int command, List<Value> values) {
    this.command = command;
    this.values = new ArrayList<>(values);
  }

  public Value value(int index) {
    return values.get(index);
  }

  public int size() {
    return values.size();
  }

  public List<Value> values() {
    return new ArrayList<>(values);
  }
}
