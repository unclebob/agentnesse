package agnesse.testsystems.slim.sequence;

/**
 * The candidate set after one command, or why that command matched nothing.
 * {@code shown} is the command row to paint.
 */
public final class Advance {
  public enum Kind { OK, CELL, GUARD, EMPTY }

  public final Kind kind;
  public final Candidates candidates;
  public final int shown;
  public final int column;
  public final String actual;
  public final String expected;
  public final int guard;
  public final String name;
  public final boolean model;
  public final String detail;

  private Advance(Kind kind, Candidates candidates, int shown, int column, String actual, String expected,
                  int guard, String name, boolean model, String detail) {
    this.kind = kind;
    this.candidates = candidates;
    this.shown = shown;
    this.column = column;
    this.actual = actual;
    this.expected = expected;
    this.guard = guard;
    this.name = name;
    this.model = model;
    this.detail = detail;
  }

  public static Advance ok(Candidates candidates, int shown) {
    return new Advance(Kind.OK, candidates, shown, -1, null, null, -1, "", false, "");
  }

  public static Advance cell(int shown, int column, String actual, String expected) {
    return new Advance(Kind.CELL, null, shown, column, actual, expected, -1, "", false, "");
  }

  public static Advance empty(int shown, int column, String actual, String expected, String detail) {
    return new Advance(Kind.EMPTY, null, shown, column, actual, expected, -1, "", false, detail);
  }

  public static Advance guard(int shown, int guard, String name, String actual, String expected, boolean model) {
    return new Advance(Kind.GUARD, null, shown, model ? 2 : 4, actual, expected, guard, name, model, "");
  }

  public boolean ok() {
    return kind == Kind.OK;
  }
}
