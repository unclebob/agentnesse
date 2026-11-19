package agnesse.testsystems.slim.property;

import java.util.ArrayList;
import java.util.List;

/** Hostile strings mixed into {@code string:adversarial}. Length is UTF-16 units. */
final class Adversarial {
  static final List<String> PAYLOADS = List.of(
      "'",
      "\"",
      "' OR '1'='1",
      "\"; DROP TABLE t;--",
      "${7*7}",
      "{{7*7}}",
      "<script>alert(1)</script>",
      "\"><img src=x onerror=alert(1)>",
      "<b>bold</b>",
      "''italic''",
      "!include",
      "|",
      "&",
      "e\u0301",
      "\u202Ertl",
      "\uD83D\uDE00",
      "a\u200Bb",
      "\uFEFF");

  private Adversarial() {
  }

  static List<String> fitting(Span length) {
    List<String> out = new ArrayList<>();
    for (String payload : PAYLOADS) {
      if (payload.length() >= length.min && payload.length() <= length.max) {
        out.add(payload);
      }
    }
    if (length.max >= 1) {
      out.add("A".repeat(length.max));
    }
    return List.copyOf(out);
  }
}
