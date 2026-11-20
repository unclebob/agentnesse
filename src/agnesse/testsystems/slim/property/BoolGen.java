package agnesse.testsystems.slim.property;

import java.util.List;
import java.util.Random;

final class BoolGen extends Generator {
  @Override
  String family() {
    return "boolean";
  }

  @Override
  int variety() {
    return 2;
  }
  @Override
  public Value draw(Random random, Row row) {
    return new TextValue(Boolean.toString(random.nextBoolean()));
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    if (!"true".equals(value.cell())) {
      return List.of();
    }
    return List.of(new TextValue("false"));
  }
}
