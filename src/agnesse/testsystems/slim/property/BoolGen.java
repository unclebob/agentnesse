package agnesse.testsystems.slim.property;

import java.util.List;
import java.util.Random;

final class BoolGen extends Generator {
  @Override
  public Value draw(Random random) {
    return new TextValue(Boolean.toString(random.nextBoolean()));
  }

  @Override
  List<Value> simpler(Value value) {
    if (!"true".equals(value.cell())) {
      return List.of();
    }
    return List.of(new TextValue("false"));
  }
}
