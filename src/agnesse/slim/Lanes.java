package agnesse.slim;

import agnesse.slim.instructions.Instruction;
import agnesse.slim.instructions.InstructionExecutor;
import agnesse.slim.instructions.InstructionFactory;
import agnesse.slim.instructions.InstructionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Runs one lane per thread inside a single Slim connection. */
public final class Lanes {
  private Lanes() {
  }

  public static final String READY = "lanes";

  public static Object run(InstructionExecutor executor, List<Object> groups) {
    NameTranslator translator = new NameTranslatorIdentity();
    AtomicLong clock = new AtomicLong();
    List<Object> rows = new ArrayList<>();
    List<Thread> threads = new ArrayList<>();
    for (Object group : groups) {
      threads.add(lane(executor, translator, clock, rows, group));
    }
    for (Thread thread : threads) {
      thread.start();
    }
    for (Thread thread : threads) {
      join(thread);
    }
    return rows;
  }

  private static Thread lane(InstructionExecutor executor, NameTranslator translator, AtomicLong clock,
                             List<Object> rows, Object group) {
    return new Thread(() -> runLane(executor, translator, clock, rows, group), "agnesse-lane");
  }

  private static void runLane(InstructionExecutor executor, NameTranslator translator, AtomicLong clock,
                              List<Object> rows, Object group) {
    if (!(group instanceof List<?> lane) || lane.size() < 2 || !(lane.get(1) instanceof List<?> calls)) {
      return;
    }
    String tag = String.valueOf(lane.get(0));
    for (Object call : calls) {
      if (call instanceof List<?> words) {
        stamp(executor, translator, clock, rows, tag, words);
      }
    }
  }

  private static void stamp(InstructionExecutor executor, NameTranslator translator, AtomicLong clock,
                            List<Object> rows, String tag, List<?> words) {
    long started = clock.incrementAndGet();
    String result = invoke(executor, translator, words);
    long finished = clock.incrementAndGet();
    synchronized (rows) {
      rows.add(List.of(tag, id(words), Long.toString(started), Long.toString(finished), result));
    }
  }

  private static String id(List<?> words) {
    if (words.isEmpty()) {
      return "";
    }
    return String.valueOf(words.get(0));
  }

  private static String invoke(InstructionExecutor executor, NameTranslator translator, List<?> words) {
    try {
      Instruction instruction = InstructionFactory.createInstruction(cast(words), translator);
      InstructionResult result = instruction.execute(executor);
      Object value = result.getResult();
      return value == null ? "null" : value.toString();
    } catch (RuntimeException e) {
      return e.toString();
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Object> cast(List<?> words) {
    return (List<Object>) words;
  }

  private static void join(Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException caught) {
      Thread.currentThread().interrupt();
    }
  }
}
