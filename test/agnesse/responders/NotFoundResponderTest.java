// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import static org.junit.Assert.assertEquals;
import static util.RegexTestCase.assertHasRegexp;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.http.MockRequest;
import agnesse.http.SimpleResponse;
import agnesse.testutil.AgNesseUtil;
import org.junit.Before;
import org.junit.Test;

public class NotFoundResponderTest {
  
  private AgNesseContext context;

  @Before
  public void setUp() {
    context = AgNesseUtil.makeTestContext();
  }

  @Test
  public void testResponse() throws Exception {
    MockRequest request = new MockRequest();
    request.setResource("some page");

    Responder responder = new NotFoundResponder();
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);

    assertEquals(404, response.getStatus());
    assertHasRegexp("\"error\": \"notFound\"", response.getContent());
    assertHasRegexp("No page some page", response.getContent());
  }

}
