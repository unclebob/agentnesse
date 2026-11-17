// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.socketservice;

import static agnesse.socketservice.SocketServer.StreamUtility.GetBufferedReader;
import static agnesse.socketservice.SocketServer.StreamUtility.GetPrintStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Before;
import org.junit.Test;

public class SocketServiceTest {
  private int connections = 0;
  private SocketServer connectionCounter;
  private SocketService ss;
  private int port;

  public SocketServiceTest() {
    connectionCounter = new SocketServer() {
      @Override
      public void serve(Socket s) {
        connections++;
      }
    };
  }

  @Before
  public void setUp() throws Exception {
    connections = 0;
  }

  @Test
  public void testNoConnections() throws Exception {
    SocketServer connectionCounter1 = this.connectionCounter;
    ss = createSocketService(connectionCounter1);
    ss.close();
    assertEquals(0, connections);
  }

  public SocketService createSocketService(SocketServer socketServer) throws IOException {
    ServerSocket socket = new PlainServerSocketFactory().createServerSocket(0);
    port = socket.getLocalPort();
    return new SocketService(socketServer, false, socket);
  }

  @Test
  public void testOneConnection() throws Exception {
    ss = createSocketService(connectionCounter);
    connect(port);
    ss.close();
    assertEquals(1, connections);
  }

  @Test
  public void testManyConnections() throws Exception {
    ss = createSocketService(connectionCounter);
    for (int i = 0; i < 10; i++)
      connect(port);
    ss.close();
    assertEquals(10, connections);
  }

  @Test
  public void testSendMessage() throws Exception {
    ss = createSocketService(new HelloService());
    Socket s = new Socket("localhost", port);
    BufferedReader br = GetBufferedReader(s);
    String answer = br.readLine();
    s.close();
    ss.close();
    assertEquals("Hello", answer);
  }

  @Test
  public void keepsAcceptingAfterAServeFailure() throws Exception {
    final int[] calls = {0};
    ss = createSocketService(new SocketServer() {
      @Override
      public void serve(Socket s) throws IOException {
        calls[0]++;
        s.close();
        throw new IOException("boom");
      }
    });
    connect(port);
    connect(port);
    ss.close();
    assertEquals(2, calls[0]);
  }

  @Test
  public void testReceiveMessage() throws Exception {
    ss = createSocketService(new EchoService());
    Socket s = new Socket("localhost", port);
    BufferedReader br = GetBufferedReader(s);
    PrintStream ps = GetPrintStream(s);
    ps.println("MyMessage");
    String answer = br.readLine();
    s.close();
    ss.close();
    assertEquals("MyMessage", answer);
  }

  private void connect(int port) {
    try {
      Socket s = new Socket("localhost", port);
      sleep(30);
      s.close();
    }
    catch (IOException e) {
      fail("could not connect");
    }
  }

  private void sleep(int ms) {
    try {
      Thread.sleep(ms);
    }
    catch (InterruptedException e) {
    }
  }
}

class HelloService implements SocketServer {
  @Override
  public void serve(Socket s) {
    try {
      PrintStream ps = GetPrintStream(s);
      ps.println("Hello");
    }
    catch (IOException e) {
    }
  }
}

class EchoService implements SocketServer {
  @Override
  public void serve(Socket s) {
    try {
      PrintStream ps = GetPrintStream(s);
      BufferedReader br = GetBufferedReader(s);
      String token = br.readLine();
      ps.println(token);
    }
    catch (IOException e) {
    }
  }
}
