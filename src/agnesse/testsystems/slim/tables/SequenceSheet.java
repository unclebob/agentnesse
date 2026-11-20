package agnesse.testsystems.slim.tables;

import agnesse.testsystems.slim.Table;
import agnesse.testsystems.slim.property.CellLanguage;
import agnesse.testsystems.slim.property.Clock;
import agnesse.testsystems.slim.property.Generator;
import agnesse.testsystems.slim.sequence.Clause;
import agnesse.testsystems.slim.sequence.Command;
import agnesse.testsystems.slim.sequence.Course;
import agnesse.testsystems.slim.sequence.Guard;
import agnesse.testsystems.slim.sequence.Plan;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The rows of one sequence table: trials, length, seed, the starting model,
 * the commands, and the invariants. Command arguments use the cell generator
 * language. A blank seed is chosen here and shown on the result, not saved
 * back onto the page.
 */
final class SequenceSheet {
  static final int DEFAULT_TRIALS = 100;
  private static final int DEFAULT_STEPS = 8;
  private static final Clock PARSED = new Clock(ZoneId.of("UTC"), Instant.parse("2020-01-01T00:00:00Z"));
  private static final Pattern RANGE = Pattern.compile("(\\d+)\\s*\\.\\.\\s*(\\d+)");
  private static final Set<String> KEYS = Set.of("trials", "steps", "seed", "state");
  private static final Set<String> TIMED = Set.of("date", "time", "datetime", "period", "now");
  private static final Set<String> WORDS = Set.of(
      "int", "boolean", "string", "list", "one", "all", "decimal", "money", "pattern",
      "date", "time", "datetime", "period", "now", "span", "partition", "monotonic", "record");

  final String fixture;
  final String[] arguments;
  final int trials;
  final int trialsRow;
  final boolean hasTrials;
  final long seed;
  final int seedRow;
  final boolean hasSeed;
  final int width;
  final Plan plan;

  private SequenceSheet(String fixture, String[] arguments, Config config, int width, Plan plan) {
    this.fixture = fixture;
    this.arguments = arguments;
    this.trials = config.trials;
    this.trialsRow = config.trialsRow;
    this.hasTrials = config.trialsSeen;
    this.seed = config.seed.explicit() == null ? new Random().nextLong() : config.seed.explicit();
    this.seedRow = config.seed.row();
    this.hasSeed = config.seed.seen();
    this.width = width;
    this.plan = plan;
  }

  static SequenceSheet read(Table table, String fixtureName) throws SyntaxError {
    try {
      return build(table, fixtureName);
    } catch (IllegalArgumentException e) {
      throw new SyntaxError(e.getMessage());
    }
  }

  private static SequenceSheet build(Table table, String fixtureName) throws SyntaxError {
    boolean named = !fixtureName.isBlank();
    String fixture = fixture(table, fixtureName, named);
    String[] arguments = arguments(table, named);
    Config config = config(table);
    int header = config.next;
    commandHeader(table, header);
    int width = table.getColumnCountInRow(header);
    List<Command> commands = commands(table, header, width);
    int row = header + 1 + commands.size();
    Plan plan = Plan.create(config.state, config.low, config.high, commands, guards(table, row));
    return new SequenceSheet(fixture, arguments, config, width, plan);
  }

  private static String fixture(Table table, String fixtureName, boolean named) throws SyntaxError {
    if (named) {
      return fixtureName.trim();
    }
    if (table.getColumnCountInRow(0) < 2 || table.getCellContents(1, 0).isBlank()) {
      throw new SyntaxError("a sequence needs a name");
    }
    return Disgracer.disgraceClassName(table.getCellContents(1, 0).trim());
  }

  private static String[] arguments(Table table, boolean named) {
    int start = named ? 1 : 2;
    List<String> args = new ArrayList<>();
    for (int column = start; column < table.getColumnCountInRow(0); column++) {
      args.add(table.getCellContents(column, 0));
    }
    return args.toArray(new String[0]);
  }

  private static Config config(Table table) throws SyntaxError {
    Config config = new Config();
    int row = 1;
    while (row < table.getRowCount() && !cell(table, row, 0).equalsIgnoreCase("command")) {
      take(table, row, config);
      row++;
    }
    if (row >= table.getRowCount()) {
      throw new SyntaxError("a sequence needs a command");
    }
    config.next = row;
    return config;
  }

