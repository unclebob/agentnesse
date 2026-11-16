// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.http.MockRequest;
import agnesse.testutil.AgNesseUtil;
import agnesse.wiki.WikiPage;
import org.junit.Before;

public abstract class ResponderTestCase {
  protected WikiPage root;
  protected MockRequest request;
  protected Responder responder;
  protected AgNesseContext context;

  @Before
  public void setUp() throws Exception {
    request = new MockRequest();
    responder = responderInstance();
    context = AgNesseUtil.makeTestContext();
    root = context.getRootPage();
  }

  // Return an instance of the Responder being tested.
  protected abstract Responder responderInstance();
}
