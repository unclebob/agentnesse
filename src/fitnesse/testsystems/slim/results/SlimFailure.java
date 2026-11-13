package fitnesse.testsystems.slim.results;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fitnesse.slim.SlimServer.COULD_NOT_INVOKE_CONSTRUCTOR;
import static fitnesse.slim.SlimServer.NO_CLASS;
import static fitnesse.slim.SlimServer.NO_CONSTRUCTOR;
import static fitnesse.slim.SlimServer.NO_METHOD_IN_CLASS;

/**
 * A Slim failure pulled out of an {@code __EXCEPTION__} string. The message
 * and the stack come from the string. Page, block, row, and cell are filled
 * in by the test system when it knows them.
 */
public final class SlimFailure {
  public enum Kind {
    MISSING_FIXTURE,
    MISSING_METHOD,
    EXCEPTION,
    LAUNCH_FAILURE
  }

  private static final Pattern MESSAGE = Pattern.compile("message:<<(.*?)>>(?!>)", Pattern.DOTALL);

  private final Kind kind;
  private final String type;
  private final String message;
  private final String stack;
  private String page;
  private String block;
  private Integer row;
  private Integer cell;

  private SlimFailure(Kind kind, String type, String message, String stack) {
    this.kind = kind;
    this.type = type;
    this.message = message;
    this.stack = stack;
  }

  public static SlimFailure parse(String exceptionValue) {
    String raw = exceptionValue == null ? "" : exceptionValue;
    Matcher matcher = MESSAGE.matcher(raw);
    String body = matcher.find() ? matcher.group(1) : raw;
    String type = typeOf(body);
    String message = messageOf(body, type);
    String stack = stackOf(raw, body);
    return new SlimFailure(kindOf(raw, type), type, message, stack);
  }

  public Kind kind() {
    return kind;
  }

  public String type() {
    return type;
  }

  public String page() {
    return page;
  }

  public String block() {
    return block;
  }

  public Integer row() {
    return row;
  }

  public Integer cell() {
    return cell;
  }

  public String message() {
    return message;
  }

  public String stack() {
    return stack;
  }

  public SlimFailure at(String page, String block, Integer row, Integer cell) {
    this.page = page;
    this.block = block;
    this.row = row;
    this.cell = cell;
    return this;
  }

  private static Kind kindOf(String raw, String type) {
    String lower = raw.toLowerCase();
    if (lower.contains("bindexception") || lower.contains("could not start")
      || lower.contains("connection refused") || lower.contains("slim server died")
      || lower.contains("failed to connect")) {
      return Kind.LAUNCH_FAILURE;
    }
    if (NO_METHOD_IN_CLASS.equals(type)) {
      return Kind.MISSING_METHOD;
    }
    if (NO_CLASS.equals(type) || NO_CONSTRUCTOR.equals(type) || COULD_NOT_INVOKE_CONSTRUCTOR.equals(type)) {
      return Kind.MISSING_FIXTURE;
    }
    return Kind.EXCEPTION;
  }

  private static String typeOf(String body) {
    if (body.isEmpty()) {
      return "";
    }
    int space = body.indexOf(' ');
    String first = space < 0 ? body : body.substring(0, space);
    if (first.startsWith("NO_") || first.startsWith("COULD_") || first.startsWith("MALFORMED") || first.startsWith("TIMED")) {
      return first;
    }
    int colon = body.indexOf(':');
    if (colon > 0 && colon < 80 && !body.substring(0, colon).contains(" ")) {
      return body.substring(0, colon);
    }
    return "";
  }

  private static String messageOf(String body, String type) {
    if (!type.isEmpty() && body.startsWith(type)) {
      String rest = body.substring(type.length()).trim();
      if (rest.startsWith(":")) {
        rest = rest.substring(1).trim();
      }
      int newline = rest.indexOf('\n');
      if (newline >= 0) {
        rest = rest.substring(0, newline).trim();
      }
      return rest.isEmpty() ? body : rest;
    }
    int newline = body.indexOf('\n');
    return newline < 0 ? body : body.substring(0, newline);
  }

  private static String stackOf(String raw, String body) {
    int start = body.indexOf('\n');
    if (start >= 0 && body.substring(start).contains("at ")) {
      return body.substring(start + 1).trim();
    }
    int at = raw.indexOf("\tat ");
    return at < 0 ? "" : raw.substring(at).trim();
  }
}
