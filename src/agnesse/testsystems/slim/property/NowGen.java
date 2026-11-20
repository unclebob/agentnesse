package agnesse.testsystems.slim.property;

import java.util.List;
import java.util.Random;

/** The property's fixed clock, the same string on every trial. */
final class NowGen extends Generator {
  private final String text;

  NowGen(Clock clock) {
    this.text = clock.text();
  }

  @Override
  public boolean timed() {
    return true;
  }

  @Override
  String family() {
    return "now";
  }

  @Override
  public Value draw(Random random, Row row) {
    return new TextValue(text);
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    return List.of();
  }
}
