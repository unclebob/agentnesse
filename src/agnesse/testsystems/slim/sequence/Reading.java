package agnesse.testsystems.slim.sequence;

import java.util.Map;

/** What one command returned, and what its queries returned. */
public record Reading(String returned, Map<String, String> queries) {
  public Reading {
    queries = queries == null ? Map.of() : Map.copyOf(queries);
  }

  public static Reading returned(String value) {
    return new Reading(value, Map.of());
  }
}
