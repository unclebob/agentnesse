package agnesse.testsystems.slim.property;

import java.util.List;

/** Compact JSON with declaration order. Leaf cells are JSON strings. */
final class JsonText {
  private JsonText() {
  }

  static String quote(String text) {
    StringBuilder out = new StringBuilder(text.length() + 2);
    out.append('"');
    for (int i = 0; i < text.length(); i++) {
      append(out, text.charAt(i));
    }
    out.append('"');
    return out.toString();
  }

  static String array(List<Value> items) {
    StringBuilder out = new StringBuilder("[");
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      out.append(items.get(i).json());
    }
    out.append(']');
    return out.toString();
  }

  static String object(List<String> names, List<Value> fields) {
    StringBuilder out = new StringBuilder("{");
    for (int i = 0; i < names.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      out.append(quote(names.get(i))).append(':').append(fields.get(i).json());
    }
    out.append('}');
    return out.toString();
  }

  private static void append(StringBuilder out, char c) {
    switch (c) {
      case '"' -> out.append("\\\"");
      case '\\' -> out.append("\\\\");
      case '\b' -> out.append("\\b");
      case '\f' -> out.append("\\f");
      case '\n' -> out.append("\\n");
      case '\r' -> out.append("\\r");
      case '\t' -> out.append("\\t");
      default -> plain(out, c);
    }
  }

  private static void plain(StringBuilder out, char c) {
    if (c < 0x20) {
      out.append("\\u00");
      out.append(Character.forDigit(c / 16, 16));
      out.append(Character.forDigit(c % 16, 16));
      return;
    }
    out.append(c);
  }
}
