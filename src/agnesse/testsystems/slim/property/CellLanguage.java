package agnesse.testsystems.slim.property;

/**
 * The v1 cell language. Keywords are lowercase. Whitespace may separate tokens.
 *
 * <pre>
 * spec    = int | boolean | string | list | oneof
 * int     = "int" filter? (":" range)?
 * filter  = ("!=" | "&gt;=" | "&lt;=" | "&gt;" | "&lt;") integer
 * range   = integer ".." integer
 * boolean = "boolean"
 * string  = "string" (":" "ascii")? ("length" range)?
 * list    = "list" "of" spec ("size" range)?
 * oneof   = "one" "of" ":" choice ("," choice)*
 * choice  = characters other than comma or whitespace
 * integer = "-"? digits, in the 32-bit signed range
 * </pre>
 *
 * A missing int range is {@code -10000..10000}. A missing string length is
 * {@code 0..20} of printable ASCII except {@code |}. A missing list size is
 * {@code 0..8}. A filter that leaves no int is rejected here, so a draw never
 * discards. Ranges are inclusive. {@code size} binds to the list just closed,
 * and {@code length} binds to the string just closed.
 */
public final class CellLanguage {
  private CellLanguage() {
  }

  public static Generator parse(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("generator spec is missing");
    }
    return new SpecParser(text).parse();
  }
}
