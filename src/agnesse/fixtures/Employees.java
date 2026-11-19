package agnesse.fixtures;

import java.util.List;

/** The one row the WithSetUp sample queries. */
public class Employees {
  public List<List<List<String>>> query() {
    return List.of(List.of(
      List.of("name", "Bob"),
      List.of("hired", "10-Oct-1974")));
  }
}
