package agnesse.fixtures;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class McpSessionTest {

  @Test
  public void theGuidePageCanListRunAndReadResults() throws Exception {
    Path parent = Files.createTempDirectory("agnesse-mcp");
    Path home = parent.resolve("a:b");
    Files.createDirectories(home);
    McpSession session = new McpSession(home.toFile());
    try {
      assertEquals("2025-06-18", session.protocol());
      session.page("McpMixed", "suite", ".");
      session.page("McpMixed.McpPass", "test", table("hello", "hello"));
      session.taggedPage("McpMixed.McpLater", "test", "later, ", table("hello", "hello"));
      session.page("McpMixed.McpFail", "test", table("hello", "goodbye"));
      session.page("McpMixed.SetUp", "test-adjunct", ".");
      session.page("McpMixedNot", "static", ".");

      assertEquals("list_pages, run, get_results", session.tools());
      assertEquals("suite", session.typeOf("McpMixed"));
      assertEquals("test", session.typeOf("McpMixed.McpPass"));
      assertEquals("test-adjunct", session.typeOf("McpMixed.SetUp"));
      assertEquals("later", session.tagsOf("McpMixed.McpLater"));
      assertEquals("", session.tagsOf("McpMixed.McpPass"));
      assertEquals(3, session.pagesOfType("test"));
      assertEquals(1, session.pagesOfType("test-adjunct"));
      assertEquals(1, session.pagesWithTag("later"));
      assertEquals(1, session.pagesWithTag("later, missing"));
      assertEquals(0, session.pagesWithTag("Later"));
      assertEquals(5, session.pagesUnder("McpMixed"));

      String pass = session.run("McpMixed.McpPass");
      assertTrue(session.resultsForContain(pass, "\"schemaVersion\": 1"));
      assertTrue(session.resultsForContain(pass, "\"status\": \"pass\""));
      assertEquals(0, session.failuresIn(pass));

      String suite = session.run("McpMixed");
      assertEquals(1, session.failuresIn(suite));
      assertTrue(session.resultsForContain(suite, "McpFail"));

      String rerun = session.rerun("McpMixed");
      assertEquals(1, session.failuresIn(rerun));
      assertFalse(session.resultsForContain(rerun, "McpPass"));
      assertTrue(session.resultsForContain(rerun, "McpFail"));

      String tagged = session.runWithTags("McpMixed", "later");
      assertTrue(session.resultsForContain(tagged, "McpLater"));
      assertFalse(session.resultsForContain(tagged, "McpFail"));

      String excluded = session.runExcluding("McpMixed", "later");
      assertFalse(session.resultsForContain(excluded, "McpLater"));
      assertTrue(session.resultsForContain(excluded, "McpFail"));

      session.page("Solo", "test", table("hello", "hello"));
      session.page("Solo.Boom", "test", table("hello", "goodbye"));
      String alone = session.run("Solo");
      assertEquals(0, session.failuresIn(alone));
      assertFalse(session.resultsForContain(alone, "Boom"));
      String withChild = session.runAsSuite("Solo");
      assertEquals(1, session.failuresIn(withChild));
      assertTrue(session.resultsForContain(withChild, "Boom"));
      String pageOnly = session.runAsTest("McpMixed");
      assertEquals(0, session.failuresIn(pageOnly));
      assertFalse(session.resultsForContain(pageOnly, "McpFail"));

      session.taggedPage("Colon.Bird", "test", "a:b", table("hello", "hello"));
      session.page("Colon", "suite", ".");
      String encoded = session.runWithTags("Colon", "a:b");
      assertTrue(session.resultsForContain(encoded, "Bird"));

      try {
        session.run("McpMixed.SetUp");
        fail("a test-adjunct page does not run");
      } catch (IllegalStateException e) {
        assertTrue(e.getMessage(), e.getMessage().contains("error loading page"));
      }
      try {
        session.typeOf("Missing");
        fail("missing page");
      } catch (IllegalStateException e) {
        assertTrue(e.getMessage(), e.getMessage().contains("no such page"));
      }
    } finally {
      session.end();
      Files.deleteIfExists(parent);
    }
  }

  @Test
  public void thePublicConstructorStartsAServer() throws Exception {
    McpSession session = new McpSession();
    try {
      session.page("Only", "static", null);
      session.taggedPage("Tags", "static", null, null);
      session.taggedPage("Empty", "static", "", ".");
      session.taggedPage("Hole", "static", "red,,blue", "line<br>two<br/>three");
      assertEquals("static", session.typeOf("Only"));
      assertEquals("", session.tagsOf("Tags"));
      assertEquals("", session.tagsOf("Empty"));
      assertEquals("red, blue", session.tagsOf("Hole"));
      try {
        session.typeOf("");
        fail("blank path");
      } catch (IllegalStateException e) {
        assertTrue(e.getMessage(), e.getMessage().contains("no such page"));
      }
    } finally {
      session.end();
    }
  }

  @Test(timeout = 10000)
  public void aFileHomeClosesThePipe() throws Exception {
    File file = File.createTempFile("agnesse-mcp", ".home");
    try {
      new McpSession(file);
      fail("home is a file");
    } catch (IllegalStateException e) {
      assertEquals("mcp closed", e.getMessage());
    } finally {
      assertTrue(file.delete());
    }
  }

  @Test
  public void mcpRefusesAHomeThatIsAFile() throws Exception {
    File file = File.createTempFile("agnesse-mcp", ".home");
    try {
      McpSession.launch(file, InputStream.nullInputStream(), OutputStream.nullOutputStream());
      fail("home is a file");
    } catch (IllegalStateException e) {
      assertEquals("mcp exited 1", e.getMessage());
    } finally {
      assertTrue(file.delete());
    }
  }

  @Test
  public void closingABrokenStreamReportsTheCause() {
    McpSession.close(OutputStream.nullOutputStream());
    assertEquals("nope", closeMessage(new IOException("nope")));
    assertEquals("java.io.IOException", closeMessage(new IOException((String) null)));
  }

  @Test
  public void aLiveServerThreadIsReported() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    Thread thread = new Thread(() -> {
      started.countDown();
      try {
        release.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    thread.setDaemon(true);
    thread.start();
    assertTrue(started.await(10, TimeUnit.SECONDS));
    try {
      McpSession.stopped(thread);
      fail("still running");
    } catch (IllegalStateException e) {
      assertEquals("mcp did not exit", e.getMessage());
    } finally {
      release.countDown();
      thread.join(10_000);
    }
    McpSession.stopped(thread);
  }

  private static String closeMessage(IOException cause) {
    try {
      McpSession.close(new OutputStream() {
        @Override
        public void write(int b) {
        }

        @Override
        public void close() throws IOException {
          throw cause;
        }
      });
      fail("close should fail");
      return "";
    } catch (IllegalStateException e) {
      return e.getMessage();
    }
  }

  private static String table(String sent, String expected) {
    return """
      | script | agnesse.fixtures.EchoFixture |
      | --- | --- | --- |
      | check | echo | %s | %s |
      """.formatted(sent, expected);
  }
}
