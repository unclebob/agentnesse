package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class StringGen extends Generator {
  @Override
  String family() {
    return "string";
  }
  /** Printable ASCII, without {@code |}, so a draw can sit in a markdown cell. */
  static final String ASCII = ascii();

  private final Span length;
  private final boolean adversarial;

  StringGen(Span length, boolean adversarial) {
    this.length = length;
    this.adversarial = adversarial;
  }

  boolean adversarial() {
    return adversarial;
  }

  int shortest() {
    return length.min;
  }

  int longest() {
    return length.max;
  }

  @Override
  public Value draw(Random random, Row row) {
    if (adversarial && random.nextBoolean()) {
      List<String> catalog = Adversarial.fitting(length);
      if (!catalog.isEmpty()) {
        return new TextValue(catalog.get(random.nextInt(catalog.size())));
      }
    }
    return ordinary(random);
  }

  private Value ordinary(Random random) {
    int count = length.draw(random);
    StringBuilder text = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      text.append(ASCII.charAt(random.nextInt(ASCII.length())));
    }
    return new TextValue(text.toString());
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    String text = ((TextValue) value).text();
    List<Value> out = new ArrayList<>();
    for (int size : Numbers.shorter(text.length(), length.min)) {
      if (!cutsSurrogate(text, size)) {
        out.add(new TextValue(text.substring(0, size)));
      }
    }
    return out;
  }

  private static boolean cutsSurrogate(String text, int size) {
    return size > 0 && Character.isHighSurrogate(text.charAt(size - 1));
  }

  private static String ascii() {
    StringBuilder out = new StringBuilder();
    for (int c = 32; c <= 126; c++) {
      if (c != '|') {
        out.append((char) c);
      }
    }
    return out.toString();
  }
}
