package agnesse.testsystems.slim.sequence;

import java.util.ArrayList;
import java.util.List;

/** The readings from one run of a schedule, or why the run stopped. */
public final class History {
  public final List<Reading> prefix;
  public final List<List<Reading>> branches;
  public final String broken;

  private History(List<Reading> prefix, List<List<Reading>> branches, String broken) {
    this.prefix = prefix;
    this.branches = branches;
    this.broken = broken;
  }

  public static History ok(List<Reading> prefix, List<List<Reading>> branches) {
    List<List<Reading>> copy = new ArrayList<>();
    for (List<Reading> branch : branches) {
      copy.add(List.copyOf(branch));
    }
    return new History(List.copyOf(prefix), List.copyOf(copy), null);
  }

  public static History broke(String message) {
    String text = message == null || message.isBlank() ? "no result" : message;
    return new History(List.of(), List.of(), text);
  }

  public boolean ok() {
    return broken == null;
  }
}
