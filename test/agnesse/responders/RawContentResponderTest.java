// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.responders;

import static org.junit.Assert.assertEquals;
import static util.RegexTestCase.assertSubString;

import agnesse.Responder;
import agnesse.http.MockResponseSender;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPageUtil;
import org.junit.Test;

public class RawContentResponderTest extends ResponderTestCase {
  @Override
  protected Responder responderInstance() {
    return new RawContentResponder();
  }

  @Test
  public void testSimplePage() throws Exception {
    String result = getResultsUsing("simple content");
    assertSubString("simple content", result);
  }

  @Test
  public void aMissingPageIsNotFound() throws Exception {
    request.setResource("MissingPage");
    SimpleResponse response = (SimpleResponse) responder.makeResponse(context, request);
    assertEquals(404, response.getStatus());
    assertSubString("No page MissingPage", response.getContent());
  }

  @Test
  public void testNoHtmlRendered() throws Exception {
    String result = getResultsUsing("'''simple content'''");
    assertSubString("'''simple content'''", result);
  }

  private String getResultsUsing(String content) throws Exception {
    WikiPageUtil.addPage(root, PathParser.parse("SimplePage"), content);
    request.setResource("SimplePage");
    Response response = responder.makeResponse(context, request);
    MockResponseSender sender = new MockResponseSender();
    sender.doSending(response);
    String result = sender.sentData();
    return result;
  }
}
