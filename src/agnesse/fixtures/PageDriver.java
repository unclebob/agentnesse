// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.fixtures;

import agnesse.AgNesseExpediter;
import agnesse.http.MockRequest;
import agnesse.http.MockResponseSender;
import agnesse.util.MockSocket;
import agnesse.util.SerialExecutorService;
import agnesse.wiki.PageCrawler;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;

import java.nio.file.Files;

public class PageDriver {
  private PageCreator creator = new PageCreator();
  private ResponseRequester requester = new ResponseRequester();
  private ResponseExaminer examiner = new ResponseExaminer();

  public void createMarkdownPageWithContent(String pageName, String content) throws Exception {
    creator.pageName = pageName;
    creator.pageContents = content;
    creator.markdown();
  }

  public void createPageWithContent(String pageName, String content) throws Exception {
    createMarkdownPageWithContent(pageName, content);
  }

  public void createPageWithAuthentication(String pageName, String attributes) throws Exception {
    creator.pageAttributes = attributes;
    creator.pageContents = "nothing";
    createPageWithContent(pageName, "");
  }

  public int agnessePort() {
    return AgNesseFixtureContext.context.port;
  }

  public int requestPage(String uri) throws Exception {
    requester.uri = uri;
    requester.execute();
    return requester.status();
  }

  public int requestPageAuthenticatedByUserAndPassword(String uri, String user, String password) throws Exception {
    requester.username = user;
    requester.password = password;
    return requestPage(uri);
  }

  public int requestPageSaveWithContentsByUserAndPassword(String pageName, String contents, String username, String password) throws Exception {
    return post(pageName, contents, null, username, password);
  }

  public int requestPageSaveWithContentsAndFrontMatter(String pageName, String contents, String frontMatter) throws Exception {
    return post(pageName, contents, frontMatter, null, null);
  }

  public int requestNewPageNamedWithContentsOfTypeAndFrontMatter(String parent, String name, String contents, String type, String frontMatter) throws Exception {
    MockRequest request = new MockRequest();
    request.addInput("responder", "new");
    request.addInput("pageName", name);
    request.addInput("pageContent", contents);
    request.addInput("pageType", type);
    request.addInput("frontMatter", frontMatter);
    request.parseRequestUri("/" + parent);
    return send(request);
  }

  private int post(String pageName, String contents, String frontMatter, String username, String password) throws Exception {
    MockRequest request = new MockRequest();
    if (username != null)
      request.setCredentials(username, password);
    request.addInput("responder", "saveData");
    request.addInput("pageContent", contents);
    if (frontMatter != null)
      request.addInput("frontMatter", frontMatter);
    request.parseRequestUri("/" + pageName);
    return send(request);
  }

  private int send(MockRequest request) throws Exception {
    WikiPagePath path = PathParser.parse(request.getResource());
    AgNesseFixtureContext.page = AgNesseFixtureContext.context.getRootPage().getPageCrawler().getPage(path);
    AgNesseExpediter expediter = new AgNesseExpediter(new MockSocket(""), AgNesseFixtureContext.context, new SerialExecutorService());
    AgNesseFixtureContext.response = expediter.createGoodResponse(request);
    AgNesseFixtureContext.sender = new MockResponseSender();
    AgNesseFixtureContext.sender.doSending(AgNesseFixtureContext.response);
    return AgNesseFixtureContext.response.getStatus();
  }

  public int requestPageSaveWithContents(String pageName, String contents) throws Exception {
    return requestPageSaveWithContentsByUserAndPassword(pageName, contents, null, null);
  }

  public String lastModifiedOfPage(String pageName) throws Exception {
    WikiPage root = AgNesseFixtureContext.context.getRootPage();
    WikiPagePath pagePath = PathParser.parse(pageName);
    WikiPage thePage = root.getPageCrawler().getPage(pagePath);
    PageData data = thePage.getData();
    return data.getAttribute(PageData.LAST_MODIFYING_USER);
  }

  public void makeATestPage(String pageName) throws Exception {
	onPageSetAttribute(pageName, "Test");
  }

  public void makeASuitePage(String pageName) throws Exception {
	onPageSetAttribute(pageName, "Suite");
  }

  private void onPageSetAttribute(String pageName, String attrName) {
    WikiPage root = AgNesseFixtureContext.context.getRootPage();
    WikiPagePath pagePath = PathParser.parse(pageName);
    WikiPage thePage = root.getPageCrawler().getPage(pagePath);
    PageData data = thePage.getData();
    data.setAttribute(attrName, "true");
    thePage.commit(data);
  }

  public boolean contentMatches(String pattern) throws Exception {
    JsonBody json = responseJson();
    if (json.isJson()) {
      return json.matches(pattern);
    }
    examiner.type = "contents";
    examiner.pattern = pattern;
    return examiner.matches();
  }

  public String extractMatch(String pattern, String type, int group) throws Exception {
    examiner.type = type;
    examiner.pattern = pattern;
    return examiner.found(group);
  }

  public boolean contentContains(String subString) throws Exception {
    JsonBody json = responseJson();
    if (json.isJson()) {
      return json.contains(subString);
    }
    examiner.type = "contents";
    examiner.extractValueFromResponse();
    return examiner.getValue().contains(subString);
  }

  public boolean sourceContains(String pageName, String text) {
    return pageSource(pageName).contains(text);
  }

  /** The shipped wiki file, not the in-memory wiki the acceptance suite builds. */
  public boolean bundledSourceContains(String pageName, String text) throws Exception {
    String relative = pageName.replace('.', '/') + ".md";
    String source = Files.readString(java.nio.file.Path.of("AgNesseRoot", relative));
    return text != null && source.contains(text);
  }

  public String pageSource(String pageName) {
    WikiPage page = AgNesseFixtureContext.context.getRootPage().getPageCrawler().getPage(PathParser.parse(pageName));
    return page.getData().getContent();
  }

  private JsonBody responseJson() throws Exception {
    return JsonBody.parse(AgNesseFixtureContext.sender.sentData());
  }

  public String content() throws Exception {
    return requester.html();
  }

  public boolean pageHasAttribute(String fullPathOfPage, String attribute) throws Exception {
    PageCrawler crawler = AgNesseFixtureContext.context.getRootPage().getPageCrawler();
    WikiPage page = crawler.getPage(PathParser.parse(fullPathOfPage));
    PageData data = page.getData();
    return data.hasAttribute(attribute);
  }
}
