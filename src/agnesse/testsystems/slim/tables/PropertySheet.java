package agnesse.testsystems.slim.tables;

import agnesse.testsystems.slim.SlimTestContext;
import agnesse.testsystems.slim.Table;
import agnesse.testsystems.slim.property.CellLanguage;
import agnesse.testsystems.slim.property.Clock;
import agnesse.testsystems.slim.property.Generator;
import agnesse.testsystems.slim.property.Links;
import agnesse.testsystems.slim.property.Row;
import agnesse.testsystems.slim.property.Seed;
import agnesse.testsystems.slim.property.Value;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The rows of one property table: how many trials, which seed, which clock,
 * and the generator or expected text in each column. A scenario names its
 * inputs instead of a header row. The clock is the {@code now} row, or an
 * instant derived from the seed when that row is blank.
 */
final class PropertySheet {
  static final int DEFAULT_TRIALS = 100;

  final String name;
  final int trials;
  final Seed seed;
  final Clock clock;
  final int trialsRow;
  final int seedRow;
  final int nowRow;
  final boolean hasTrials;
  final boolean hasSeed;
  final boolean hasNow;
  final boolean timed;
  final List<Column> columns;
  final ScenarioTable scenario;

  private PropertySheet(String name, Config config, List<Column> columns, ScenarioTable scenario,
                        Seed seed, Clock clock, boolean timed) throws SyntaxError {
    this.name = name;
    this.trials = resolved(config, columns);
    this.seed = seed;
    this.clock = clock;
    this.trialsRow = config.trialsRow;
    this.seedRow = config.seedRow;
    this.nowRow = config.nowRow;
    this.hasTrials = config.trialsSeen;
    this.hasSeed = config.seedSeen;
    this.hasNow = config.nowSeen;
    this.timed = timed;
    this.columns = columns;
    this.scenario = scenario;
  }

  static PropertySheet read(Table table, String fixtureName, SlimTestContext context) throws SyntaxError {
    String name = named(table, fixtureName);
    ScenarioTable scenario = context.getScenario(name);
    Config config = readConfig(table);
    Seed seed = new Seed(config.explicit == null ? new Random().nextLong() : config.explicit);
    Clock clock = clock(context, config, seed);
    if (scenario != null) {
      return scenario(name, scenario, config, table, seed, clock);
    }
    return fixture(name, config, table, seed, clock);
  }

  private static Clock clock(SlimTestContext context, Config config, Seed seed) throws SyntaxError {
    try {
      ZoneId zone = Clock.named(context.getPageToTest().getVariable("zone"));
      Instant now = config.nowText == null ? Clock.fromSeed(seed.value()) : Clock.parse(config.nowText, zone);
      return new Clock(zone, now);
    } catch (IllegalArgumentException bad) {
      throw new SyntaxError(bad.getMessage());
    }
  }

  List<Drawn> draw() {
    if (enumerated()) {
      return enumeratedRows();
    }
    Random random = seed.random();
    List<Drawn> rows = new ArrayList<>();
    for (int trial = 0; trial < trials; trial++) {
      rows.add(drawRow(random));
    }
    return rows;
  }

  private boolean enumerated() {
    for (Column column : columns) {
      if (column.kind == Kind.INPUT) {
        return column.generator.domain() != null;
      }
    }
    throw new IllegalStateException("a property needs an input");
  }

  private List<Drawn> enumeratedRows() {
    List<Column> inputs = new ArrayList<>();
    for (Column column : columns) {
      if (column.kind == Kind.INPUT) {
        inputs.add(column);
      }
    }
    List<Drawn> rows = new ArrayList<>();
    for (int ordinal = 0; ordinal < trials; ordinal++) {
      rows.add(enumeratedRow(inputs, ordinal));
    }
    return rows;
  }

  private Drawn enumeratedRow(List<Column> inputs, int ordinal) {
    Value[] picks = new Value[inputs.size()];
    int rest = ordinal;
    for (int i = inputs.size() - 1; i >= 0; i--) {
      List<Value> domain = inputs.get(i).generator.domain();
      picks[i] = domain.get(rest % domain.size());
      rest /= domain.size();
    }
    List<String> cells = new ArrayList<>();
    List<Value> values = new ArrayList<>();
    int input = 0;
    for (Column column : columns) {
      if (column.kind == Kind.INPUT) {
        cells.add(picks[input].cell());
        values.add(picks[input]);
        input++;
      } else {
        values.add(null);
        cells.add(column.literal);
      }
    }
    return new Drawn(cells, values);
  }

  private static int resolved(Config config, List<Column> columns) throws SyntaxError {
    long cases = cases(columns);
    if (cases == 0) {
      return config.trials;
    }
    if (cases > Generator.ENUMERATION_LIMIT) {
      throw new SyntaxError("all of has " + cases + " cases; the limit is " + Generator.ENUMERATION_LIMIT);
    }
    if (config.trialsNumber && config.trials != cases) {
      throw new SyntaxError("all of has " + cases + " cases, not " + config.trials);
    }
    return (int) cases;
  }

