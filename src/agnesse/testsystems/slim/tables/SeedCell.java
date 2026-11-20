package agnesse.testsystems.slim.tables;

/** One seed row. Blank means the run draws a seed. A second row is refused. */
final class SeedCell {
  private boolean seen;
  private Long explicit;
  private int row = -1;

  void read(String value, int at) throws SyntaxError {
    if (seen) {
      throw new SyntaxError("seed is already set");
    }
    seen = true;
    explicit = number(value);
    row = at;
  }

  boolean seen() {
    return seen;
  }

  Long explicit() {
    return explicit;
  }

  int row() {
    return row;
  }

  private static Long number(String text) throws SyntaxError {
    if (text.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(text.trim());
    } catch (NumberFormatException e) {
      throw new SyntaxError("seed must be a whole number");
    }
  }
}
