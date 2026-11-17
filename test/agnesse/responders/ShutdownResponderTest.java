// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import agnesse.socketservice.PlainServerSocketFactory;
import java.net.ServerSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import agnesse.AgNesseContext;
import agnesse.http.MockRequest;
import agnesse.http.RequestBuilder;
import agnesse.http.ResponseParser;
import agnesse.testutil.AgNesseUtil;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    Thread.sleep(200);
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

    Thread.sleep(500);

    assertTrue(doneShuttingDown);
    assertFalse(context.agNesse.isRunning());
  }

}