  private static long cases(List<Column> columns) throws SyntaxError {
    long product = 1;
    boolean enumerated = false;
    boolean sampled = false;
    for (Column column : columns) {
      if (column.kind != Kind.INPUT) {
        continue;
      }
      List<Value> domain = column.generator.domain();
      if (domain == null) {
        sampled = true;
      } else {
        enumerated = true;
        product *= domain.size();
      }
    }
    if (!enumerated) {
      return 0;
    }
    if (sampled) {
      throw new SyntaxError("all of replaces sampling");
    }
    return product;
  }

  private Drawn drawRow(Random random) {
    List<String> cells = new ArrayList<>();
    List<Value> values = new ArrayList<>();
    Row row = Row.EMPTY;
    for (Column column : columns) {
      if (column.kind == Kind.INPUT) {
        Value value = column.generator.draw(random, row);
        row = row.with(column.name, value);
        values.add(value);
        cells.add(value.cell());
      } else {
        values.add(null);
        cells.add(column.literal);
      }
    }
    return new Drawn(cells, values);
  }

  static final class Drawn {
    final List<String> cells;
    final List<Value> values;

    private Drawn(List<String> cells, List<Value> values) {
      this.cells = cells;
      this.values = values;
    }
  }

  private static String named(Table table, String fixtureName) throws SyntaxError {
    if (!fixtureName.isBlank()) {
      return Disgracer.disgraceClassName(fixtureName.trim());
    }
    if (table.getColumnCountInRow(0) < 2) {
      throw new SyntaxError("a property needs a name");
    }
    return Disgracer.disgraceClassName(table.getCellContents(1, 0).trim());
  }

  private static Config readConfig(Table table) throws SyntaxError {
    Config config = new Config();
    int row = 1;
    while (row < table.getRowCount() && configKey(table, row)) {
      config.take(table, row);
      row++;
    }
    config.next = row;
    return config;
  }

  /** A clock column needs a wider row than the {@code now} setting, which has one or two cells. */
  private static boolean configKey(Table table, int row) {
    if (table.getColumnCountInRow(row) > 2) {
      return false;
    }
    String key = table.getCellContents(0, row).trim().toLowerCase(Locale.ROOT);
    return key.equals("trials") || key.equals("seed") || key.equals("now");
  }

  private static PropertySheet fixture(String name, Config config, Table table, Seed seed, Clock clock) throws SyntaxError {
    int header = config.next;
    int spec = header + 1;
    if (spec >= table.getRowCount()) {
      throw new SyntaxError("a property needs a header and one generator row");
    }
    if (spec != table.getRowCount() - 1) {
      throw new SyntaxError("a property has one generator row");
    }
    return assemble(name, config, columns(table, header, spec, clock), null, seed, clock);
  }

  private static PropertySheet assemble(String name, Config config, List<Column> columns, ScenarioTable scenario,
                                        Seed seed, Clock clock) throws SyntaxError {
    boolean timed = config.nowSeen || anyTimed(columns);
    if (timed && clock.zone() == null) {
      throw new SyntaxError("zone is not configured");
    }
    bind(columns);
    return new PropertySheet(name, config, columns, scenario, seed, clock, timed);
  }

  private static void bind(List<Column> columns) throws SyntaxError {
    List<String> names = new ArrayList<>();
    List<Generator> generators = new ArrayList<>();
    for (Column column : columns) {
      if (column.kind != Kind.INPUT) {
        continue;
      }
      names.add(column.name);
      generators.add(column.generator);
    }
    try {
      Links.bind(names, generators);
    } catch (IllegalArgumentException bad) {
      throw new SyntaxError(bad.getMessage());
    }
  }

  private static boolean anyTimed(List<Column> columns) {
    for (Column column : columns) {
      if (column.generator != null && column.generator.timed()) {
        return true;
      }
    }
    return false;
  }

  private static List<Column> columns(Table table, int header, int spec, Clock clock) throws SyntaxError {
    int width = table.getColumnCountInRow(header);
    if (table.getColumnCountInRow(spec) != width) {
      throw new SyntaxError("the generator row must have " + width + " columns");
    }
    List<Column> columns = new ArrayList<>();
    for (int index = 0; index < width; index++) {
      columns.add(column(table, header, spec, index, clock));
    }
    requireInput(columns);
    return columns;
  }

  private static void requireInput(List<Column> columns) throws SyntaxError {
    for (Column column : columns) {
      if (column.kind == Kind.INPUT) {
        return;
      }
    }
    throw new SyntaxError("a property needs an input");
  }

  private static Column column(Table table, int header, int spec, int index, Clock clock) throws SyntaxError {
    String title = table.getCellContents(index, header).trim();
    String body = table.getCellContents(index, spec).trim();
    if (title.isBlank()) {
      throw new SyntaxError("a column needs a name");
    }
    if (title.startsWith("#")) {
      return Column.note(index, body);
    }
    if (title.endsWith("?") || title.endsWith("!")) {
      return Column.output(index, title.substring(0, title.length() - 1).trim(), body);
    }
    return Column.input(index, title, parse(body, clock));
  }

