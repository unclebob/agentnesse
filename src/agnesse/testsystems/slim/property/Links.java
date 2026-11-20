package agnesse.testsystems.slim.property;

import java.util.List;

/** Resolve column references once the whole row is known. */
public final class Links {
  Links() {
  }

  public static void bind(List<String> names, List<Generator> generators) {
    for (int i = 0; i < names.size(); i++) {
      generators.get(i).link(Scope.columns(names, generators, i));
    }
  }
}
