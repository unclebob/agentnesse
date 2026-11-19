package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class OneOfGen extends Generator {
  private final List<String> choices;
  private final int[] weights;
  private final long total;

  OneOfGen(List<String> choices) {
    this(choices, null);
  }

  OneOfGen(List<String> choices, List<Integer> weights) {
    this.choices = List.copyOf(choices);
    if (weights == null) {
      this.weights = null;
      this.total = 0;
    } else {
      this.weights = copy(weights);
      this.total = total(this.weights);
    }
  }

  List<String> choices() {
    return choices;
  }

  List<Integer> weights() {
    if (weights == null) {
      return List.of();
    }
    List<Integer> out = new ArrayList<>();
    for (int weight : weights) {
      out.add(weight);
    }
    return List.copyOf(out);
  }

  @Override
  public Value draw(Random random) {
    if (weights == null) {
      return new TextValue(choices.get(random.nextInt(choices.size())));
    }
    return new TextValue(choices.get(weightedIndex(random)));
  }

  private int weightedIndex(Random random) {
    long roll = random.nextLong(total);
    long cursor = 0;
    int chosen = weights.length - 1;
    for (int i = 0; i < weights.length - 1; i++) {
      cursor += weights[i];
      if (roll < cursor) {
        chosen = i;
        break;
      }
    }
    return chosen;
  }

  private static int[] copy(List<Integer> weights) {
    int[] out = new int[weights.size()];
    for (int i = 0; i < weights.size(); i++) {
      int weight = weights.get(i);
      if (weight < 1) {
        throw new IllegalArgumentException("weight must be at least 1");
      }
      out[i] = weight;
    }
    return out;
  }

  private static long total(int[] weights) {
    long sum = 0;
    for (int weight : weights) {
      sum += weight;
    }
    return sum;
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
