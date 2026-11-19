package agnesse.testsystems.slim.property;

/**
 * The v1 cell language. Keywords are lowercase. Whitespace may separate tokens.
 *
 * <pre>
 * spec    = int | boolean | string | list | oneof | decimal | money | pattern | allof
 * int     = "int" filter? (":" range)?
 * filter  = ("!=" | "&gt;=" | "&lt;=" | "&gt;" | "&lt;") integer
 * range   = integer ".." integer
 * boolean = "boolean"
 * string  = "string" (":" ("ascii" | "adversarial"))? ("length" range)?
 * list    = "list" "of" spec ("size" range)?
 * oneof   = "one" "of" ":" choice (weight)? ("," choice (weight)?)*
 * allof   = "all" "of" ":" group ("x" group)*
 * group   = choice ("," choice)*
 * decimal = "decimal" ":" fixed ".." fixed "scale" integer
 * money   = "money" ":" fixed ".." fixed
 * pattern = "pattern" ":" (literal | class "{" integer "}")+
 * class   = "[0-9]" | "[A-Z]" | "[a-z]"
 * choice  = characters other than comma or whitespace
 * weight  = digits, at least 1
 * fixed   = "-"? digits ("." digits)?
 * integer = "-"? digits, in the 32-bit signed range
 * </pre>
 *
 * A missing int range is {@code -10000..10000}. A missing string length is
 * {@code 0..20} of printable ASCII except {@code |}. {@code string:adversarial}
 * mixes those draws with a fixed catalog of hostile payloads. A missing list
 * size is {@code 0..8}. A filter that leaves no int is rejected here, so a
 * draw never discards. Ranges are inclusive. {@code size} binds to the list
 * just closed, and {@code length} binds to the string just closed.
 * {@code decimal} is fixed-point, scale 0 through 8. {@code money} is scale 2.
 * {@code all of} enumerates; a property made of those columns runs their
 * cross product instead of sampling.
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
