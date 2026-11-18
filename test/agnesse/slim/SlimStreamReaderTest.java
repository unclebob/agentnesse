package agnesse.slim;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SlimStreamReaderTest {
  @Test
  public void anEmptyStreamHasNoMessageLength() {
    assertReadFails(new byte[0], "EOF reached");
  }

  @Test
  public void aLengthMissingItsColonEndsAtEof() {
    assertReadFails("000000x".getBytes(), "EOF reached");
  }

  @Test
  public void aLengthThatIsNotANumberIsRejected() {
    assertReadFails("xxxxxx:".getBytes(), "not a number");
  }

  @Test
  public void aCountedMessageIsReturned() throws Exception {
    SlimStreamReader reader = new SlimStreamReader(new ByteArrayInputStream("000005:hello".getBytes()));

    assertEquals("hello", reader.getSlimMessage());
  }

  private static void assertReadFails(byte[] bytes, String expected) {
    SlimStreamReader reader = new SlimStreamReader(new ByteArrayInputStream(bytes));
    try {
      reader.getSlimMessage();
      fail("expected the read to fail");
    } catch (IOException e) {
      assertTrue(e.getMessage(), e.getMessage().contains(expected));
    }
  }
}
