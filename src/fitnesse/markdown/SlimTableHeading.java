package fitnesse.markdown;

import java.util.Locale;
import java.util.Map;

/**
 * The first header cell of a bare GFM table. {@code decision:Division} is a
 * decision table named Division. A name with no type prefix is a decision table.
 */
public final class SlimTableHeading {
  private static final Map<String, String> TYPES = Map.ofEntries(
    Map.entry("decision", "decision"),
    Map.entry("dt", "decision"),
    Map.entry("dynamic decision", "dynamic-decision"),
    Map.entry("ddt", "dynamic-decision"),
    Map.entry("query", "query"),
    Map.entry("ordered query", "ordered-query"),
    Map.entry("subset query", "subset-query"),
    Map.entry("table", "table"),
    Map.entry("script", "script"),
    Map.entry("verify script", "verify-script"),
    Map.entry("scenario", "scenario"),
    Map.entry("import", "import"),
    Map.entry("library", "library"),
    Map.entry("baseline", "baseline")
  );

  private final String type;
  private final String name;
  private final String error;

  private SlimTableHeading(String type, String name, String error) {
    this.type = type;
    this.name = name;
    this.error = error;
  }

  public static SlimTableHeading parse(String cell) {
    String text = cell == null ? "" : cell.trim();
    int colon = text.indexOf(':');
    if (colon >= 0) {
      return heading(text.substring(0, colon), text.substring(colon + 1).trim());
    }
    String known = TYPES.get(text.toLowerCase(Locale.ROOT));
    if (known != null) {
      return new SlimTableHeading(known, "", null);
    }
    return new SlimTableHeading("decision", text, null);
  }

  public String type() {
    return type;
  }

  public String name() {
    return name;
  }

  public String error() {
    return error;
  }

  private static SlimTableHeading heading(String prefix, String name) {
    String known = TYPES.get(prefix.trim().toLowerCase(Locale.ROOT));
    if (known == null) {
      return new SlimTableHeading("error", name, "unknown table type \"" + prefix.trim() + "\"");
    }
    return new SlimTableHeading(known, name, null);
  }

}
