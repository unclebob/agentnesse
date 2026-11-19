package agnesse.slim.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A list, reversed twice, is the list. */
public class ListReversal {
  private List<Integer> items = List.of();

  public void setItems(List<Integer> items) {
    this.items = items == null ? List.of() : List.copyOf(items);
  }

  public boolean reversedTwice() {
    return reverse(reverse(items)).equals(items);
  }

  public boolean stillEmpty() {
    return items.isEmpty();
  }

  private static List<Integer> reverse(List<Integer> source) {
    List<Integer> copy = new ArrayList<>(source);
    Collections.reverse(copy);
    return copy;
  }
}
