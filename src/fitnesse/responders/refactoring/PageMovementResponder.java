package fitnesse.responders.refactoring;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.html.HtmlUtil;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.responders.ErrorResponder;
import fitnesse.responders.NotFoundResponder;
import fitnesse.wiki.*;
import fitnesse.wiki.refactoring.ChangeReference;
import fitnesse.wiki.refactoring.OutboundReferences;
import fitnesse.wiki.refactoring.ReferenceRenamingTraverser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class PageMovementResponder implements Responder {

  protected String oldNameOfPageToBeMoved;
  protected WikiPage oldRefactoredPage;
  protected WikiPage newParentPage;
  protected WikiPagePath newParentPath;

  protected abstract boolean getAndValidateNewParentPage(FitNesseContext context, Request request);

  protected abstract boolean getAndValidateRefactoringParameters(Request request);

  protected abstract ChangeReference getReferenceRenamer();

  protected abstract String getNewPageName();

  protected abstract String getErrorMessageHeader();

  protected abstract void execute() throws RefactorException;

  @Override
  public Response makeResponse(FitNesseContext context, Request request) throws Exception {
    if (!getAndValidateRefactoredPage(context, request)) {
      return new NotFoundResponder().makeResponse(context, request);
    }

    if (!getAndValidateNewParentPage(context, request)) {
      return makeErrorMessageResponder(newParentPath == null ? "null" : newParentPath + " does not exist.").makeResponse(context, request);
    }

    if (!getAndValidateRefactoringParameters(request)) {
      return makeErrorMessageResponder("").makeResponse(context, request);
    }

    if (targetPageExists()) {
      return makeErrorMessageResponder(makeLink(getNewPageName()) + " already exists").makeResponse(context, request);
    }

    rewriteThenMove(context, request);

    SimpleResponse response = new SimpleResponse();
    response.redirect(createRedirectionUrl(newParentPage, getNewPageName()));

    return response;
  }

  private void rewriteThenMove(FitNesseContext context, Request request) throws Exception {
    if (!request.hasInput("refactorReferences")) {
      execute();
      return;
    }
    Map<String, String> original = snapshot(context.getRootPage());
    try {
      rewriteReferences(context);
      execute();
    } catch (Exception failure) {
      restore(context.getRootPage(), original);
      throw failure;
    }
  }

  private void rewriteReferences(FitNesseContext context) {
    if (newParentPage != oldRefactoredPage.getParent()) {
      WikiPagePath movedPath = oldRefactoredPage.getFullPath();
      OutboundReferences.absolutize(oldRefactoredPage);
      oldRefactoredPage = context.getRootPage().getPageCrawler().getPage(movedPath);
    }
    ReferenceRenamingTraverser.renameReferences(context.getRootPage(), getReferenceRenamer());
  }

  private static Map<String, String> snapshot(WikiPage root) {
    Map<String, String> contents = new LinkedHashMap<>();
    root.getPageCrawler().traverse(page -> contents.put(key(page), page.getData().getContent()), new NoPruningStrategy());
    return contents;
  }

  private static void restore(WikiPage root, Map<String, String> original) {
    root.getPageCrawler().traverse(page -> writeBack(page, original.get(key(page))), new NoPruningStrategy());
  }

  private static void writeBack(WikiPage page, String content) {
    if (content == null || content.equals(page.getData().getContent())) {
      return;
    }
    PageData data = page.getData();
    data.setContent(content);
    page.commit(data);
  }

  private static String key(WikiPage page) {
    String rendered = PathParser.render(page.getFullPath());
    return rendered == null || rendered.isEmpty() ? "root" : rendered;
  }

  protected boolean getAndValidateRefactoredPage(FitNesseContext context, Request request) {

    oldNameOfPageToBeMoved = request.getResource();

    WikiPagePath path = PathParser.parse(oldNameOfPageToBeMoved);
    oldRefactoredPage = context.getRootPage().getPageCrawler().getPage(path);
    return (oldRefactoredPage != null);
  }

  private Responder makeErrorMessageResponder(String message) {
    return new ErrorResponder(getErrorMessageHeader() + " " + message);
  }

  private boolean targetPageExists() {
    return newParentPage.hasChildPage(getNewPageName());
  }

  protected String makeLink(String page) {
    return HtmlUtil.makeLink(page, page).html();
  }

  protected String createRedirectionUrl(WikiPage newParent, String newName) {
    if(newParent.isRoot()) {
      return newName;
    }
    PageCrawler crawler = newParent.getPageCrawler();
    return PathParser.render(crawler.getFullPath().addNameToEnd(newName));
  }

  protected void movePage(WikiPage movedPage, WikiPage newParentPage, String pageName) throws RefactorException {

    PageData pageData = movedPage.getData();
    WikiPage targetPage = newParentPage.addChildPage(pageName);
    targetPage.commit(pageData);
    moveChildren(movedPage, targetPage);
    movedPage.remove();
  }

  protected void moveChildren(WikiPage movedPage, WikiPage newParentPage) throws RefactorException {
    List<WikiPage> children = movedPage.getChildren();
    for (WikiPage page : children) {
      movePage(page, newParentPage, page.getName());
    }
  }

}
