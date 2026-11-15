package fitnesse.responders.refactoring;

import fitnesse.FitNesseContext;
import fitnesse.http.MockRequest;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.wiki.NoPruningStrategy;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MoveAtomicityTest {
  @Test
  public void targetExistsLeavesEveryPageUnchanged() throws Exception {
    FitNesseContext context = wiki();
    Map<String, String> before = contents(context.getRootPage());
    MockRequest request = moveRequest("ParentPage.PageMove", "OtherPage");
    new MovePageResponder().makeResponse(context, request);
    assertEquals(before, contents(context.getRootPage()));
    assertTrue(context.getRootPage().getChildPage("ParentPage").getChildPage("PageMove") != null);
  }

  @Test
  public void moveIntoOwnSubtreeLeavesEveryPageUnchanged() throws Exception {
    FitNesseContext context = wiki();
    Map<String, String> before = contents(context.getRootPage());
    MockRequest request = moveRequest("ParentPage.PageMove", "ParentPage.PageMove.PageKid");
    new MovePageResponder().makeResponse(context, request);
    assertEquals(before, contents(context.getRootPage()));
  }

  @Test
  public void executeFailureRestoresEveryPage() throws Exception {
    FitNesseContext context = wiki();
    Map<String, String> before = contents(context.getRootPage());
    try {
      new FailingMove().makeResponse(context, moveRequest("ParentPage.PageMove", "Elsewhere"));
      fail("execute should have thrown");
    } catch (RefactorException expected) {
      assertEquals("boom", expected.getMessage());
    }
    assertEquals(before, contents(context.getRootPage()));
    assertTrue(context.getRootPage().getChildPage("ParentPage").getChildPage("PageMove") != null);
  }

  @Test
  public void copyFailureAfterTheFirstPageRemovesTheDestinationAndRestoresContent() throws Exception {
    FitNesseContext context = wiki();
    Map<String, String> before = contents(context.getRootPage());
    try {
      new FailAfterCopies(1).makeResponse(context, moveRequest("ParentPage.PageMove", "Elsewhere"));
      fail("copy should have thrown");
    } catch (RefactorException expected) {
      assertEquals("copied", expected.getMessage());
    }
    assertEquals(before, contents(context.getRootPage()));
  }

  @Test
  public void aRandomCopyFailureLeavesTheTreeByteIdentical() throws Exception {
    Random random = new Random(8);
    for (int trial = 0; trial < 12; trial++) {
      FitNesseContext context = deeper();
      Map<String, String> before = contents(context.getRootPage());
      int failAfter = 1 + random.nextInt(3);
      try {
        new FailAfterCopies(failAfter).makeResponse(context, moveRequest("ParentPage.PageMove", "Elsewhere"));
        fail("copy should have thrown");
      } catch (RefactorException expected) {
        assertEquals("copied", expected.getMessage());
      }
      assertEquals("trial " + trial + " after " + failAfter, before, contents(context.getRootPage()));
    }
  }

  @Test
  public void passingMoveAbsolutizesOutboundReferencesAndKeepsInsideOnesRelative() throws Exception {
    FitNesseContext context = wiki();
    new MovePageResponder().makeResponse(context, moveRequest("ParentPage.PageMove", "Elsewhere"));
    String content = context.getRootPage().getChildPage("Elsewhere").getChildPage("PageMove").getData().getContent();
    assertEquals("see .ParentPage.PageStay here\n>PageKid\n", content);
  }

  private static FitNesseContext wiki() throws Exception {
    FitNesseContext context = FitNesseUtil.makeTestContext();
    WikiPage root = context.getRootPage();
    WikiPage parent = WikiPageUtil.addPage(root, PathParser.parse("ParentPage"), "");
    WikiPageUtil.addPage(parent, PathParser.parse("PageStay"), "stay");
    WikiPage mover = WikiPageUtil.addPage(parent, PathParser.parse("PageMove"), "see PageStay here\n>PageKid\n");
    WikiPageUtil.addPage(mover, PathParser.parse("PageKid"), "kid");
    WikiPage other = WikiPageUtil.addPage(root, PathParser.parse("OtherPage"), "");
    WikiPageUtil.addPage(other, PathParser.parse("PageMove"), "already");
    WikiPageUtil.addPage(root, PathParser.parse("Elsewhere"), "");
    return context;
  }

  private static FitNesseContext deeper() throws Exception {
    FitNesseContext context = wiki();
    WikiPage kid = context.getRootPage().getChildPage("ParentPage").getChildPage("PageMove").getChildPage("PageKid");
    WikiPageUtil.addPage(kid, PathParser.parse("PageGrand"), "grand");
    return context;
  }

  private static MockRequest moveRequest(String page, String destination) {
    MockRequest request = new MockRequest();
    request.setResource(page);
    request.addInput("newLocation", destination);
    request.addInput("refactorReferences", "");
    return request;
  }

  private static Map<String, String> contents(WikiPage root) {
    Map<String, String> contents = new LinkedHashMap<>();
    root.getPageCrawler().traverse(page -> contents.put(PathParser.render(page.getFullPath()), page.getData().getContent()),
      new NoPruningStrategy());
    return contents;
  }

  private static final class FailingMove extends MovePageResponder {
    @Override
    protected void execute() throws RefactorException {
      throw new RefactorException("boom");
    }
  }

  private static final class FailAfterCopies extends MovePageResponder {
    private final int failAfter;
    private int copies;

    private FailAfterCopies(int failAfter) {
      this.failAfter = failAfter;
    }

    @Override
    protected void copied() throws RefactorException {
      copies++;
      if (copies >= failAfter) {
        throw new RefactorException("copied");
      }
    }
  }
}
