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
import fitnesse.wiki.refactoring.ReferenceRenamingTraverser;

import java.util.List;

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

    if (request.hasInput("refactorReferences")) {
      ReferenceRenamingTraverser.renameReferences(context.getRootPage(), getReferenceRenamer());
    }
    execute();

    SimpleResponse response = new SimpleResponse();
    response.redirect(createRedirectionUrl(newParentPage, getNewPageName()));

    return response;
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
