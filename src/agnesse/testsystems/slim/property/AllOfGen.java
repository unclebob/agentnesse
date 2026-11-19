package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Every case of a small domain, or of a cross product of domains.
 * The rightmost group varies fastest. This generator is enumerated, not sampled.
 */
final class AllOfGen extends Generator {
  private final List<List<String>> groups;
  private final List<Value> domain;

  AllOfGen(List<List<String>> groups) {
    if (groups.isEmpty()) {
      throw new IllegalArgumentException("empty choice");
    }
    this.groups = new ArrayList<>();
    long size = 1;
    for (List<String> group : groups) {
      List<String> unique = unique(group);
      size = Math.multiplyExact(size, unique.size());
      if (size > ENUMERATION_LIMIT) {
        throw new IllegalArgumentException("all of has " + size + " cases; the limit is " + ENUMERATION_LIMIT);
      }
      this.groups.add(unique);
    }
    this.domain = expand();
  }

  @Override
  public List<Value> domain() {
    return domain;
  }

  @Override
  public Value draw(Random random) {
    throw new IllegalStateException("all of is enumerated, not sampled");
  }

  @Override
  List<Value> simpler(Value value) {
    int[] index = indexes(value.cell());
    List<Value> out = new ArrayList<>();
    offer(out, index, new int[index.length]);
    for (int group = 0; group < index.length; group++) {
      for (int closer : Numbers.closerToZero(index[group])) {
        int[] copy = index.clone();
        copy[group] = closer;
        offer(out, index, copy);
      }
    }
    return out;
  }

  private int[] indexes(String cell) {
    String[] parts = groups.size() == 1 ? new String[] {cell} : cell.split(" ", -1);
    if (parts.length != groups.size()) {
      throw new IllegalArgumentException("not a case: " + cell);
    }
    int[] index = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      index[i] = groups.get(i).indexOf(parts[i]);
      if (index[i] < 0) {
        throw new IllegalArgumentException("not a case: " + cell);
      }
    }
    return index;
  }

  private void offer(List<Value> out, int[] current, int[] candidate) {
    if (same(current, candidate)) {
      return;
    }
    String rendered = text(candidate);
    for (Value value : out) {
      if (value.cell().equals(rendered)) {
        return;
      }
    }
    out.add(new TextValue(rendered));
  }

  private String text(int[] index) {
    String[] parts = new String[index.length];
    for (int i = 0; i < index.length; i++) {
      parts[i] = groups.get(i).get(index[i]);
    }
    return join(parts);
  }

  private List<Value> expand() {
    List<Value> out = new ArrayList<>();
    fill(out, 0, new String[groups.size()]);
    return List.copyOf(out);
  }

  private void fill(List<Value> out, int group, String[] parts) {
    if (group == groups.size()) {
      out.add(new TextValue(join(parts)));
      return;
    }
    for (String choice : groups.get(group)) {
      parts[group] = choice;
      fill(out, group + 1, parts);
    }
  }

  private static String join(String[] parts) {
    if (parts.length == 1) {
      return parts[0];
    }
    return String.join(" ", parts);
  }

  private static boolean same(int[] left, int[] right) {
    for (int i = 0; i < left.length; i++) {
      if (left[i] != right[i]) {
        return false;
      }
    }
    return true;
  }

  private static List<String> unique(List<String> group) {
    if (group.isEmpty()) {
      throw new IllegalArgumentException("empty choice");
    }
    Set<String> seen = new HashSet<>();
    List<String> copy = new ArrayList<>();
    for (String choice : group) {
      if (!seen.add(choice)) {
        throw new IllegalArgumentException("duplicate choice " + choice);
      }
      copy.add(choice);
    }
    return List.copyOf(copy);
  }
}
