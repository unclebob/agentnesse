package agnesse.testsystems.slim.tables;

import agnesse.slim.SlimServer;
import agnesse.slim.instructions.Instruction;
import agnesse.slim.instructions.LanesInstruction;
import agnesse.slim.instructions.MakeInstruction;
import agnesse.testsystems.slim.SlimClient;
import agnesse.testsystems.slim.SlimCommunicationException;
import agnesse.testsystems.slim.sequence.CallBroke;
import agnesse.testsystems.slim.sequence.Desk;
import agnesse.testsystems.slim.sequence.Lane;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** The page's one Slim connection. Concurrent calls leave as one lanes instruction. */
final class SlimDesk implements Desk {
  private final SlimClient client;
  private final AtomicInteger ids = new AtomicInteger();

  SlimDesk(SlimClient client) {
    this.client = client;
  }

  @Override
  public boolean lanes() {
    return true;
  }

  boolean probes() {
    Object value = invoke(new LanesInstruction(id()));
    return value != null && LanesInstruction.INSTRUCTION.equals(value.toString());
  }

  @Override
  public void make(String instance, String fixture, String[] arguments) {
    Object value = invoke(new MakeInstruction(id(), instance, fixture, arguments));
    if (value != null && "OK".equals(value.toString())) {
      return;
    }
    throw new CallBroke(value == null ? "no result" : value.toString());
  }

  @Override
  public String call(int lane, String instance, String method, List<String> arguments) {
    agnesse.slim.instructions.CallInstruction instruction = new agnesse.slim.instructions.CallInstruction(
        id(), instance, Disgracer.disgraceMethodName(method), arguments.toArray());
    return text(invoke(instruction));
  }

  @Override
  public List<Lane.Stamp> runLanes(List<Lane.Group> groups) {
    String id = id();
    List<Object> words = new ArrayList<>();
    words.add(id);
    words.add(LanesInstruction.INSTRUCTION);
    for (Lane.Group group : groups) {
      words.add(group.wire());
    }
    Object value = invoke(LanesInstruction.fromWire(id, words));
    return stamps(value);
  }

  void close() {
    // The page owns the Slim process and closes it after the last table.
  }

  private String text(Object value) {
    if (value instanceof String text && text.startsWith(SlimServer.EXCEPTION_TAG)) {
      throw new CallBroke(text);
    }
    if (value == null) {
      throw new CallBroke("no result");
    }
    return value.toString();
  }

  private Object invoke(Instruction instruction) {
    try {
      Map<String, Object> result = client.invokeAndGetResponse(List.of(instruction));
      return result.get(instruction.getId());
    } catch (SlimCommunicationException e) {
      throw new CallBroke(e.getMessage());
    }
  }

  private List<Lane.Stamp> stamps(Object value) {
    if (!(value instanceof List<?> rows)) {
      throw new CallBroke(value == null ? "no result" : value.toString());
    }
    List<Lane.Stamp> hits = new ArrayList<>();
    for (Object row : rows) {
      hits.add(stamp(row));
    }
    return hits;
  }

  private Lane.Stamp stamp(Object row) {
    if (!(row instanceof List<?> cells) || cells.size() < 5) {
      throw new CallBroke("no result");
    }
    return new Lane.Stamp(String.valueOf(cells.get(0)), String.valueOf(cells.get(1)),
        Long.parseLong(String.valueOf(cells.get(2))), Long.parseLong(String.valueOf(cells.get(3))),
        String.valueOf(cells.get(4)));
  }

  private String id() {
    return "c" + ids.incrementAndGet();
  }
}
