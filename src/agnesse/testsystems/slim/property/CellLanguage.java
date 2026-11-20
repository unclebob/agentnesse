package agnesse.testsystems.slim.property;

/**
 * The v1 cell language. Keywords are lowercase. Whitespace may separate tokens.
 *
 * <pre>
 * spec    = int | boolean | string | list | oneof | decimal | money | pattern | allof
 *         | date | time | datetime | period | now | span | partition | monotonic | record
 * int     = "int" relation? filter? (":" range)?
 * filter  = ("!=" | "&gt;=" | "&lt;=" | "&gt;" | "&lt;") integer
 * range   = integer ".." integer
 * boolean = "boolean"
 * string  = "string" (":" ("ascii" | "adversarial"))? ("length" range)?
 * list    = "list" "of" spec ("size" range)? ("unique" | "sorted")*
 * oneof   = "one" "of" ":" choice (weight)? ("," choice (weight)?)*
 * allof   = "all" "of" ":" group ("x" group)*
 * group   = choice ("," choice)*
 * pattern = "pattern" ":" (literal | class "{" integer "}")+
 * class   = "[0-9]" | "[A-Z]" | "[a-z]"
 * date    = "date" relation? zone? (":" bound ".." bound)?
 * time    = "time" relation? zone? (":" bound ".." bound)?
 * datetime= "datetime" relation? zone? (":" bound ".." bound)?
 * decimal = "decimal" relation? ":" fixed ".." fixed "scale" integer
 * money   = "money" relation? ":" fixed ".." fixed
 * period  = "period" ":" ("day" | "week" | "month") zone?
 * now     = "now"
 * span    = "span" ":" range "length" range
 * partition = "partition" ":" fixed "into" integer
 * monotonic = "monotonic" ":" range ("size" range)? "reset"?
 * record  = "record" ":" recordFields | "record" ":" "(" recordFields ")"
 * recordFields = field (";" field)*
 * field   = name spec
 * relation= ("after" | "before") name
 * zone    = "zone" ":" zoneId
 * bound   = iso | "now" offset*
 * offset  = ("+" | "-") (number unit)+
 * unit    = "d" | "h" | "m" | "s"
 * choice  = characters other than comma, semicolon, or whitespace
 * name    = letter then letters, digits, or "_"
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
 *
 * <p>A missing date range is {@code 2000-01-01..2099-12-31}. A missing time
 * range is {@code 00:00:00..23:59:59}. A missing datetime range is that same
 * window at second resolution in the generator's zone. {@code d} on a date
 * is a calendar day. On a time or datetime, {@code d} is 24 hours and
 * {@code h}, {@code m}, and {@code s} are exact. A time range does not wrap
 * past midnight. Cells are ISO-8601: {@code yyyy-MM-dd}, {@code HH:mm:ss},
 * and {@code yyyy-MM-dd'T'HH:mm:ssXXX}. {@code period} draws one second
 * before, at, or one second after a boundary in that date window. Weeks
 * start on Sunday. Half the draws are boundaries when the range contains
 * any: DST transitions in the zone, month ends, year ends, and leap days.
 *
 * <p>{@code after} and {@code before} name an earlier column or, inside a
 * record, an earlier field. The value stays one unit strictly on that side
 * of the named cell and inside the written range. {@code span} is
 * {@code start+length}, half-open. {@code partition} splits a total into
 * 1 through 8 non-negative parts. {@code monotonic} is a non-decreasing
 * list, and {@code reset} keeps the running total while shrinking.
 * {@code unique} and {@code sorted} modify the list just closed.
 * A record cell is compact JSON. A nested record is parenthesized.
 * Inside one, a choice or a bound ends at {@code )}.
 */
public final class CellLanguage {
  private CellLanguage() {
  }

  public static Generator parse(String text) {
    return parse(text, null);
  }

  public static Generator parse(String text, Clock clock) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("generator spec is missing");
    }
    return new SpecParser(text, clock).parse();
  }
}
