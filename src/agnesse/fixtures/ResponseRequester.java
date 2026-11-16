// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.fixtures;

import agnesse.AgNesseExpediter;
import agnesse.html.HtmlUtil;
import agnesse.http.MockRequest;
import agnesse.http.MockResponseSender;
import agnesse.util.MockSocket;
import agnesse.util.SerialExecutorService;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPagePath;

public class ResponseRequester {
  public String uri;
  public String username;
  public String password;

  protected MockRequest request;

  public boolean valid() {
    return status() == 200;
  }

  public void execute() throws Exception {
    setRequest(new MockRequest());

    details();

    if (username != null)
      request.setCredentials(username, password);

    request.parseRequestUri("/" + uri);
    WikiPagePath path = PathParser.parse(request.getResource()); // uri;
    AgNesseFixtureContext.page = AgNesseFixtureContext.context.getRootPage().getPageCrawler().getPage(path);
    AgNesseExpediter expediter = new AgNesseExpediter(new MockSocket(""), AgNesseFixtureContext.context, new SerialExecutorService());
    AgNesseFixtureContext.response = expediter.createGoodResponse(request);
    AgNesseFixtureContext.sender = new MockResponseSender();
    AgNesseFixtureContext.sender.doSending(AgNesseFixtureContext.response);
  }

  public int status() {
    int status = AgNesseFixtureContext.response.getStatus();
    return status;
  }

  public String contents() throws Exception {
    return "<pre>" + HtmlUtil.escapeHTML(AgNesseFixtureContext.sender.sentData()) + "</pre>";
  }

  public String html() throws Exception {
    return AgNesseFixtureContext.sender.sentData();
  }

  protected void details() {
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setRequest(MockRequest request) {
    this.request = request;
  }
}
