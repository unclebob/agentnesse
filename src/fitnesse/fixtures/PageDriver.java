// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.fixtures;

import fitnesse.FitNesseExpediter;
import fitnesse.http.MockRequest;
import fitnesse.http.MockResponseSender;
import fitnesse.util.MockSocket;
import fitnesse.util.SerialExecutorService;
import fitnesse.wiki.PageCrawler;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPagePath;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.util.NodeList;

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
    MockRequest request = new MockRequest();
    if (username != null)
      request.setCredentials(username, password);
    request.addInput("responder", "saveData");
    request.addInput("pageContent", contents);
    request.parseRequestUri("/" + pageName);
    WikiPagePath path = PathParser.parse(request.getResource()); // uri;
    FitnesseFixtureContext.page = FitnesseFixtureContext.context.getRootPage().getPageCrawler().getPage(path);
    FitNesseExpediter expediter = new FitNesseExpediter(new MockSocket(""), FitnesseFixtureContext.context, new SerialExecutorService());
    FitnesseFixtureContext.response = expediter.createGoodResponse(request);
    FitnesseFixtureContext.sender = new MockResponseSender();
    FitnesseFixtureContext.sender.doSending(FitnesseFixtureContext.response);
    return FitnesseFixtureContext.response.getStatus();
  }

  public int requestPageSaveWithContents(String pageName, String contents) throws Exception {
    return requestPageSaveWithContentsByUserAndPassword(pageName, contents, null, null);
  }

  public String lastModifiedOfPage(String pageName) throws Exception {
    WikiPage root = FitnesseFixtureContext.context.getRootPage();
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
    WikiPage root = FitnesseFixtureContext.context.getRootPage();
    WikiPagePath pagePath = PathParser.parse(pageName);
    WikiPage thePage = root.getPageCrawler().getPage(pagePath);
    PageData data = thePage.getData();
    data.setAttribute(attrName, "true");
    thePage.commit(data);
  }

  public boolean contentMatches(String pattern) throws Exception {
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
    examiner.type = "contents";
    examiner.extractValueFromResponse();
    return examiner.getValue().contains(subString);
  }

  public String content() throws Exception {
    return requester.html();
  }

  public int countOfTagWithIdPrefix(String tag, String idPrefix) throws Exception {
    NodeFilter filter =
      new AndFilter(
        new TagNameFilter(tag),
        new HasAttributePrefixFilter("id", idPrefix));
    return getMatchingTags(filter).size();
  }

  private NodeList getMatchingTags(NodeFilter filter) throws Exception {
    String html = examiner.html();
    Parser parser = new Parser(new Lexer(new Page(html)));
    NodeList list = parser.parse(null);
    NodeList matches = list.extractAllNodesThatMatch(filter, true);
    return matches;
  }

  private static class HasAttributePrefixFilter extends org.htmlparser.filters.HasAttributeFilter {
    private static final long serialVersionUID = 1L;

    public HasAttributePrefixFilter(String attribute, String prefix) {
      super(attribute, prefix);
    }

    @Override
    public boolean accept(Node node) {
      if (!(node instanceof Tag)) {
        return false;
      }

      if (mValue == null) {
        return false;
      }

      Tag tag = (Tag) node;
      if (tag.getAttributeEx(mAttribute) == null) {
        return false;
      }

      return tag.getAttributeEx(mAttribute).getValue().startsWith(mValue);
    }
  }

  public boolean pageHasAttribute(String fullPathOfPage, String attribute) throws Exception {
    PageCrawler crawler = FitnesseFixtureContext.context.getRootPage().getPageCrawler();
    WikiPage page = crawler.getPage(PathParser.parse(fullPathOfPage));
    PageData data = page.getData();
    return data.hasAttribute(attribute);
  }
}
