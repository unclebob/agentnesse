package agnesse.testsystems.slim.tables;

import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.SlimTestContext;
import agnesse.testsystems.slim.results.SlimTestResult;
import agnesse.testsystems.slim.sequence.Command;
import agnesse.testsystems.slim.sequence.History;
import agnesse.testsystems.slim.sequence.Plan;
import agnesse.testsystems.slim.sequence.Race;
import agnesse.testsystems.slim.sequence.Reading;
import agnesse.testsystems.slim.sequence.Schedule;
import agnesse.testsystems.slim.sequence.Step;
import agnesse.testsystems.slim.sequence.Trace;

import java.util.Arrays;
import java.util.List;

/** Paints a concurrent sequence: the trials cell, the seed, and the kept branches. */
final class RaceView {
  private RaceView() {
  }

  static void paint(MarkdownTable table, SequenceSheet sheet, Race.Result result, SlimTestContext context) {
    if (!result.passed) {
      paintHistory(table, sheet, result);
    }
    int row = trialsRow(table, sheet);
    showSeed(table, sheet);
    SlimTestResult verdict = verdict(sheet, result);
    table.updateContent(1, row, verdict);
    context.increment(verdict.getExecutionResult());
  }

  static void showSeed(MarkdownTable table, SequenceSheet sheet) {
    int row = sheet.seedRow;
    String text = Long.toString(sheet.seed);
    if (!sheet.hasSeed) {
      row = table.addRow(List.of("seed", text));
    } else {
      table.secondCell(row);
      table.substitute(1, row, text);
    }
    table.updateContent(1, row, SlimTestResult.plain(text));
  }

  static int trialsRow(MarkdownTable table, SequenceSheet sheet) {
    if (!sheet.hasTrials) {
      return table.addRow(List.of("trials", ""));
    }
    table.secondCell(sheet.trialsRow);
    return sheet.trialsRow;
  }

  private static void paintHistory(MarkdownTable table, SequenceSheet sheet, Race.Result result) {
    History history = result.history;
    if (history == null || !history.ok()) {
      return;
    }
    paintTrace(table, sheet, "prefix", result.schedule.prefix, history.prefix);
    for (int index = 0; index < result.schedule.branches.size(); index++) {
      paintTrace(table, sheet, "t" + (index + 1), result.schedule.branches.get(index), history.branches.get(index));
    }
  }

  private static void paintTrace(MarkdownTable table, SequenceSheet sheet, String label, Trace trace,
                                 List<Reading> readings) {
    for (int index = 0; index < trace.steps.size() && index < readings.size(); index++) {
      int row = table.addRow(cells(sheet, label, trace.steps.get(index), readings.get(index)));
      plain(table, row);
    }
  }

  private static List<String> cells(SequenceSheet sheet, String label, Step step, Reading reading) {
    Plan plan = sheet.plan;
    Command command = plan.commands.get(step.command);
    String[] row = new String[sheet.width];
    Arrays.fill(row, "");
    row[0] = label + " " + command.name;
    row[1] = command.whenText;
    row[2] = command.nextText;
    arguments(row, command, step);
    observed(row, command, reading);
    return List.of(row);
  }

  private static void arguments(String[] row, Command command, Step step) {
    for (int index = 0; index < command.arguments.size(); index++) {
      if (step.value(index) != null) {
        row[command.arguments.get(index).column] = step.value(index).cell();
      }
    }
  }

  private static void observed(String[] row, Command command, Reading reading) {
    for (Command.Expect expect : command.expects) {
      row[expect.column] = expect.result ? text(reading.returned()) : text(reading.queries().get(expect.method));
    }
  }

  private static void plain(MarkdownTable table, int row) {
    for (int column = 0; column < table.getColumnCountInRow(row); column++) {
      table.updateContent(column, row, SlimTestResult.plain(table.originalCell(column, row)));
    }
  }

  private static SlimTestResult verdict(SequenceSheet sheet, Race.Result result) {
    if (result.passed) {
      return SlimTestResult.pass(result.trials + " trials");
    }
    String label = label(sheet, result);
    if (result.broken) {
      return SlimTestResult.error(label);
    }
    return SlimTestResult.fail(label);
  }

  private static String label(SequenceSheet sheet, Race.Result result) {
    return "trial " + result.trial + " of " + result.trials + " drew " + Race.drew(sheet.plan, result.drawn)
        + "; " + result.why + tail(result);
  }

  private static String tail(Race.Result result) {
    if (result.capped) {
      return "; shrink cap";
    }
    if (result.shrunk) {
      return "; shrunk";
    }
    return "";
  }

  private static String text(String value) {
    return value == null ? "" : value;
  }
}
