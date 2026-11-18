package agnesse.testsystems.slim;

import agnesse.slim.SlimError;
import agnesse.slim.SlimVersion;
import agnesse.socketservice.ClientSocketFactory;
import agnesse.testsystems.CompositeExecutionLogListener;
import agnesse.testsystems.MockCommandRunner;
import agnesse.util.Clock;
import agnesse.util.DateAlteringClock;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SlimCommandRunningClientTest {
  @After
  public void restoreClock() {
    Clock.restoreDefaultClock();
    Thread.interrupted();
  }

  @Test
  public void startupReportsTheCommandFailure() {
    ScriptedRunner runner = new ScriptedRunner(false, new IOException("down"));
    SlimCommandRunningClient client = client(runner, 1, 0.3, socket("Slim -- V0.5\n"), millis -> { });

    try {
      client.start();
      fail("expected startup to fail");
    } catch (SlimError e) {
      assertTrue(e.getMessage().contains("SLiM server startup failed"));
      assertTrue(e.getMessage().contains("port already taken"));
      assertTrue(e.getCause() instanceof IOException);
    } catch (Exception e) {
      fail(e.toString());
    }
  }

  @Test
  public void aLiveServerReportsItsVersion() throws Exception {
    SlimCommandRunningClient client = started("Slim -- V0.5\n", 0.3);

    assertEquals(0.5, client.getServerVersion(), 0.0);
    assertTrue(client.isConnected());
  }

  @Test
  public void theRequiredVersionItselfIsAccepted() throws Exception {
    assertEquals(0.3, started("Slim -- V0.3\n", 0.3).getServerVersion(), 0.0);
  }

  @Test
  public void anOlderServerIsRejected() throws Exception {
    try {
      started("Slim -- V0.1\n", 0.3);
      fail("expected a version mismatch");
    } catch (SlimVersionMismatch e) {
      assertTrue(e.getMessage().contains("Expected V0.3"));
      assertTrue(e.getMessage().contains("V0.1"));
    }
  }

  @Test
  public void aMissingVersionNumberIsRejected() throws Exception {
    try {
      started("Slim -- V" + SlimCommandRunningClient.NO_SLIM_SERVER_CONNECTION_FLAG + "\n", 0.3);
      fail("expected a missing version");
    } catch (SlimVersionMismatch e) {
      assertTrue(e.getMessage().contains("did not respond with a valid version number"));
    }
  }

  @Test
  public void aHeaderThatIsNotAVersionIsRejected() throws Exception {
    try {
      started("Slim -- Vnope\n", 0.3);
      fail("expected an invalid version");
    } catch (SlimError e) {
      assertTrue(e.getMessage().contains("invalid slim version"));
      assertTrue(e.getMessage().contains("nope"));
    }
  }

  @Test
  public void aDeadRunnerIsReportedBeforeASocketIsOpened() {
    ScriptedRunner runner = new ScriptedRunner(true, null);
    AtomicInteger opened = new AtomicInteger();
    SlimCommandRunningClient client = client(runner, 1, 0.3, socket("Slim -- V0.5\n", opened), millis -> { });

    try {
      client.connect();
      fail("expected the dead runner");
    } catch (SlimError e) {
      assertEquals(0, opened.get());
      assertTrue(e.getMessage().contains("died before a connection"));
      assertTrue(e.getMessage().contains("port already taken"));
    } catch (Exception e) {
      fail(e.toString());
    }
  }

  @Test
  public void aRetryThenAConnectionDoesNotSleep() throws Exception {
    AtomicInteger opened = new AtomicInteger();
    ScriptedSocket socket = socket("Slim -- V0.5\n", opened);
    socket.failFirst = true;
    DateAlteringClock clock = new DateAlteringClock(new Date(0)).freeze();
    SlimCommandRunningClient client = client(new ScriptedRunner(false, null), 1, 0.3, socket, millis -> { });

    client.start();

    assertEquals(0.5, client.getServerVersion(), 0.0);
    assertTrue(clock.currentClockTimeInMillis() >= 0);
  }

  @Test
  public void givingUpOnTheSocketWaitsOutTheTimeoutWithoutSleeping() {
    DateAlteringClock clock = new DateAlteringClock(new Date(0)).freeze();
    AtomicInteger pauses = new AtomicInteger();
    ScriptedSocket socket = socket("Slim -- V0.5\n");
    socket.alwaysFail = true;
    SlimCommandRunningClient client = client(new ScriptedRunner(false, null), 1, 0.3, socket, millis -> {
      pauses.incrementAndGet();
      clock.elapse(millis);
    });

    try {
      client.connect();
      fail("expected the connection to time out");
    } catch (SlimError e) {
      assertEquals(21, pauses.get());
      assertTrue(e.getMessage().contains("Error connecting to SLiM server on localhost:1"));
    } catch (Exception e) {
      fail(e.toString());
    }
  }

  @Test
  public void anInterruptedWaitLeavesTheThreadInterrupted() throws Exception {
    ScriptedSocket socket = socket("Slim -- V0.5\n");
    socket.failFirst = true;
    SlimCommandRunningClient client = client(null, 1, 0.3, socket, millis -> {
      throw new InterruptedException("stop");
    });

    client.connect();

    assertTrue(Thread.currentThread().isInterrupted());
    assertEquals(0.5, client.getServerVersion(), 0.0);
  }

  @Test
  public void aSlowHeaderIsATimeout() throws Exception {
    SlimCommandRunningClient client = connected(null, 0.3, true);

    try {
      client.connect();
      fail("expected a read timeout");
    } catch (SlimError e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Timeout while reading slim header"));
    }
  }

  @Test
  public void aHeaderThatNeverArrivesTimesOutAfterOneRead() throws Exception {
    new DateAlteringClock(new Date(0)).freeze();
    AtomicInteger reads = new AtomicInteger();
    SlimCommandRunningClient client = client(new ScriptedRunner(false, null), 1, 0.3,
      new ScriptedSocket(new InputStream() {
        @Override
        public int read() {
          if (reads.incrementAndGet() == 1) {
            Clock.restoreDefaultClock();
            new DateAlteringClock(new Date(0)).freeze().elapse(1000);
            return '\n';
          }
          new DateAlteringClock(new Date(1001)).freeze();
          return -1;
        }
      }, new ByteArrayOutputStream()), millis -> { });

    try {
      client.connect();
      fail("expected the header wait to time out");
    } catch (SlimError e) {
      assertTrue(reads.get() >= 2);
      assertTrue(e.getMessage().contains("Timeout while reading slim header"));
    }
  }

  @Test
  public void aDeadRunnerIsReportedWhileWaitingForTheHeader() throws Exception {
    ScriptedRunner runner = new ScriptedRunner(false, null);
    ScriptedSocket socket = socket("nope\n");
    socket.afterCreate = runner::die;
    SlimCommandRunningClient client = client(runner, 5, 0.3, socket, millis -> { });

    try {
      client.connect();
      fail("expected the dead runner");
    } catch (SlimError e) {
      assertTrue(e.getMessage(), e.getMessage().contains("died before Header"));
      assertTrue(e.getMessage(), e.getMessage().contains("port already taken"));
    }
  }

  private static SlimCommandRunningClient started(String header, double required) throws Exception {
    SlimCommandRunningClient client = client(new ScriptedRunner(false, null), 1, required,
      new ScriptedSocket(new java.io.ByteArrayInputStream(header.getBytes()), new ByteArrayOutputStream()), millis -> { });
    client.start();
    return client;
  }

  private static SlimCommandRunningClient connected(String header, double required, boolean timeout) throws Exception {
    InputStream input = timeout ? new TimeoutStream() : new java.io.ByteArrayInputStream(header.getBytes());
    return client(new ScriptedRunner(false, null), 1, required, new ScriptedSocket(input, new ByteArrayOutputStream()), millis -> { });
  }

  private static SlimCommandRunningClient client(ScriptedRunner runner, int timeoutSeconds, double required, ScriptedSocket socket, SlimCommandRunningClient.Pause pause) {
    SlimCommandRunningClient client = new SlimCommandRunningClient(
      runner, "localhost", 1, timeoutSeconds, required, socket);
    client.pauseWith(pause);
    return client;
  }

  private static ScriptedSocket socket(String header) {
    return socket(header, new AtomicInteger());
  }

  private static ScriptedSocket socket(String header, AtomicInteger opened) {
    return new ScriptedSocket(new java.io.ByteArrayInputStream(header.getBytes()), new ByteArrayOutputStream(), opened);
  }

  private static final class ScriptedRunner extends MockCommandRunner {
    private boolean dead;
    private final IOException startError;

    private ScriptedRunner(boolean dead, IOException startError) {
      super(new CompositeExecutionLogListener());
      this.dead = dead;
      this.startError = startError;
      setCommandErrorMessage("port already taken");
    }

    private void die() {
      dead = true;
    }

    @Override
    public boolean isDead() {
      return dead;
    }

    @Override
    public void asynchronousStart() throws IOException {
      if (startError != null) throw startError;
    }
  }

  private static final class ScriptedSocket extends Socket implements ClientSocketFactory {
    private final InputStream input;
    private final OutputStream output;
    private final AtomicInteger opened;
    private int soTimeout;
    private boolean failFirst;
    private boolean alwaysFail;
    private boolean failed;
    private Runnable afterCreate;

    private ScriptedSocket(InputStream input, OutputStream output) {
      this(input, output, new AtomicInteger());
    }

    private ScriptedSocket(InputStream input, OutputStream output, AtomicInteger opened) {
      this.input = input;
      this.output = output;
      this.opened = opened;
    }

    @Override
    public Socket createSocket(String hostName, int port) throws IOException {
      opened.incrementAndGet();
      if (alwaysFail || (failFirst && !failed)) {
        failed = true;
        throw new IOException("not yet");
      }
      if (afterCreate != null) afterCreate.run();
      return this;
    }

    @Override
    public InputStream getInputStream() {
      return input;
    }

    @Override
    public OutputStream getOutputStream() {
      return output;
    }

    @Override
    public synchronized void setSoTimeout(int timeout) {
      soTimeout = timeout;
    }

    @Override
    public synchronized int getSoTimeout() {
      return soTimeout;
    }
  }

  private static final class TimeoutStream extends InputStream {
    @Override
    public int read() throws IOException {
      throw new SocketTimeoutException("slow");
    }
  }
}
