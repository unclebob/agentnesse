package agnesse.testsystems.slim.tables;

import agnesse.testsystems.slim.SlimTestContext;
import agnesse.testsystems.slim.Table;
import agnesse.testsystems.slim.property.CellLanguage;
import agnesse.testsystems.slim.property.Generator;
import agnesse.testsystems.slim.property.Seed;
import agnesse.testsystems.slim.property.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The rows of one property table: how many trials, which seed, and the
 * generator or expected text in each column. A scenario names its inputs
 * instead of a header row.
 */
final class PropertySheet {
  static final int DEFAULT_TRIALS = 100;

  final String name;
  final int trials;
  final Seed seed;
  final int trialsRow;
  final int seedRow;
  final boolean hasTrials;
  final boolean hasSeed;
  final List<Column> columns;
  final ScenarioTable scenario;

  private PropertySheet(String name, Config config, List<Column> columns, ScenarioTable scenario) {
    this.name = name;
    this.trials = config.trials;
    this.seed = config.explicit == null ? new Seed(new Random().nextLong()) : new Seed(config.explicit);
    this.trialsRow = config.trialsRow;
    this.seedRow = config.seedRow;
    this.hasTrials = config.trialsSeen;
    this.hasSeed = config.seedSeen;
    this.columns = columns;
    this.scenario = scenario;
  }

  static PropertySheet read(Table table, String fixtureName, SlimTestContext context) throws SyntaxError {
    String name = named(table, fixtureName);
    ScenarioTable scenario = context.getScenario(name);
    Config config = readConfig(table);
    if (scenario != null) {
      return scenario(name, scenario, config, table);
    }
    return fixture(name, config, table);
  }

  List<Drawn> draw() {
    Random random = seed.random();
    List<Drawn> rows = new ArrayList<>();
    for (int trial = 0; trial < trials; trial++) {
      rows.add(drawRow(random));
    }
    return rows;
  }

  private Drawn drawRow(Random random) {
    List<String> cells = new ArrayList<>();
    List<Value> values = new ArrayList<>();
    for (Column column : columns) {
      if (column.kind == Kind.INPUT) {
        Value value = column.generator.draw(random);
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

  private static boolean configKey(Table table, int row) {
    String key = table.getCellContents(0, row).trim().toLowerCase(Locale.ROOT);
    return key.equals("trials") || key.equals("seed");
  }

  private static PropertySheet fixture(String name, Config config, Table table) throws SyntaxError {
    int header = config.next;
    int spec = header + 1;
    if (spec >= table.getRowCount()) {
      throw new SyntaxError("a property needs a header and one generator row");
    }
    if (spec != table.getRowCount() - 1) {
      throw new SyntaxError("a property has one generator row");
    }
    return new PropertySheet(name, config, columns(table, header, spec), null);
  }

  private static List<Column> columns(Table table, int header, int spec) throws SyntaxError {
    int width = table.getColumnCountInRow(header);
    if (table.getColumnCountInRow(spec) != width) {
      throw new SyntaxError("the generator row must have " + width + " columns");
    }
    List<Column> columns = new ArrayList<>();
    for (int index = 0; index < width; index++) {
      columns.add(column(table, header, spec, index));
    }
    return columns;
  }

  private static Column column(Table table, int header, int spec, int index) throws SyntaxError {
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
    return Column.input(index, title, parse(body));
  }

  private static PropertySheet scenario(String name, ScenarioTable scenario, Config config, Table table) throws SyntaxError {
    if (config.next >= table.getRowCount()) {
      throw new SyntaxError("a property needs a generator");
    }
    List<Column> columns = new ArrayList<>();
    for (int row = config.next; row < table.getRowCount(); row++) {
      columns.add(argument(scenario, table, row, columns));
    }
    requireInputs(scenario, columns);
    return new PropertySheet(name, config, columns, scenario);
  }

  private static Column argument(ScenarioTable scenario, Table table, int row, List<Column> soFar) throws SyntaxError {
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
    return Column.input(soFar.size(), name, parse(spec));
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

  private static Generator parse(String spec) throws SyntaxError {
    try {
      return CellLanguage.parse(spec);
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
    private Long explicit;
    private int seedRow = -1;
    private boolean seedSeen;
    private int next = 1;

    private void take(Table table, int row) throws SyntaxError {
      String key = table.getCellContents(0, row).trim().toLowerCase(Locale.ROOT);
      if (key.equals("trials")) {
        setTrials(second(table, row), row);
        return;
      }
      setSeed(second(table, row), row);
    }

    private void setTrials(String value, int row) throws SyntaxError {
      if (trialsSeen) {
        throw new SyntaxError("trials is already set");
      }
      trialsSeen = true;
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