  private static void take(Table table, int row, Config config) throws SyntaxError {
    String key = cell(table, row, 0).toLowerCase(Locale.ROOT);
    if (table.getColumnCountInRow(row) > 2) {
      throw new SyntaxError("a command starts with command, when, and next");
    }
    if (!KEYS.contains(key)) {
      throw new SyntaxError("unknown sequence setting " + key);
    }
    config.take(key, second(table, row), row);
  }

  private static void commandHeader(Table table, int row) throws SyntaxError {
    if (table.getColumnCountInRow(row) < 3
        || !cell(table, row, 1).equalsIgnoreCase("when")
        || !cell(table, row, 2).equalsIgnoreCase("next")) {
      throw new SyntaxError("a command starts with command, when, and next");
    }
  }

  private static List<Command> commands(Table table, int header, int width) throws SyntaxError {
    List<Command> commands = new ArrayList<>();
    for (int row = header + 1; row < table.getRowCount(); row++) {
      if (cell(table, row, 0).equalsIgnoreCase("invariant")) {
        break;
      }
      commands.add(command(table, header, row, width));
    }
    return commands;
  }

  private static Command command(Table table, int header, int row, int width) throws SyntaxError {
    if (table.getColumnCountInRow(row) != width) {
      throw new SyntaxError("a command row must have " + width + " columns");
    }
    String name = cell(table, row, 0).trim();
    if (name.isEmpty()) {
      throw new SyntaxError("a command needs a name");
    }
    List<Command.Argument> arguments = new ArrayList<>();
    List<Command.Expect> expects = new ArrayList<>();
    for (int column = 3; column < width; column++) {
      split(table, header, row, column, arguments, expects);
    }
    return new Command(name, cell(table, row, 1), cell(table, row, 2), arguments, expects);
  }

  private static void split(Table table, int header, int row, int column,
                            List<Command.Argument> arguments, List<Command.Expect> expects) throws SyntaxError {
    String title = cell(table, header, column).trim();
    String body = cell(table, row, column);
    if (output(title)) {
      expects.add(expect(title, body, column));
    } else {
      arguments.add(argument(title, body, column));
    }
  }

  private static boolean output(String title) {
    return title.endsWith("?") || title.endsWith("!");
  }

  private static Command.Expect expect(String title, String body, int column) throws SyntaxError {
    boolean result = title.equalsIgnoreCase("result?") || title.equalsIgnoreCase("result!");
    String method = result ? null : title.substring(0, title.length() - 1).trim();
    if (!result && method.isEmpty()) {
      throw new SyntaxError("a column needs a name");
    }
    return new Command.Expect(result, method, body.trim(), column);
  }

  private static Command.Argument argument(String title, String body, int column) throws SyntaxError {
    if (title.isEmpty()) {
      throw new SyntaxError("a column needs a name");
    }
    String text = body.trim();
    if (text.isEmpty()) {
      return new Command.Argument(title, column, null, null);
    }
    return drawn(title, text, column);
  }

  private static Command.Argument drawn(String title, String text, int column) throws SyntaxError {
    String word = word(text);
    if (TIMED.contains(word)) {
      throw new SyntaxError("a sequence argument cannot draw a time");
    }
    if (!WORDS.contains(word)) {
      return new Command.Argument(title, column, null, text);
    }
    Generator generator = CellLanguage.parse(text, PARSED);
    return new Command.Argument(title, column, generator, null);
  }

  private static String word(String text) {
    Matcher matcher = Pattern.compile("[A-Za-z]*").matcher(text);
    matcher.lookingAt();
    return text.substring(0, matcher.end()).toLowerCase(Locale.ROOT);
  }

  private static List<Guard> guards(Table table, int row) throws SyntaxError {
    if (row >= table.getRowCount()) {
      return List.of();
    }
    invariantHeader(table, row);
    List<Guard> guards = new ArrayList<>();
    for (int next = row + 1; next < table.getRowCount(); next++) {
      guards.add(guard(table, next));
    }
    return guards;
  }

