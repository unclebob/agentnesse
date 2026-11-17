package agnesse.wiki;

import java.util.List;
import java.util.Set;

/**
 * A page included into a test. It is not a test of its own.
 */
public final class TestAdjunct {
  public static final String TYPE = "test-adjunct";
  public static final String SET_UP = "SetUp";
  public static final String TEAR_DOWN = "TearDown";
  public static final String SUITE_SET_UP = "SuiteSetUp";
  public static final String SUITE_TEAR_DOWN = "SuiteTearDown";
  public static final String SCENARIO_LIBRARY = "ScenarioLibrary";

  public static final List<String> SET_UPS = List.of(SET_UP, SUITE_SET_UP);
  public static final List<String> TEAR_DOWNS = List.of(TEAR_DOWN, SUITE_TEAR_DOWN);
  public static final List<String> NAMES = List.of(SET_UP, TEAR_DOWN, SUITE_SET_UP, SUITE_TEAR_DOWN, SCENARIO_LIBRARY);

  private static final Set<String> NAME_SET = Set.copyOf(NAMES);

  private TestAdjunct() {
  }

  public static boolean named(String name) {
    return name != null && NAME_SET.contains(name);
  }

  public static boolean is(WikiPage page) {
    return page.getData().hasAttribute(TYPE);
  }

  public static String quotedNames() {
    return String.join(", ", NAMES.stream().map(name -> "'" + name + "'").toList());
  }
}
