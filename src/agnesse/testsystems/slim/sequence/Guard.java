package agnesse.testsystems.slim.sequence;

import java.util.List;

/** An invariant checked after every command whose {@code when} matches. */
public final class Guard {
  public final String whenText;
  public final String modelText;
  public final String query;
  public final String expect;
  public final List<Clause> when;
  public final List<Clause> model;

  public Guard(String whenText, String modelText, String query, String expect) {
    this.whenText = text(whenText);
    this.modelText = text(modelText);
    this.query = name(query);
    this.expect = text(expect);
    this.when = Clause.when(this.whenText);
    this.model = Clause.model(this.modelText);
    present(this.model, this.query, this.expect);
  }

  private static String text(String value) {
    if (value == null) {
      return "";
    }
    return value.trim();
  }

  private static String name(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static void present(List<Clause> model, String query, String expect) {
    if (model.isEmpty() && query == null) {
      throw new IllegalArgumentException("an invariant needs a model or a query");
    }
    if (query != null && expect.isEmpty()) {
      throw new IllegalArgumentException("a check needs an expected value");
    }
  }
}
