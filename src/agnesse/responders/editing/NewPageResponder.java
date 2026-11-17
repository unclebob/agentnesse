package agnesse.responders.editing;

import static agnesse.wiki.PageData.PAGE_TYPE_ATTRIBUTES;

import agnesse.AgNesseContext;
import agnesse.Responder;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.html.template.HtmlPage;
import agnesse.html.template.PageTitle;
import agnesse.responders.ErrorResponder;
import agnesse.responders.NotFoundResponder;
import agnesse.markdown.FrontMatter;
import agnesse.wiki.PageCrawler;
import agnesse.wiki.PageData;
import agnesse.wiki.PageType;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;
import agnesse.wiki.WikiPageUtil;

public class NewPageResponder implements Responder {
  public static final String DEFAULT_PAGE_CONTENT_PROPERTY = "NewPage.default.content";
  public static final String DEFAULT_PAGE_CONTENT_PROPERTY_DEPRECATED = "newpage.default.content";
  public static final String DEFAULT_PAGE_CONTENT = "```agnesse\ncontents\n```\n";

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
  public Response makeResponse(AgNesseContext context, Request request) throws Exception {
    if (request.hasInput(EditResponder.PAGE_NAME)) {
      return createChild(context, request);
    }
    SimpleResponse response = new SimpleResponse();
    response.setContent(doMakeHtml(context, request));
    return response;
  }

  private Response createChild(AgNesseContext context, Request request) throws Exception {
    parseRequest(context, request);
    if (currentPage == null)
      return new NotFoundResponder().makeResponse(context, request);
    else if (nameIsInvalid(childName))
      return new ErrorResponder("Invalid Child Name").makeResponse(context, request);
    else if (currentPage.getPageCrawler().pageExists(PathParser.parse(childName)))
      return new ErrorResponder("Child page already exists", 409).makeResponse(context, request);

    FrontMatter matter = null;
    if (request.hasInput(EditResponder.FRONT_MATTER)) {
      try {
        matter = FrontMatter.readSubmitted(request.getInput(EditResponder.FRONT_MATTER));
      } catch (IllegalArgumentException e) {
        return redisplay(context, request, e.getMessage());
      }
    }

    WikiPage childPage = WikiPageUtil.addPage(currentPage, childPath, childContent);
    if (matter == null) {
      setAttributes(childPage);
    } else {
      PageData data = childPage.getData();
      data.setContent(childContent);
      FrontMatter.keepOrApply(data, matter, request.getInput(EditResponder.FRONT_MATTER), childName);
      childPage.commit(data);
    }
    SimpleResponse response = new SimpleResponse();
    response.redirect(currentPage.getFullPath().toString());
    return response;
  }

  private Response redisplay(AgNesseContext context, Request request, String error) throws Exception {
    SimpleResponse response = new SimpleResponse(400);
    response.setContent(form(context, request, error));
    return response;
  }

  private void parseRequest(AgNesseContext context, Request request) {
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
      childContent = "```agnesse\ncontents\n```\n";
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

  private String doMakeHtml(AgNesseContext context, Request request) {
    return form(context, request, null);
  }

  private String form(AgNesseContext context, Request request, String error) {
    HtmlPage html = context.pageFactory.newPage();
    html.setTitle("New page");

    html.setPageTitle(new PageTitle("New Page", PathParser.parse(request.getResource())));
    html.setMainTemplate("editPage");
    makeEditForm(html, context, request, error);

    return html.html(request);
  }

  private void makeEditForm(HtmlPage html, AgNesseContext context, Request request, String error) {
    html.put("resource", request.getResource());
    html.put("isNewPage", true);

    if (error != null) {
      html.put("pageName", request.getInput(EditResponder.PAGE_NAME));
      html.put(PAGE_TYPES, PAGE_TYPE_ATTRIBUTES);
      html.put("selectedType", request.getInput(PAGE_TYPE));
      EditForm.putFields(html, request.getInput(EditResponder.CONTENT_INPUT_NAME),
        request.getInput(EditResponder.FRONT_MATTER), error);
      return;
    }

    WikiPage parentWikiPage = getParentWikiPage(context, request);
    if (request.hasInput(PAGE_TYPE)) {
      String requestedType = request.getInput(PAGE_TYPE);
      PageType.fromString(requestedType);
      html.put(EditResponder.PAGE_TYPE, requestedType);
      EditForm.putFields(html, getDefaultContent(parentWikiPage), FrontMatter.defaultJson(requestedType), null);
    } else {
      html.put(PAGE_TYPES, PAGE_TYPE_ATTRIBUTES);
      EditForm.putFields(html, getDefaultContent(parentWikiPage), FrontMatter.defaultJson("Default"), null);
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

  private WikiPage getParentWikiPage(AgNesseContext context, Request request) {
    WikiPagePath parentPath = PathParser.parse(request.getResource());
    PageCrawler crawler = context.getRootPage().getPageCrawler();
    return crawler.getPage(parentPath);
  }
}
