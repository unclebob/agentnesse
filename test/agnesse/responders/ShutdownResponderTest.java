// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import agnesse.socketservice.PlainServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import agnesse.AgNesseContext;
import agnesse.http.MockRequest;
import agnesse.http.RequestBuilder;
import agnesse.http.ResponseParser;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static util.RegexTestCase.assertSubString;

public class ShutdownResponderTest {
  private AgNesseContext context;
  private boolean doneShuttingDown;

  @Before
  public void setUp() throws Exception {
    ServerSocket socket = new PlainServerSocketFactory().createServerSocket(0);
    context = AgNesseUtil.makeTestContext(socket.getLocalPort());
    AgNesseUtil.startAgNesse(context, socket);
  }

  @After
  public void tearDown() throws Exception {
    context.agNesse.stop();
  }

  @Test
  public void testAgNesseGetsShutdown() throws Exception {
    ShutdownResponder responder = new ShutdownResponder();
    responder.makeResponse(context, new MockRequest());
    waitUntilStopped();
    assertFalse(context.agNesse.isRunning());
  }

  @Test
  public void testShutdownCalledFromServer() throws Exception {
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          RequestBuilder request = new RequestBuilder("/?responder=shutdown");
          ResponseParser.performHttpRequest("localhost", context.port, request);
          doneShuttingDown = true;
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    thread.start();
    thread.join(5_000);

    assertTrue(doneShuttingDown);
    waitUntilStopped();
    assertFalse(context.agNesse.isRunning());
  }

  @Test
  public void theShutdownPageIsReturnedWhenStopFails() throws Exception {
    AgNesseContext failing = AgNesseUtil.makeTestContext(0);
    ServerSocket socket = new ServerSocket(0) {
      @Override
      public void close() throws IOException {
        super.close();
        throw new IOException("stop failed");
      }
    };
    failing.agNesse.start(socket);
    Logger log = Logger.getLogger(ShutdownResponder.class.getName());
    List<LogRecord> records = new ArrayList<>();
    Handler handler = new Handler() {
      @Override
      public void publish(LogRecord record) {
        records.add(record);
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() {
      }
    };
    Level previous = log.getLevel();
    log.setLevel(Level.ALL);
    log.addHandler(handler);
    try {
      SimpleResponse response = (SimpleResponse) new ShutdownResponder().makeResponse(failing, new MockRequest());
      assertSubString("Shutdown", response.getContent());
      long deadline = System.nanoTime() + 2_000_000_000L;
      while (records.isEmpty() && System.nanoTime() < deadline) {
        Thread.onSpinWait();
      }
      assertEquals(1, records.size());
      assertEquals(Level.WARNING, records.get(0).getLevel());
      assertTrue(records.get(0).getThrown() instanceof IOException);
    } finally {
      log.removeHandler(handler);
      log.setLevel(previous);
    }
  }

  private void waitUntilStopped() {
    long deadline = System.nanoTime() + 5_000_000_000L;
    while (context.agNesse.isRunning()) {
      if (System.nanoTime() > deadline)
        break;
      Thread.onSpinWait();
    }
  }

}
