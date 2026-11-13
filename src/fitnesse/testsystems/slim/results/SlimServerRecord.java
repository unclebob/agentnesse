package fitnesse.testsystems.slim.results;

/**
 * The Slim server for one test system. A launch failure lives here, not on a
 * table cell. Phase 7 will put this in the per-server section.
 */
public final class SlimServerRecord {
  private final SlimFailure failure;

  private SlimServerRecord(SlimFailure failure) {
    this.failure = failure;
  }

  public static SlimServerRecord started() {
    return new SlimServerRecord(null);
  }

  public static SlimServerRecord stopped(Throwable cause) {
    if (cause == null) {
      return started();
    }
    SlimFailure parsed = SlimFailure.parse(cause.toString());
    if (parsed.kind() != SlimFailure.Kind.LAUNCH_FAILURE) {
      return started();
    }
    return new SlimServerRecord(parsed);
  }

  public SlimFailure failure() {
    return failure;
  }

  public String dump() {
    if (failure == null) {
      return "{\"status\":\"started\"}";
    }
    return "{\"status\":\"launch-failure\",\"error\":" + failure.dump() + "}";
  }
}
