package agnesse.testsystems.slim.tables;

import agnesse.testsystems.slim.property.Row;
import agnesse.testsystems.slim.property.Shrinker;
import agnesse.testsystems.slim.property.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * One shrink round is every simpler value of one input, most aggressive first.
 * The first of those that still fails is kept. Work stops at {@link #CAP}.
 */
final class ShrinkLoop {
  static final int CAP = 32;

  private final List<PropertySheet.Column> inputs;
  final List<String> cells;
  private final List<Value> values;
  private final Shrinker shrinker = new Shrinker();
  private int focus;
  private int steps;
  private int pendingColumn = -1;
  private boolean capped;

  private ShrinkLoop(List<PropertySheet.Column> inputs, List<String> cells, List<Value> values) {
    this.inputs = inputs;
    this.cells = cells;
    this.values = values;
  }

  static ShrinkLoop start(PropertySheet sheet, List<String> cells, List<Value> values) {
    List<PropertySheet.Column> inputs = new ArrayList<>();
    for (PropertySheet.Column column : sheet.columns) {
      if (column.kind == PropertySheet.Kind.INPUT) {
        inputs.add(column);
      }
    }
    return new ShrinkLoop(inputs, cells, values);
  }

  boolean capped() {
    return capped;
  }

  int steps() {
    return steps;
  }

  int column() {
    return pendingColumn;
  }

  /** The next round, or none when shrinking is finished. */
  List<Value> nextRound() {
    if (steps >= CAP) {
      capped = true;
      return List.of();
    }
    while (focus < inputs.size()) {
      PropertySheet.Column column = inputs.get(focus);
      List<Value> candidates = legal(column);
      if (!candidates.isEmpty()) {
        pendingColumn = column.index;
        return candidates;
      }
      focus++;
    }
    return List.of();
  }

  private List<Value> legal(PropertySheet.Column column) {
    Row row = row();
    List<Value> out = new ArrayList<>();
    for (Value candidate : shrinker.candidates(column.generator, values.get(column.index), row)) {
      if (keeps(column, candidate, row)) {
        out.add(candidate);
      }
    }
    return out;
  }

  private boolean keeps(PropertySheet.Column changed, Value candidate, Row row) {
    Row trial = row.with(changed.name, candidate);
    for (PropertySheet.Column input : inputs) {
      Value value = input.index == changed.index ? candidate : values.get(input.index);
      if (!input.generator.holds(value, trial)) {
        return false;
      }
    }
    return true;
  }

  private Row row() {
    Row row = Row.EMPTY;
    for (PropertySheet.Column column : inputs) {
      row = row.with(column.name, values.get(column.index));
    }
    return row;
  }

  void accept(Value chosen) {
    if (chosen == null) {
      focus++;
      return;
    }
    values.set(pendingColumn, chosen);
    cells.set(pendingColumn, chosen.cell());
    steps++;
    focus = 0;
  }
}
