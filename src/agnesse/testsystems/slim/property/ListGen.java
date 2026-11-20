package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class ListGen extends Generator {
  private final Generator element;
  private final Span size;
  private final boolean unique;
  private final boolean sorted;

  ListGen(Generator element, Span size, boolean unique, boolean sorted) {
    if (unique && element.variety() >= 0 && size.min > element.variety()) {
      throw new IllegalArgumentException(
        "unique list needs " + size.min + " values but " + element.family() + " has " + element.variety());
    }
    this.element = element;
    this.size = size;
    this.unique = unique;
    this.sorted = sorted;
  }

  Generator element() {
    return element;
  }

  int shortest() {
    return size.min;
  }

  int longest() {
    return size.max;
  }

  @Override public boolean timed() { return element.timed(); }

  @Override
  String family() {
    return "list";
  }

  @Override
  void link(Scope scope) {
    element.link(scope);
  }

  @Override
  public Value draw(Random random, Row row) {
    int count = size.draw(random);
    List<Value> items = new ArrayList<>();
    if (unique) {
      drawUnique(random, row, count, items);
    } else {
      for (int i = 0; i < count; i++) {
        items.add(element.draw(random, row));
      }
    }
    if (sorted) {
      items.sort((left, right) -> element.compare(left, right));
    }
    return new ListValue(items);
  }

  private void drawUnique(Random random, Row row, int count, List<Value> items) {
    Set<String> seen = new HashSet<>();
    long limit = (long) count * 32L;
    long spins = 0;
    while (items.size() < count) {
      if (spins == limit) {
        throw new IllegalArgumentException("unique list ran out of values");
      }
      spins++;
      Value picked = element.draw(random, row);
      if (seen.add(picked.cell())) {
        items.add(picked);
      }
    }
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    ListValue items = (ListValue) value;
    List<Value> out = new ArrayList<>();
    for (int length : Numbers.shorter(items.items().size(), size.min)) {
      addIfHolds(out, items.prefix(length), row);
    }
    for (int i = 0; i < items.items().size(); i++) {
      for (Value candidate : element.simpler(items.items().get(i), row)) {
        addIfHolds(out, items.replace(i, candidate), row);
      }
    }
    return out;
  }

  private void addIfHolds(List<Value> out, Value candidate, Row row) {
    if (holds(candidate, row)) {
      out.add(candidate);
    }
  }

  @Override
  public boolean holds(Value value, Row row) {
    List<Value> items = ((ListValue) value).items();
    if (!eachHolds(items, row) || (unique && !distinct(items))) {
      return false;
    }
    return !sorted || ordered(items);
  }

  private boolean eachHolds(List<Value> items, Row row) {
    for (Value item : items) {
      if (!element.holds(item, row)) {
        return false;
      }
    }
    return true;
  }

  private static boolean distinct(List<Value> items) {
    Set<String> seen = new HashSet<>();
    for (Value item : items) {
      if (!seen.add(item.cell())) {
        return false;
      }
    }
    return true;
  }

  private boolean ordered(List<Value> items) {
    for (int i = 1; i < items.size(); i++) {
      int cmp = element.compare(items.get(i - 1), items.get(i));
      if (unique ? cmp >= 0 : cmp > 0) {
        return false;
      }
    }
    return true;
  }
}
