package util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandLineTest {
  @Test
  public void readsShortAndLongOptionsAndTheirArguments() {
    CommandLine line = new CommandLine("[-v][-p port][--listen address]");
    assertTrue(line.parse(new String[] {"-v", "-p", "80", "--listen", "127.0.0.1"}));
    assertTrue(line.hasOption("v"));
    assertEquals("80", line.getOptionArgument("p", "port"));
    assertEquals("127.0.0.1", line.getOptionArgument("listen", "address"));
  }

  @Test
  public void rejectsAnUnknownOption() {
    assertFalse(new CommandLine("[-v]").parse(new String[] {"-x"}));
  }

  @Test
  public void rejectsAMissingArgument() {
    assertFalse(new CommandLine("[-p port]").parse(new String[] {"-p"}));
  }

  @Test
  public void rejectsAnExtraArgument() {
    assertFalse(new CommandLine("[-v]").parse(new String[] {"-v", "extra"}));
  }

  @Test
  public void anUnknownOptionNameIsInactive() {
    CommandLine line = new CommandLine("[-p port]");
    assertTrue(line.parse(new String[] {"-p", "9"}));
    assertFalse(line.hasOption("missing"));
    assertEquals(null, line.getOptionArgument("missing", "port"));
    assertEquals(null, line.getOptionArgument("missing", "port", "fallback"));
    assertEquals("fallback", line.getOptionArgument("p", "absent", "fallback"));
  }

  @Test
  public void readsALaterArgumentByName() {
    CommandLine line = new CommandLine("[-p port host]");
    assertTrue(line.parse(new String[] {"-p", "9", "localhost"}));
    assertEquals("9", line.getOptionArgument("p", "port"));
    assertEquals("localhost", line.getOptionArgument("p", "host"));
  }

  @Test
  public void rejectsAnOptionWhileAnArgumentIsStillRequired() {
    assertFalse(new CommandLine("[-p port][-v]").parse(new String[] {"-p", "-v"}));
  }
}
