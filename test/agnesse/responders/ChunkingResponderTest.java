// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static util.RegexTestCase.assertNotSubString;
import static util.RegexTestCase.assertSubString;

import agnesse.AgNesseContext;
import agnesse.http.ChunkedResponse;
import agnesse.http.MockRequest;
import agnesse.http.MockResponseSender;
import agnesse.http.Response;
import agnesse.testutil.AgNesseUtil;
import org.junit.Test;

import java.net.SocketException;

public class ChunkingResponderTest {

  private final AgNesseContext context = AgNesseUtil.makeTestContext();

  private Exception exception;
  private ChunkingResponder responder = new ChunkingResponder() {
    @Override
    protected void doSending() throws Exception {
      throw exception;
    }
  };

  @Test
  public void testException() throws Exception {
    exception = new Exception("test exception");
    ChunkedResponse response = (ChunkedResponse)responder.makeResponse(context, new MockRequest());
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    String responseSender = sender.sentData();
    assertSubString("test exception", responseSender);
  }

  @Test
  public void chunkingShouldBeTurnedOffIfnochunkParameterIsPresent() throws Exception {
    MockRequest request = new MockRequest();
    request.addInput("nochunk", "");
    ChunkedResponse response = (ChunkedResponse) responder.makeResponse(context, request);
    assertTrue(response.isChunkingTurnedOff());
  }

  @Test
  public void turnOffChunkingDisablesChunkingForTheNextResponse() throws Exception {
    responder.turnOffChunking();
    ChunkedResponse response = (ChunkedResponse) responder.makeResponse(context, new MockRequest());
    assertTrue(response.isChunkingTurnedOff());
  }

  @Test
  public void aMissingPageIsNotFound() throws Exception {
    MockRequest request = new MockRequest();
    request.setResource("MissingPage");
    Response response = responder.makeResponse(context, request);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void aClosedSocketDoesNotWriteTheErrorIntoThePage() throws Exception {
    exception = new SocketException("connection reset");
    ChunkedResponse response = (ChunkedResponse) responder.makeResponse(context, new MockRequest());
    MockResponseSender sender = new MockResponseSender();
    response.sendTo(sender);
    assertFalse(sender.isClosed());
    assertNotSubString("connection reset", sender.sentData());
  }
}
