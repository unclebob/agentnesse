package agnesse.testsystems.slim.sequence;

import java.util.ArrayList;
import java.util.List;

/** Runs one wave of calls. Tests run them directly. The table starts a thread per call. */
public final class Waves {
  @FunctionalInterface
  public interface Overlap {
    void run(List<Runnable> tasks);
  }

  /** Same thread, in lane order. Unit tests use this so they do not start threads. */
  public static final Overlap DIRECT = Waves::direct;

  private Waves() {
  }

  public static void direct(List<Runnable> tasks) {
    for (Runnable task : tasks) {
      task.run();
    }
  }

  public static void together(List<Runnable> tasks) {
    join(start(tasks));
  }

  private static List<Thread> start(List<Runnable> tasks) {
    List<Thread> started = new ArrayList<>();
    for (Runnable task : tasks) {
      started.add(launch(task));
    }
    return started;
  }

  private static Thread launch(Runnable task) {
    Thread thread = new Thread(task, "agnesse-lane");
    thread.start();
    return thread;
  }

  private static void join(List<Thread> started) {
    for (Thread thread : started) {
      await(thread);
    }
  }

  private static void await(Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException caught) {
      Thread.currentThread().interrupt();
    }
  }
}
