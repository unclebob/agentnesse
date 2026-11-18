package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;

/** Candidates that move an int toward zero, or a length down toward a floor. */
final class Numbers {
  private Numbers() {
  }

  static List<Integer> closerToZero(int current) {
    List<Integer> out = new ArrayList<>(3);
    addCloser(out, current, 0);
    addCloser(out, current, current / 2);
    addCloser(out, current, current - Integer.signum(current));
    return out;
  }

  private static void addCloser(List<Integer> out, int current, int candidate) {
    if (Math.abs((long) candidate) >= Math.abs((long) current)) {
      return;
    }
    if (out.contains(candidate)) {
      return;
    }
    out.add(candidate);
  }

  static List<Integer> shorter(int length, int floor) {
    List<Integer> out = new ArrayList<>(3);
    addShorter(out, length, floor, floor);
    addShorter(out, length, floor, Math.max(floor, length / 2));
    addShorter(out, length, floor, length - 1);
    return out;
  }

  private static void addShorter(List<Integer> out, int length, int floor, int candidate) {
    if (candidate < floor || candidate >= length || out.contains(candidate)) {
      return;
    }
    out.add(candidate);
  }
}
