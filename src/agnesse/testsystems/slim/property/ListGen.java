package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class ListGen extends Generator {
  private final Generator element;
  private final Span size;

  ListGen(Generator element, Span size) {
    this.element = element;
    this.size = size;
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

  @Override
  public boolean timed() {
    return element.timed();
  }

  @Override
  public Value draw(Random random) {
    int count = size.draw(random);
    List<Value> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      items.add(element.draw(random));
    }
    return new ListValue(items);
  }

  @Override
  List<Value> simpler(Value value) {
    ListValue items = (ListValue) value;
    List<Value> out = new ArrayList<>();
    for (int length : Numbers.shorter(items.items().size(), size.min)) {
      out.add(items.prefix(length));
    }
    for (int i = 0; i < items.items().size(); i++) {
      for (Value candidate : element.simpler(items.items().get(i))) {
        out.add(items.replace(i, candidate));
      }
    }
    return out;
  }
}
