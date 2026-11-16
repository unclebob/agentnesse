package fitnesse.testrunner;

import fitnesse.wiki.WikiPage;

public class WikiPageIdentity {

  public static final String COMMAND_PATTERN = "COMMAND_PATTERN";
  public static final String TEST_RUNNER = "TEST_RUNNER";
  public static final String TEST_SYSTEM = "TEST_SYSTEM";
  private static final String[] LAUNCH = {
    TEST_RUNNER,
    COMMAND_PATTERN,
    "SLIM_PORT",
    "SLIM_HOST",
    "SLIM_FLAGS",
    "SLIM_VERSION",
    "MANUALLY_START_TEST_RUNNER",
    "MANUALLY_START_TEST_RUNNER_ON_DEBUG",
    "REMOTE_DEBUG_COMMAND",
    "REMOTE_DEBUG_RUNNER",
    "slim.port",
    "slim.host",
    "slim.flags",
    "slim.timeout",
    "slim.pool.size"
  };
  private final WikiPage page;
  private final String launch;

  public WikiPageIdentity(WikiPage page) {
    this.page = page;
    rejectTestSystem(page);
    launch = launchKey(page);
  }

  public static void rejectTestSystem(WikiPage page) {
    if (page.getVariable(TEST_SYSTEM) != null) {
      throw new IllegalArgumentException(page.getName() + " defines TEST_SYSTEM. Slim is the only test system.");
    }
  }

  public String getVariable(String name) {
    return page.getVariable(name);
  }

  public String testSystem() {
    return "slim";
  }

  private static String launchKey(WikiPage page) {
    StringBuilder key = new StringBuilder();
    for (String name : LAUNCH) {
      String value = page.getVariable(name);
      key.append(name).append('=').append(value == null ? "" : value).append('\n');
    }
    return key.toString();
  }

  @Override
  public int hashCode() {
    return launch.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;

    WikiPageIdentity identity = (WikiPageIdentity) obj;
    return identity.launch.equals(launch);
  }
}
