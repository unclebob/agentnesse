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
    this.whenText = whenText == null ? "" : whenText.trim();
    this.modelText = modelText == null ? "" : modelText.trim();
    this.query = query == null || query.isBlank() ? null : query.trim();
    this.expect = expect == null ? "" : expect.trim();
    this.when = Clause.when(this.whenText);
    this.model = Clause.model(this.modelText);
    if (this.model.isEmpty() && this.query == null) {
      throw new IllegalArgumentException("an invariant needs a model or a query");
    }
    if (this.query != null && this.expect.isEmpty()) {
      throw new IllegalArgumentException("a check needs an expected value");
    }
  }
}
