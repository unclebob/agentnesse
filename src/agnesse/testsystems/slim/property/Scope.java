package agnesse.testsystems.slim.property;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Earlier names a reference may use, and the reason a later or repeated name is refused. */
final class Scope {
  private final Map<String, Generator> earlier;
  private final Map<String, String> problems;
  private final Map<String, String> duplicates;

  private Scope(Map<String, Generator> earlier, Map<String, String> problems, Map<String, String> duplicates) {
    this.earlier = earlier;
    this.problems = problems;
    this.duplicates = duplicates;
  }

  static Scope columns(List<String> names, List<Generator> generators, int index) {
    Map<String, Generator> earlier = new LinkedHashMap<>();
    Map<String, String> problems = new LinkedHashMap<>();
    String self = names.get(index);
    problems.put(self, "cycle in column " + self);
    for (int i = 0; i < names.size(); i++) {
      if (i < index) {
        earlier.put(names.get(i), generators.get(i));
      } else if (i > index) {
        problems.put(names.get(i), self + " refers to later column " + names.get(i));
      }
    }
    return new Scope(earlier, problems, duplicates(names, "column"));
  }

  Scope fields(List<String> names, List<Generator> generators, int index) {
    Map<String, Generator> earlierFields = new LinkedHashMap<>(earlier);
    Map<String, String> problems = new LinkedHashMap<>();
    String self = names.get(index);
    earlierFields.remove(self);
    problems.put(self, "cycle in field " + self);
    for (int i = 0; i < names.size(); i++) {
      if (i < index) {
        earlierFields.put(names.get(i), generators.get(i));
      } else if (i > index) {
        problems.put(names.get(i), self + " refers to later field " + names.get(i));
      }
    }
    for (Map.Entry<String, String> column : this.problems.entrySet()) {
      problems.putIfAbsent(column.getKey(), column.getValue());
    }
    Map<String, String> dupes = new LinkedHashMap<>(duplicates);
    dupes.putAll(duplicates(names, "field"));
    return new Scope(earlierFields, problems, dupes);
  }

  void bind(String name, String family) {
    String duplicate = duplicates.get(name);
    if (duplicate != null) {
      throw new IllegalArgumentException(duplicate);
    }
    Generator found = earlier.get(name);
    if (found != null) {
      sameFamily(name, family, found.family());
      return;
    }
    String problem = problems.get(name);
    if (problem != null) {
      throw new IllegalArgumentException(problem);
    }
    throw new IllegalArgumentException(name + " is not a column");
  }

  private static void sameFamily(String name, String wanted, String found) {
    if (!wanted.equals(found)) {
      throw new IllegalArgumentException(name + " is " + found + ", not " + wanted);
    }
  }

  private static Map<String, String> duplicates(List<String> names, String kind) {
    Set<String> seen = new HashSet<>();
    Map<String, String> duplicates = new LinkedHashMap<>();
    for (String name : names) {
      if (!seen.add(name)) {
        duplicates.put(name, name + " is already a " + kind);
      }
    }
    return duplicates;
  }
}
