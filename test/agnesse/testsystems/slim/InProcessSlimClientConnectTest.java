package agnesse.testsystems.slim;

import agnesse.slim.SlimStreamReader;
import agnesse.slim.SlimVersion;
import agnesse.testsystems.CompositeExecutionLogListener;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InProcessSlimClientConnectTest {
  @Test
  public void aHeaderThatIsNotSlimIsRejected() throws Exception {
    InProcessSlimClient client = client("nope\n");

    try {
      client.connect();
      fail("expected an invalid header");
    } catch (SlimVersionMismatch e) {
      assertTrue(e.getMessage().contains("invalid slim header"));
      assertTrue(e.getMessage().contains("nope"));
    }
  }

  @Test
  public void aSlimHeaderSetsTheVersion() throws Exception {
    InProcessSlimClient client = client(SlimVersion.SLIM_HEADER + "0.5\n");

    client.connect();
  }

  private static InProcessSlimClient client(String header) {
    InProcessSlimClient client = new InProcessSlimClient("slim", null, new CompositeExecutionLogListener(), null);
    client.readFrom(new SlimStreamReader(new ByteArrayInputStream(header.getBytes())));
    return client;
  }
}