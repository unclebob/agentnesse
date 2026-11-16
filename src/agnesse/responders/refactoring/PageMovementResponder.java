package agnesse.responders.refactoring;

import agnesse.FitNesseContext;
import agnesse.Responder;
import agnesse.html.HtmlUtil;
import agnesse.http.Request;
import agnesse.http.Response;
import agnesse.http.SimpleResponse;
import agnesse.responders.ErrorResponder;
import agnesse.responders.NotFoundResponder;
import agnesse.wiki.*;
import agnesse.wiki.refactoring.ChangeReference;
import agnesse.wiki.refactoring.OutboundReferences;
import agnesse.wiki.refactoring.ReferenceRenamingTraverser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
      undo(context.getRootPage(), original);
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

  private static void undo(WikiPage root, Map<String, String> original) {
    removeCopies(root, original.keySet());
    root.getPageCrawler().traverse(page -> writeBack(page, original.get(key(page))), new NoPruningStrategy());
  }

  private static void removeCopies(WikiPage root, Set<String> original) {
    List<WikiPage> extras = new ArrayList<>();
    root.getPageCrawler().traverse(page -> {
      if (!original.contains(key(page)) && original.contains(key(page.getParent()))) {
        extras.add(page);
      }
    }, new NoPruningStrategy());
    for (WikiPage extra : extras) {
      extra.remove();
    }
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
    copySubtree(movedPage, newParentPage, pageName);
    movedPage.remove();
  }

  private void copySubtree(WikiPage movedPage, WikiPage newParentPage, String pageName) throws RefactorException {
    PageData pageData = movedPage.getData();
    WikiPage targetPage = newParentPage.addChildPage(pageName);
    targetPage.commit(pageData);
    copied();
    for (WikiPage child : movedPage.getChildren()) {
      copySubtree(child, targetPage, child.getName());
    }
  }

  /** Called after each page has been copied and before its children. Tests fail a move here. */
  protected void copied() throws RefactorException {
  }

}
