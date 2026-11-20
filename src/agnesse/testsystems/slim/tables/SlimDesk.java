package agnesse.testsystems.slim.tables;

import agnesse.slim.SlimServer;
import agnesse.slim.instructions.CallInstruction;
import agnesse.slim.instructions.Instruction;
import agnesse.slim.instructions.MakeInstruction;
import agnesse.testsystems.slim.SlimClient;
import agnesse.testsystems.slim.SlimCommunicationException;
import agnesse.testsystems.slim.sequence.CallBroke;
import agnesse.testsystems.slim.sequence.Desk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** One Slim connection per lane. Lane 0 is the connection the page already holds. */
final class SlimDesk implements Desk {
  private final List<SlimClient> lanes;
  private final List<SlimClient> extras;
  private final AtomicInteger ids = new AtomicInteger();

  SlimDesk(SlimClient main, List<SlimClient> extras) {
    List<SlimClient> all = new ArrayList<>();
    all.add(main);
    all.addAll(extras);
    this.lanes = List.copyOf(all);
    this.extras = List.copyOf(extras);
  }

  int lanes() {
    return lanes.size();
  }

  @Override
  public void make(String instance, String fixture, String[] arguments) {
    Object value = invoke(0, new MakeInstruction(id(), instance, fixture, arguments));
    if (value != null && "OK".equals(value.toString())) {
      return;
    }
    throw new CallBroke(value == null ? "no result" : value.toString());
  }

  @Override
  public String call(int lane, String instance, String method, List<String> arguments) {
    Instruction instruction = new CallInstruction(id(), instance, Disgracer.disgraceMethodName(method),
        arguments.toArray());
    Object value = invoke(lane, instruction);
    if (value instanceof String text && text.startsWith(SlimServer.EXCEPTION_TAG)) {
      throw new CallBroke(text);
    }
    if (value == null) {
      throw new CallBroke("no result");
    }
    return value.toString();
  }

  void close() {
    close(extras);
  }

  static void close(List<SlimClient> extras) {
    for (SlimClient extra : extras) {
      bye(extra);
    }
  }

  private static void bye(SlimClient extra) {
    try {
      extra.bye();
    } catch (IOException ignored) {
      // The page destroys the Slim process when the table is done.
    }
  }

  private Object invoke(int lane, Instruction instruction) {
    try {
      Map<String, Object> result = lanes.get(lane).invokeAndGetResponse(List.of(instruction));
      return result.get(instruction.getId());
    } catch (SlimCommunicationException e) {
      throw new CallBroke(e.getMessage());
    }
  }

  private String id() {
    return "c" + ids.incrementAndGet();
  }
}
