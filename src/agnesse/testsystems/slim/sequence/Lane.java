package agnesse.testsystems.slim.sequence;

import java.util.ArrayList;
import java.util.List;

/** One lane's calls, and one stamped result, for the lanes instruction. */
public final class Lane {
  private Lane() {
  }

  public record Call(String id, String instance, String method, List<String> arguments) {
    public List<Object> wire() {
      List<Object> row = new ArrayList<>();
      row.add(id);
      row.add("call");
      row.add(instance);
      row.add(method);
      row.addAll(arguments);
      return row;
    }
  }

  public record Group(String tag, List<Call> calls) {
    public List<Object> wire() {
      List<Object> body = new ArrayList<>();
      for (Call call : calls) {
        body.add(call.wire());
      }
      return List.of(tag, body);
    }
  }

  public record Stamp(String tag, String id, long started, long finished, String result) {
  }
}
