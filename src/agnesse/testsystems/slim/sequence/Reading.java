package agnesse.testsystems.slim.sequence;

import java.util.Map;

/** What one command returned, and what its queries returned. */
public record Reading(String returned, Map<String, String> queries, long started, long finished) {
  public Reading {
    queries = queries == null ? Map.of() : Map.copyOf(queries);
  }

  public Reading(String returned, Map<String, String> queries) {
    this(returned, queries, 0L, 0L);
  }

  public static Reading returned(String value) {
    return new Reading(value, Map.of());
  }
}
