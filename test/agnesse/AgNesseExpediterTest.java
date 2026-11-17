// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse;

import agnesse.http.MockRequest;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.ResponseParser;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;
import agnesse.util.MockSocket;
import agnesse.util.SerialExecutorService;
import agnesse.wiki.WikiPage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

public class AgNesseExpediterTest {
  public static final int REQUEST_PARSING_TIME_LIMIT = 40;
  private AgNesseExpediter expediter;
  private MockSocket socket;
  private AgNesseContext context;
  private PipedInputStream clientInput;
  private PipedOutputStream clientOutput;
  private ResponseParser response;
  private java.util.concurrent.ExecutorService executorService;

  @Before
  public void setUp() throws Exception {
    context = AgNesseUtil.makeTestContext();
    executorService = Executors.newFixedThreadPool(2);
    WikiPage root = context.getRootPage();
    root.addChildPage("FrontPage");
    socket = new MockSocket();
    expediter = new AgNesseExpediter(socket, context, executorService);
  }

  @Test(expected = IOException.class)
  public void testClosedSocketMidResponse() throws Exception {
    MockRequest request = new MockRequest();
    Response response = expediter.createGoodResponse(request);
    socket.close();
    response.sendTo(expediter);
  }

  @Test
  public void aResetConnectionWhileSendingIsIgnored() throws Exception {
    MockSocket reset = new MockSocket("GET /root HTTP/1.1\r\n\r\n") {
      @Override
      public synchronized OutputStream getOutputStream() {
        return new OutputStream() {
          @Override
          public void write(int b) throws IOException {
            throw new SocketException("connection reset");
          }
        };
      }
    };
    AgNesseExpediter sender = new AgNesseExpediter(reset, context, new SerialExecutorService());
    List<String> warnings = new ArrayList<>();
    Logger log = Logger.getLogger(AgNesseExpediter.class.getName());
    Handler handler = new Handler() {
      @Override
      public void publish(LogRecord record) {
        if (record.getLevel().intValue() >= Level.WARNING.intValue())
          warnings.add(record.getMessage());
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() {
      }
    };
    log.addHandler(handler);
    try {
      sender.run();
    } finally {
      log.removeHandler(handler);
    }

    assertTrue(reset.isClosed());
    assertFalse(warnings.contains("Unexpected exception"));
  }

  @Test
  public void testIncompleteRequestsTimeOut() throws Exception {
    final AgNesseExpediter sender = preparePipedAgNesseExpediter();

    Thread senderThread = makeSendingThread(sender);
    senderThread.start();
    Thread parseResponseThread = makeParsingThread();
    parseResponseThread.start();

    parseResponseThread.join(5_000);

    assertEquals(408, response.getStatus());
  }

  private AgNesseExpediter preparePipedAgNesseExpediter() throws Exception {
    PipedInputStream socketInput = new PipedInputStream();
    clientOutput = new PipedOutputStream(socketInput);
    clientInput = new PipedInputStream();
    PipedOutputStream socketOutput = new PipedOutputStream(clientInput);
    MockSocket socket = new MockSocket(socketInput, socketOutput);
    return new AgNesseExpediter(socket, context, executorService, REQUEST_PARSING_TIME_LIMIT);
  }

  @Test
  public void testCompleteRequest() throws Exception {
    final AgNesseExpediter sender = preparePipedAgNesseExpediter();

    Thread senderThread = makeSendingThread(sender);
    senderThread.start();
    Thread parseResponseThread = makeParsingThread();
    parseResponseThread.start();

    clientOutput.write("GET /root HTTP/1.1\r\n\r\n".getBytes());
    clientOutput.flush();

    parseResponseThread.join();

    assertEquals(200, response.getStatus());
  }

  @Test
  public void testBadRequest() throws Exception {
    final AgNesseExpediter sender = preparePipedAgNesseExpediter();

    Thread senderThread = makeSendingThread(sender);
    senderThread.start();
    Thread parseResponseThread = makeParsingThread();
    parseResponseThread.start();

    clientOutput.write("\r\n\r\n".getBytes());
    clientOutput.flush();

    parseResponseThread.join();

    assertEquals(400, response.getStatus());
    assertFalse("Body contained exception which should have been removed",
      response.getBody().contains("java.util.concurrent.ExecutionException"));
  }

  @Test
  public void slowButCompleteRequestCanTimeOut() throws Exception {
    final AgNesseExpediter sender = preparePipedAgNesseExpediter();

    Thread senderThread = makeSendingThread(sender);
    senderThread.start();
    Thread parseResponseThread = makeParsingThread();
    parseResponseThread.start();

    boolean pipeHasBeenClosed = false;

    // 22 bytes * 5ms sleep => 110ms for the whole request. Time limit is 40ms.
    byte[] bytes = "GET /root HTTP/1.1\r\n\r\n".getBytes();
    try {
      for (byte aByte : bytes) {
        clientOutput.write(aByte);
        clientOutput.flush();
        Thread.sleep(5);
      }
    }
    catch (IOException pipedClosed) {
      pipeHasBeenClosed = true;
    }

    parseResponseThread.join();

    assertTrue(pipeHasBeenClosed);
    assertEquals(408, response.getStatus());
    assertThat(response.getBody(), containsString("The client request has been unproductive for too long. It has timed out and will no longer be processed."));
  }

  private Thread makeSendingThread(final AgNesseExpediter sender) {
    return new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          sender.run();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  private Thread makeParsingThread() {
    return new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          response = new ResponseParser(clientInput);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

}
