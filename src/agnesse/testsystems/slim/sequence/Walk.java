package agnesse.testsystems.slim.sequence;

import java.util.List;

/**
 * Shrinks one failing trace. Commands are dropped, shortest first, and only
 * then are arguments shrunk. At most {@link Course#CAP} reductions are kept.
 */
public final class Walk {
  private final Plan plan;
  private Trace trace;
  private int accepted;
  private boolean dropping = true;
  private int length = 1;
  private int argStep;
  private int argIndex;
  private int issuedStep;
  private int issuedArg;
  private boolean capped;

  public Walk(Plan plan, Trace trace) {
    this.plan = plan;
    this.trace = trace;
  }

  public Trace trace() {
    return trace;
  }

  public boolean capped() {
    return capped;
  }

  public int accepted() {
    return accepted;
  }

  public List<Trace> next() {
    if (accepted >= Course.CAP) {
      capped = true;
      return List.of();
    }
    if (!dropping) {
      return argBatch();
    }
    List<Trace> shorter = dropBatch();
    if (shorter != null) {
      return shorter;
    }
    dropping = false;
    return argBatch();
  }

  public void accept(Trace next) {
    trace = next;
    accepted++;
    if (dropping) {
      length = 1;
      return;
    }
    argStep = issuedStep;
    argIndex = issuedArg;
  }

  private List<Trace> dropBatch() {
    while (length < trace.steps.size()) {
      List<Trace> found = Course.shorter(plan, trace, length);
      length++;
      if (!found.isEmpty()) {
        return found;
      }
    }
    return null;
  }

  private List<Trace> argBatch() {
    while (argStep < trace.steps.size()) {
      List<Trace> found = oneArg();
      if (found == null) {
        argStep++;
        argIndex = 0;
      } else if (!found.isEmpty()) {
        return found;
      }
    }
    return List.of();
  }

  /** {@code null} when this step has no further arguments. */
  private List<Trace> oneArg() {
    if (argIndex >= trace.steps.get(argStep).size()) {
      return null;
    }
    int step = argStep;
    int arg = argIndex;
    argIndex++;
    List<Trace> found = Course.simpler(plan, trace, step, arg);
    if (!found.isEmpty()) {
      issuedStep = step;
      issuedArg = arg;
    }
    return found;
  }
}
