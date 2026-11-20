package agnesse.testsystems.slim.property;

import java.util.List;
import java.util.Random;

/**
 * A number or a time that stays strictly after or before an earlier column.
 * The static range is the inner generator. The live range intersects that
 * with the referenced value.
 */
final class Limit extends Generator {
  private final Generator inner;
  private final Relation relation;
  private final Narrow narrow;

  Limit(Generator inner, Relation relation) {
    this.inner = inner;
    this.relation = relation;
    this.narrow = narrow(inner);
  }

  @Override public boolean timed() { return inner.timed(); }

  @Override
  String family() {
    return narrow.family();
  }

  @Override
  void link(Scope scope) {
    scope.bind(relation.name(), narrow.family());
    inner.link(scope);
  }

  @Override
  public Value draw(Random random, Row row) {
    return live(row).draw(random, row);
  }

  @Override
  List<Value> simpler(Value value, Row row) {
    return live(row).simpler(value, row);
  }

  @Override
  public boolean holds(Value value, Row row) {
    long current = narrow.read(value.cell());
    long ref = narrow.read(row.required(relation.name()).cell());
    if (current < narrow.lowest() || current > narrow.highest()) {
      return false;
    }
    return relation.after() ? current > ref : current < ref;
  }

  private Generator live(Row row) {
    long ref = narrow.read(row.required(relation.name()).cell());
    Window window = relation.after() ? above(ref) : below(ref);
    return narrow.between(window.low, window.high);
  }

  private Window above(long ref) {
    if (ref >= narrow.highest()) {
      throw empty();
    }
    return new Window(Math.max(narrow.lowest(), ref + 1), narrow.highest());
  }

  private Window below(long ref) {
    if (ref <= narrow.lowest()) {
      throw empty();
    }
    return new Window(narrow.lowest(), Math.min(narrow.highest(), ref - 1));
  }

  private IllegalArgumentException empty() {
    return new IllegalArgumentException("no " + narrow.family() + " is " + relation.word() + " " + relation.name());
  }

  private static Narrow narrow(Generator generator) {
    if (generator instanceof IntGen ints) {
      return new Ints(ints);
    }
    if (generator instanceof DecimalGen decimal) {
      return new Decimals(decimal);
    }
    if (generator instanceof MomentGen moment) {
      return new Moments(moment);
    }
    throw new IllegalArgumentException("a reference needs a number or a time");
  }

  private record Window(long low, long high) {
  }

  private interface Narrow {
    long lowest();

    long highest();

    long read(String cell);

    Generator between(long low, long high);

    String family();
  }

  private record Ints(IntGen ints) implements Narrow {
    @Override public long lowest() { return ints.lowest(); }

    @Override public long highest() { return ints.highest(); }

    @Override public long read(String cell) { return Long.parseLong(cell); }

    @Override public Generator between(long low, long high) { return ints.narrowed((int) low, (int) high); }

    @Override public String family() { return ints.family(); }
  }

  private record Decimals(DecimalGen decimal) implements Narrow {
    @Override public long lowest() { return decimal.low(); }

    @Override public long highest() { return decimal.high(); }

    @Override public long read(String cell) { return decimal.units(cell); }

    @Override public Generator between(long low, long high) { return decimal.narrowed(low, high); }

    @Override public String family() { return decimal.family(); }
  }

  private record Moments(MomentGen moment) implements Narrow {
    @Override public long lowest() { return moment.low(); }

    @Override public long highest() { return moment.high(); }

    @Override public long read(String cell) { return moment.position(cell); }

    @Override public Generator between(long low, long high) { return moment.narrowed(low, high); }

    @Override public String family() { return moment.family(); }
  }
}
