package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A fixed identifier shape: literal text and runs of one character class.
 * Not a regular expression.
 */
final class PatternGen extends Generator {
  @Override
  String family() {
    return "pattern";
  }
  static final int MAX_LENGTH = 64;
  static final int MAX_RUN = 32;

  private final List<Slot> slots;

  PatternGen(List<Piece> pieces) {
    if (pieces.isEmpty()) {
      throw new IllegalArgumentException("expected a pattern");
    }
    this.slots = slots(pieces);
  }

  @Override
  public Value draw(Random random, Row row) {
    StringBuilder out = new StringBuilder(slots.size());
    for (Slot slot : slots) {
      out.append(slot.draw(random));
    }
    return new TextValue(out.toString());
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    char[] current = matched(value.cell());
    List<Value> out = new ArrayList<>();
    offer(out, current, base());
    for (int pos = 0; pos < slots.size(); pos++) {
      Slot slot = slots.get(pos);
      if (slot.alphabet == null) {
        continue;
      }
      int index = slot.alphabet.indexOf(current[pos]);
      for (int closer : Numbers.closerToZero(index)) {
        char[] copy = current.clone();
        copy[pos] = slot.alphabet.charAt(closer);
        offer(out, current, copy);
      }
    }
    return out;
  }

  private char[] matched(String text) {
    if (text.length() != slots.size()) {
      throw new IllegalArgumentException("not a match: " + text);
    }
    char[] chars = text.toCharArray();
    for (int pos = 0; pos < slots.size(); pos++) {
      if (!slots.get(pos).accepts(chars[pos])) {
        throw new IllegalArgumentException("not a match: " + text);
      }
    }
    return chars;
  }

  private char[] base() {
    char[] chars = new char[slots.size()];
    for (int pos = 0; pos < slots.size(); pos++) {
      chars[pos] = slots.get(pos).base();
    }
    return chars;
  }

  private static void offer(List<Value> out, char[] current, char[] candidate) {
    String rendered = new String(candidate);
    if (rendered.equals(new String(current)) || listed(out, rendered)) {
      return;
    }
    out.add(new TextValue(rendered));
  }

  private static boolean listed(List<Value> out, String rendered) {
    for (Value value : out) {
      if (value.cell().equals(rendered)) {
        return true;
      }
    }
    return false;
  }

  private static List<Slot> slots(List<Piece> pieces) {
    int total = 0;
    List<Slot> slots = new ArrayList<>();
    for (Piece piece : pieces) {
      total = Math.addExact(total, piece.count);
      if (total > MAX_LENGTH) {
        throw new IllegalArgumentException("pattern is longer than " + MAX_LENGTH);
      }
      piece.fill(slots);
    }
    return List.copyOf(slots);
  }

  static final class Piece {
    private final String literal;
    private final String alphabet;
    private final int count;

    private Piece(String literal, String alphabet, int count) {
      this.literal = literal;
      this.alphabet = alphabet;
      this.count = count;
    }

    static Piece literal(String text) {
      return new Piece(text, null, text.length());
    }

    static Piece clazz(String name, int count) {
      if (count < 1 || count > MAX_RUN) {
        throw new IllegalArgumentException("a class run must be 1 through " + MAX_RUN);
      }
      return new Piece(null, alphabet(name), count);
    }

    private void fill(List<Slot> slots) {
      if (alphabet == null) {
        for (int i = 0; i < literal.length(); i++) {
          slots.add(new Slot(literal.charAt(i), null));
        }
        return;
      }
      for (int i = 0; i < count; i++) {
        slots.add(new Slot(alphabet.charAt(0), alphabet));
      }
    }

    private static String alphabet(String name) {
      if ("[0-9]".equals(name)) {
        return "0123456789";
      }
      if ("[A-Z]".equals(name)) {
        return "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
      }
      if ("[a-z]".equals(name)) {
        return "abcdefghijklmnopqrstuvwxyz";
      }
      throw new IllegalArgumentException("unknown character class " + name);
    }
  }

  private static final class Slot {
    private final char fixed;
    private final String alphabet;

    private Slot(char fixed, String alphabet) {
      this.fixed = fixed;
      this.alphabet = alphabet;
    }

    private char draw(Random random) {
      if (alphabet == null) {
        return fixed;
      }
      return alphabet.charAt(random.nextInt(alphabet.length()));
    }

    private char base() {
      return fixed;
    }

    private boolean accepts(char c) {
      return alphabet == null ? c == fixed : alphabet.indexOf(c) >= 0;
    }
  }
}
