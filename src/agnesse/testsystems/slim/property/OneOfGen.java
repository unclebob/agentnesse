package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class OneOfGen extends Generator {
  private final List<String> choices;

  OneOfGen(List<String> choices) {
    this.choices = List.copyOf(choices);
  }

  List<String> choices() {
    return choices;
  }

  @Override
  public Value draw(Random random) {
    return new TextValue(choices.get(random.nextInt(choices.size())));
  }

  @Override
  List<Value> simpler(Value value) {
    int index = choices.indexOf(value.cell());
    if (index < 0) {
      throw new IllegalArgumentException("not a choice: " + value.cell());
    }
    List<Value> out = new ArrayList<>();
    for (int candidate : Numbers.closerToZero(index)) {
      out.add(new TextValue(choices.get(candidate)));
    }
    return out;
  }
}
