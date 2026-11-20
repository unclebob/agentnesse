package agnesse.testsystems.slim.property;

/** A column or field must sit strictly after or before an earlier value. */
record Relation(String name, boolean after) {
  String word() {
    return after ? "after" : "before";
  }
}
