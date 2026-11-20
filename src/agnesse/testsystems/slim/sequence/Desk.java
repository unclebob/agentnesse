package agnesse.testsystems.slim.sequence;

import java.util.List;

/**
 * One fresh fixture. Lane 0 creates it. Later lanes call the same instance,
 * and calls on different lanes may overlap.
 */
public interface Desk {
  void make(String instance, String fixture, String[] arguments);

  String call(int lane, String instance, String method, List<String> arguments);
}
