package agnesse.fixtures;

import agnesse.AgNesseContext;
import org.junit.After;
import org.junit.Test;

import java.net.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * SetUp starts a wiki server for a suite. Two of them, in two test runs at once,
 * must not fight over one port, and AGNESSE_PORT must name the port actually bound.
 */
public class SetUpTest {
  private AgNesseContext first;

  @After
  public void stopServers() throws Exception {
    if (first != null) first.agNesse.stop();
    new TearDown();
  }

  @Test
  public void twoSetUpsRunAtOnceEachGetTheirOwnPort() throws Exception {
    new SetUp();
    first = AgNesseFixtureContext.context;
    new SetUp();
    AgNesseContext second = AgNesseFixtureContext.context;

    assertNotEquals(first.port, second.port);
    try (Socket ignored = new Socket("localhost", first.port);
         Socket alsoIgnored = new Socket("localhost", second.port)) {
      assertEquals(String.valueOf(second.port), second.getProperty("AGNESSE_PORT"));
    }
  }

  @Test
  public void pagesCanAskForThePortSetUpBound() throws Exception {
    new SetUp();

    assertEquals(AgNesseFixtureContext.context.port, new PageDriver().agnessePort());
  }
}