  private static PropertySheet scenario(String name, ScenarioTable scenario, Config config, Table table,
                                        Seed seed, Clock clock) throws SyntaxError {
    if (config.next >= table.getRowCount()) {
      throw new SyntaxError("a property needs a generator");
    }
    List<Column> columns = new ArrayList<>();
    for (int row = config.next; row < table.getRowCount(); row++) {
      columns.add(argument(scenario, table, row, columns, clock));
    }
    requireInputs(scenario, columns);
    return assemble(name, config, columns, scenario, seed, clock);
  }

  private static Column argument(ScenarioTable scenario, Table table, int row, List<Column> soFar, Clock clock)
      throws SyntaxError {
    String name = Disgracer.disgraceMethodName(table.getCellContents(0, row).trim());
    if (!scenario.getInputs().contains(name)) {
      throw new SyntaxError(name + " is not an input to the scenario");
    }
    if (contains(soFar, name)) {
      throw new SyntaxError(name + " is already set");
    }
    String spec = second(table, row);
    if (spec.isBlank()) {
      throw new SyntaxError("a generator spec is missing");
    }
    return Column.input(soFar.size(), name, parse(spec, clock));
  }

  private static void requireInputs(ScenarioTable scenario, List<Column> columns) throws SyntaxError {
    for (String input : scenario.getInputs()) {
      if (!contains(columns, input)) {
        throw new SyntaxError(input + " needs a generator");
      }
    }
  }

  private static boolean contains(List<Column> columns, String name) {
    for (Column column : columns) {
      if (column.name.equals(name)) {
        return true;
      }
    }
    return false;
  }

  private static Generator parse(String spec, Clock clock) throws SyntaxError {
    try {
      return CellLanguage.parse(spec, clock);
    } catch (IllegalArgumentException e) {
      throw new SyntaxError(e.getMessage());
    }
  }

  private static String second(Table table, int row) {
    if (table.getColumnCountInRow(row) < 2) {
      return "";
    }
    return table.getCellContents(1, row).trim();
  }

  enum Kind { INPUT, OUTPUT, NOTE }

  static final class Column {
    final String name;
    final int index;
    final Generator generator;
    final String literal;
    final Kind kind;

    private Column(String name, int index, Generator generator, String literal, Kind kind) {
      this.name = name;
      this.index = index;
      this.generator = generator;
      this.literal = literal;
      this.kind = kind;
    }

    static Column input(int index, String name, Generator generator) {
      return new Column(name, index, generator, null, Kind.INPUT);
    }

    static Column output(int index, String name, String expected) throws SyntaxError {
      if (name.isBlank()) {
        throw new SyntaxError("a column needs a name");
      }
      return new Column(name, index, null, expected, Kind.OUTPUT);
    }

    static Column note(int index, String text) {
      return new Column("", index, null, text, Kind.NOTE);
    }

  }

  private static final class Config {
    private int trials = DEFAULT_TRIALS;
    private int trialsRow = -1;
    private boolean trialsSeen;
    private boolean trialsNumber;
    private Long explicit;
    private int seedRow = -1;
    private boolean seedSeen;
    private String nowText;
    private int nowRow = -1;
    private boolean nowSeen;
    private int next = 1;

    private void take(Table table, int row) throws SyntaxError {
      String key = table.getCellContents(0, row).trim().toLowerCase(Locale.ROOT);
      if (key.equals("trials")) {
        setTrials(second(table, row), row);
      } else if (key.equals("seed")) {
        setSeed(second(table, row), row);
      } else {
        setNow(second(table, row), row);
      }
    }

    private void setTrials(String value, int row) throws SyntaxError {
      if (trialsSeen) {
        throw new SyntaxError("trials is already set");
      }
      trialsSeen = true;
      trialsNumber = !value.isBlank();
      trials = parseTrials(value);
      trialsRow = row;
    }

    private void setSeed(String value, int row) throws SyntaxError {
      if (seedSeen) {
        throw new SyntaxError("seed is already set");
      }
      seedSeen = true;
      explicit = parseSeed(value);
      seedRow = row;
    }

    private void setNow(String value, int row) throws SyntaxError {
      if (nowSeen) {
        throw new SyntaxError("now is already set");
      }
      nowSeen = true;
      nowText = value.isBlank() ? null : value.trim();
      nowRow = row;
    }

    private static int parseTrials(String text) throws SyntaxError {
      if (text.isBlank()) {
        return DEFAULT_TRIALS;
      }
      int n = whole(text);
      if (n < 1) {
        throw new SyntaxError("trials must be at least 1");
      }
      return n;
    }

    private static Long parseSeed(String text) throws SyntaxError {
      if (text.isBlank()) {
        return null;
      }
      try {
        return Long.parseLong(text.trim());
      } catch (NumberFormatException e) {
        throw new SyntaxError("seed must be a whole number");
      }
    }

    private static int whole(String text) throws SyntaxError {
      try {
        return Integer.parseInt(text.trim());
      } catch (NumberFormatException e) {
        throw new SyntaxError("trials must be a whole number");
      }
    }
  }
}