  private static void invariantHeader(Table table, int row) throws SyntaxError {
    if (!invariant(table, row)) {
      throw new SyntaxError("invariant columns are when, model, query, and expect");
    }
  }

  private static boolean invariant(Table table, int row) {
    return table.getColumnCountInRow(row) == 5
        && cell(table, row, 1).equalsIgnoreCase("when")
        && cell(table, row, 2).equalsIgnoreCase("model")
        && cell(table, row, 3).equalsIgnoreCase("query")
        && cell(table, row, 4).equalsIgnoreCase("expect");
  }

  private static Guard guard(Table table, int row) {
    if (table.getColumnCountInRow(row) != 5) {
      throw new IllegalArgumentException("invariant columns are when, model, query, and expect");
    }
    return new Guard(cell(table, row, 1), cell(table, row, 2), query(cell(table, row, 3)), cell(table, row, 4));
  }

  private static String query(String text) {
    String raw = text.trim();
    if (raw.endsWith("?") || raw.endsWith("!")) {
      raw = raw.substring(0, raw.length() - 1).trim();
    }
    return raw.isEmpty() ? null : raw;
  }

  private static String cell(Table table, int row, int column) {
    return table.getCellContents(column, row).trim();
  }

  private static String second(Table table, int row) {
    if (table.getColumnCountInRow(row) < 2) {
      return "";
    }
    return table.getCellContents(1, row);
  }

  private static final class Config {
    private int trials = DEFAULT_TRIALS;
    private int trialsRow = -1;
    private boolean trialsSeen;
    private final SeedCell seed = new SeedCell();
    private int low = 1;
    private int high = DEFAULT_STEPS;
    private boolean stepsSeen;
    private final Map<String, String> state = new LinkedHashMap<>();
    private boolean stateSeen;
    private int next = 1;

    private void take(String key, String value, int row) throws SyntaxError {
      if (key.equals("trials")) {
        trials(value, row);
      } else if (key.equals("steps")) {
        steps(value);
      } else if (key.equals("seed")) {
        seed.read(value, row);
      } else {
        state(value);
      }
    }

    private void trials(String value, int row) throws SyntaxError {
      if (trialsSeen) {
        throw new SyntaxError("trials is already set");
      }
      trialsSeen = true;
      trials = parseTrials(value);
      trialsRow = row;
    }

    private static int parseTrials(String text) throws SyntaxError {
      if (text.isBlank()) {
        return DEFAULT_TRIALS;
      }
      try {
        int n = Integer.parseInt(text.trim());
        if (n < 1) {
          throw new SyntaxError("trials must be at least 1");
        }
        return n;
      } catch (NumberFormatException e) {
        throw new SyntaxError("trials must be a whole number");
      }
    }

    private void steps(String value) throws SyntaxError {
      if (stepsSeen) {
        throw new SyntaxError("steps is already set");
      }
      stepsSeen = true;
      if (value.isBlank()) {
        return;
      }
      int[] range = range(value);
      low = range[0];
      high = range[1];
    }

    private static int[] range(String text) throws SyntaxError {
      Matcher matcher = RANGE.matcher(text.trim());
      if (!matcher.matches()) {
        throw new SyntaxError(stepsMessage());
      }
      return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }

    private static String stepsMessage() {
      return "steps must be low..high from 1 through " + Course.CAP;
    }

    private void state(String value) throws SyntaxError {
      if (stateSeen) {
        throw new SyntaxError("state is already set");
      }
      stateSeen = true;
      if (value.isBlank()) {
        return;
      }
      for (String piece : value.split(";", -1)) {
        put(piece.trim());
      }
    }

    private void put(String item) throws SyntaxError {
      int at = item.indexOf('=');
      if (at < 1 || item.indexOf(' ') != -1) {
        throw new SyntaxError("state must be name=value");
      }
      String name = item.substring(0, at).trim();
      String raw = item.substring(at + 1).trim();
      if (raw.isEmpty() || !Clause.spelled(name)) {
        throw new SyntaxError("state must be name=value");
      }
      if (state.containsKey(name)) {
        throw new SyntaxError(name + " is already set");
      }
      state.put(name, raw);
    }
  }
}
