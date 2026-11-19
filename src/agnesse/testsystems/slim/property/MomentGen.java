package agnesse.testsystems.slim.property;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A date, a local time, or an offset datetime. Half the draws are ordinary
 * when a boundary sits in the range; the other half are those boundaries.
 * Shrinking moves toward {@code now} when it is inside the range, otherwise
 * toward the start, and toward whole units along the way.
 */
final class MomentGen extends Generator {
  enum Kind { DATE, TIME, DATETIME }

  private final Kind kind;
  private final ZoneId zone;
  private final long low;
  private final long high;
  private final long target;
  private final long[] bias;

  private MomentGen(Kind kind, ZoneId zone, long low, long high, long target, long[] bias) {
    this.kind = kind;
    this.zone = zone;
    this.low = low;
    this.high = high;
    this.target = target;
    this.bias = bias;
  }

  static MomentGen open(Kind kind, ZoneId zone, Instant now) {
    return switch (kind) {
      case DATE -> of(kind, zone, TimeText.FIRST.toEpochDay(), TimeText.LAST.toEpochDay(), now);
      case TIME -> of(kind, zone, 0, 86_399, now);
      case DATETIME -> of(kind, zone, TimeText.windowStart(zone), TimeText.windowEnd(zone), now);
    };
  }

  static MomentGen of(Kind kind, ZoneId zone, long low, long high, Instant now) {
    if (low > high) {
      throw new IllegalArgumentException("inverted range");
    }
    return new MomentGen(kind, zone, low, high, target(kind, zone, low, high, now), bias(kind, zone, low, high));
  }

  @Override
  public boolean timed() {
    return true;
  }

  @Override
  public Value draw(Random random) {
    return new TextValue(render(pick(random)));
  }

  @Override
  List<Value> simpler(Value value) {
    List<Value> out = new ArrayList<>();
    for (long candidate : steps(read(value.cell()))) {
      out.add(new TextValue(render(candidate)));
    }
    return out;
  }

  private long pick(Random random) {
    if (bias.length > 0 && random.nextBoolean()) {
      return bias[random.nextInt(bias.length)];
    }
    return low + random.nextLong(high - low + 1);
  }

  private List<Long> steps(long current) {
    return switch (kind) {
      case DATE -> Closer.days(current, target, low, high);
      case TIME -> Closer.seconds(current, target, low, high);
      case DATETIME -> Closer.instants(current, target, low, high, zone);
    };
  }

  private static long target(Kind kind, ZoneId zone, long low, long high, Instant now) {
    long value = placed(kind, zone, now);
    if (value >= low && value <= high) {
      return value;
    }
    return low;
  }

  private static long placed(Kind kind, ZoneId zone, Instant now) {
    return switch (kind) {
      case DATE -> now.atZone(zone).toLocalDate().toEpochDay();
      case TIME -> now.atZone(zone).toLocalTime().toSecondOfDay();
      case DATETIME -> now.getEpochSecond();
    };
  }

  private static long[] bias(Kind kind, ZoneId zone, long low, long high) {
    return switch (kind) {
      case DATE -> Bias.dates(low, high);
      case TIME -> Bias.times(zone, (int) low, (int) high);
      case DATETIME -> Bias.instants(zone, low, high);
    };
  }

  private long read(String cell) {
    return switch (kind) {
      case DATE -> LocalDate.parse(cell).toEpochDay();
      case TIME -> LocalTime.parse(cell, TimeText.TIME).toSecondOfDay();
      case DATETIME -> OffsetDateTime.parse(cell, TimeText.OFFSET).toInstant().getEpochSecond();
    };
  }

  private String render(long value) {
    return switch (kind) {
      case DATE -> TimeText.date(LocalDate.ofEpochDay(value));
      case TIME -> TimeText.time(LocalTime.ofSecondOfDay((int) value));
      case DATETIME -> TimeText.offset(Instant.ofEpochSecond(value), zone);
    };
  }
}
