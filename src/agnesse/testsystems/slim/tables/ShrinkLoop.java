package agnesse.testsystems.slim.tables;

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
      List<Value> candidates = shrinker.candidates(column.generator, values.get(column.index));
      if (!candidates.isEmpty()) {
        pendingColumn = column.index;
        return candidates;
      }
      focus++;
    }
    return List.of();
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
