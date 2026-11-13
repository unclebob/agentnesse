package fitnesse.responders.editing;

import static fitnesse.wiki.PageData.PAGE_TYPE_ATTRIBUTES;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.html.template.HtmlPage;
import fitnesse.html.template.PageTitle;
import fitnesse.responders.ErrorResponder;
import fitnesse.responders.NotFoundResponder;
import fitnesse.wiki.PageCrawler;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PageType;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPagePath;
import fitnesse.wiki.WikiPageUtil;

public class NewPageResponder implements Responder {
  public static final String DEFAULT_PAGE_CONTENT_PROPERTY = "NewPage.default.content";
  public static final String DEFAULT_PAGE_CONTENT_PROPERTY_DEPRECATED = "newpage.default.content";
  public static final String DEFAULT_PAGE_CONTENT = "```agentnesse\ncontents\n```\n";

  public static final String PAGE_TYPE = "pageType";
  public static final String PAGE_TYPES = "pageTypes";

  private WikiPage currentPage;
  private String childName;
  private WikiPagePath childPath;
  private String childContent;
  private String pageType;
  private String helpText;
  private String suites;
  private String user;

  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    if (request.hasInput(EditResponder.PAGE_NAME)) {
      return createChild(context, request);
    }
    SimpleResponse response = new SimpleResponse();
    response.setContent(doMakeHtml(context, request));
    return response;
  }

  private Response createChild(FitNesseContext context, Request request) throws Exception {
    parseRequest(context, request);
    if (currentPage == null)
      return new NotFoundResponder().makeResponse(context, request);
    else if (nameIsInvalid(childName))
      return new ErrorResponder("Invalid Child Name").makeResponse(context, request);
    else if (currentPage.getPageCrawler().pageExists(PathParser.parse(childName)))
      return new ErrorResponder("Child page already exists", 409).makeResponse(context, request);

    WikiPage childPage = WikiPageUtil.addPage(currentPage, childPath, childContent);
    setAttributes(childPage);
    SimpleResponse response = new SimpleResponse();
    response.redirect(currentPage.getFullPath().toString());
    return response;
  }

  private void parseRequest(FitNesseContext context, Request request) {
    user = request.getAuthorizationUsername();
    childName = request.getInput(EditResponder.PAGE_NAME);
    childName = childName == null ? "null" : childName;
    childPath = PathParser.parse(childName);
    WikiPagePath currentPagePath = PathParser.parse(request.getResource());
    PageCrawler pageCrawler = context.getRootPage().getPageCrawler();
    currentPage = pageCrawler.getPage(currentPagePath);
    pageType = request.getInput(EditResponder.PAGE_TYPE);
    childContent = request.getInput(EditResponder.CONTENT_INPUT_NAME);
    helpText = request.getInput(EditResponder.HELP_TEXT);
    suites = request.getInput(EditResponder.SUITES);
    if (childContent == null)
      childContent = "```agentnesse\ncontents\n```\n";
    if (pageType == null)
      pageType = "Default";
  }

  private boolean nameIsInvalid(String name) {
    if (name.equals(""))
      return true;
    return !PathParser.isSingleWikiWord(name);
  }

  private void setAttributes(WikiPage childPage) {
    PageData childPageData = childPage.getData();
    if (pageType.equals("Static")) {
      childPageData.getProperties().remove("Test");
      childPageData.getProperties().remove("Suite");
    } else if ("Test".equals(pageType) || "Suite".equals(pageType)) {
      childPageData.getProperties().remove("Test");
      childPageData.getProperties().remove("Suite");
      childPageData.setAttribute(pageType);
    }
    childPageData.setOrRemoveAttribute(PageData.PropertyHELP, helpText);
    childPageData.setOrRemoveAttribute(PageData.PropertySUITES, suites);
    childPageData.setOrRemoveAttribute(PageData.LAST_MODIFYING_USER, user);
    childPage.commit(childPageData);
  }

  private String doMakeHtml(FitNesseContext context, Request request) {
    HtmlPage html = context.pageFactory.newPage();
    html.setTitle("New page");

    html.setPageTitle(new PageTitle("New Page", PathParser.parse(request.getResource())));
    html.setMainTemplate("editPage");
    makeEditForm(html, context, request);

    return html.html(request);
  }

  private void makeEditForm(HtmlPage html, FitNesseContext context, Request request) {
    html.put("resource", request.getResource());

    html.put("isNewPage", true);
    html.put(EditResponder.HELP_TEXT, "");

    WikiPage parentWikiPage = getParentWikiPage(context, request);
    if (request.hasInput(PAGE_TYPE)) {
      String requestedType = request.getInput(PAGE_TYPE);
      PageType.fromString(requestedType);
      html.put(EditResponder.PAGE_TYPE, requestedType);
      html.put(EditResponder.CONTENT_INPUT_NAME, getDefaultContent(parentWikiPage));
    } else {
      html.put(PAGE_TYPES, PAGE_TYPE_ATTRIBUTES);
      html.put(EditResponder.CONTENT_INPUT_NAME, getDefaultContent(parentWikiPage));
    }
  }

  public static String getDefaultContent(WikiPage page) {
    String content = page.getVariable(DEFAULT_PAGE_CONTENT_PROPERTY);
    content = content == null ? page.getVariable(DEFAULT_PAGE_CONTENT_PROPERTY_DEPRECATED) : content;
    if (content == null) {
      content = DEFAULT_PAGE_CONTENT;
    }
    return content;
  }

  private WikiPage getParentWikiPage(FitNesseContext context, Request request) {
    WikiPagePath parentPath = PathParser.parse(request.getResource());
    PageCrawler crawler = context.getRootPage().getPageCrawler();
    return crawler.getPage(parentPath);
  }
}
