package agnesse.testsystems.slim.property;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Turns one bound token into a day number, a second of the day, or an epoch second. */
final class Moments {
  private Moments() {
  }

  static long bound(MomentGen.Kind kind, String token, ZoneId zone, Instant now) {
    if (token.startsWith("now")) {
      return relative(kind, token.substring(3), zone, now);
    }
    return absolute(kind, token, zone);
  }

  private static long relative(MomentGen.Kind kind, String tail, ZoneId zone, Instant now) {
    if (kind == MomentGen.Kind.DATE) {
      return dateShift(tail, zone, now);
    }
    Instant shifted = shift(now, tail);
    if (kind == MomentGen.Kind.TIME) {
      return shifted.atZone(zone).toLocalTime().toSecondOfDay();
    }
    return shifted.getEpochSecond();
  }

  private static long absolute(MomentGen.Kind kind, String token, ZoneId zone) {
    try {
      return switch (kind) {
        case DATE -> LocalDate.parse(token, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay();
        case TIME -> LocalTime.parse(token, TimeText.TIME).toSecondOfDay();
        case DATETIME -> TimeText.instant(token, zone).getEpochSecond();
      };
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(expected(kind));
    }
  }

  private static String expected(MomentGen.Kind kind) {
    return switch (kind) {
      case DATE -> "expected a date";
      case TIME -> "expected a time";
      case DATETIME -> "expected a datetime";
    };
  }

  private static long dateShift(String tail, ZoneId zone, Instant now) {
    LocalDate date = now.atZone(zone).toLocalDate();
    int index = 0;
    while (index < tail.length()) {
      int sign = signAt(tail, index);
      Piece piece = piece(tail, index + 1);
      if (piece.empty || piece.hours != 0 || piece.minutes != 0 || piece.seconds != 0) {
        throw new IllegalArgumentException(piece.empty ? "expected a duration" : "a date offset uses d");
      }
      date = plusDays(date, sign, piece.days);
      index = piece.next;
    }
    return date.toEpochDay();
  }

  private static LocalDate plusDays(LocalDate date, int sign, long days) {
    try {
      return date.plusDays(Math.multiplyExact(sign, days));
    } catch (DateTimeException e) {
      throw new IllegalArgumentException("date is out of range");
    }
  }

  static Instant shift(Instant now, String tail) {
    Instant at = now;
    int index = 0;
    while (index < tail.length()) {
      int sign = signAt(tail, index);
      Piece piece = piece(tail, index + 1);
      if (piece.empty) {
        throw new IllegalArgumentException("expected a duration");
      }
      at = move(at, sign, piece);
      index = piece.next;
    }
    return at;
  }

  private static Instant move(Instant at, int sign, Piece piece) {
    try {
      Duration duration = piece.duration();
      return sign > 0 ? at.plus(duration) : at.minus(duration);
    } catch (DateTimeException | ArithmeticException e) {
      throw new IllegalArgumentException("duration is too large");
    }
  }

  private static int signAt(String tail, int index) {
    char sign = tail.charAt(index);
    if (sign == '+') {
      return 1;
    }
    if (sign == '-') {
      return -1;
    }
    throw new IllegalArgumentException("expected a duration");
  }

  private static Piece piece(String tail, int index) {
    long days = 0;
    long hours = 0;
    long minutes = 0;
    long seconds = 0;
    int span = 0;
    int i = index;
    while (i < tail.length() && tail.charAt(i) != '+' && tail.charAt(i) != '-') {
      NumberRead read = number(tail, i);
      if (read.count == 0 || read.next >= tail.length()) {
        throw new IllegalArgumentException("expected a duration");
      }
      long n = read.value;
      switch (tail.charAt(read.next)) {
        case 'd' -> days = add(days, n);
        case 'h' -> hours = add(hours, n);
        case 'm' -> minutes = add(minutes, n);
        case 's' -> seconds = add(seconds, n);
        default -> throw new IllegalArgumentException("expected a duration");
      }
      i = read.next + 1;
      span++;
    }
    return new Piece(days, hours, minutes, seconds, i, span == 0);
  }

  private static NumberRead number(String tail, int index) {
    long value = 0;
    int count = 0;
    int i = index;
    while (i < tail.length() && Character.isDigit(tail.charAt(i))) {
      value = appendDigit(value, tail.charAt(i));
      i++;
      count++;
    }
    return new NumberRead(value, i, count);
  }

  private static long appendDigit(long value, char digit) {
    try {
      return Math.addExact(Math.multiplyExact(value, 10), digit - '0');
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("duration is too large");
    }
  }

  private static long add(long left, long right) {
    try {
      return Math.addExact(left, right);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("duration is too large");
    }
  }

  private record NumberRead(long value, int next, int count) {
  }

  private static final class Piece {
    final long days;
    final long hours;
    final long minutes;
    final long seconds;
    final int next;
    final boolean empty;

    private Piece(long days, long hours, long minutes, long seconds, int next, boolean empty) {
      this.days = days;
      this.hours = hours;
      this.minutes = minutes;
      this.seconds = seconds;
      this.next = next;
      this.empty = empty;
    }

    Duration duration() {
      return Duration.ofDays(days).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
    }
  }
}
