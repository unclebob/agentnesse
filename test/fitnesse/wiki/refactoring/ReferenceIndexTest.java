package fitnesse.wiki.refactoring;

import fitnesse.FitNesseContext;
import fitnesse.http.MockRequest;
import fitnesse.responders.refactoring.MovePageResponder;
import fitnesse.responders.refactoring.RenamePageResponder;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReferenceIndexTest {
  @Test
  public void leavesRemoteLinksAlone() {
    String content = "[web](http://example.com/PageTwo) and [spaced](not a path)";
    String rewritten = MarkdownReferences.replace(null, content, reference -> java.util.Optional.of("Gone"));
    assertEquals(content, rewritten);
    assertEquals("[go](PageThree)", MarkdownReferences.replace(null, "[go](PageTwo)", reference -> java.util.Optional.of("PageThree")));
    assertEquals("[go](New) [go](New) [go](New)",
      MarkdownReferences.replace(null, "[go](<PageTwo) [go](>PageTwo) [go](^PageTwo)", reference -> java.util.Optional.of("New")));
    assertEquals("[x](foo/bar)", MarkdownReferences.replace(null, "[x](foo/bar)", reference -> java.util.Optional.of("Gone")));
  }

  @Test
  public void renameRewritesLinksIncludesScenariosAndChildren() throws Exception {
    FitNesseContext context = FitNesseUtil.makeTestContext();
    WikiPage root = context.getRootPage();
    WikiPage parent = WikiPageUtil.addPage(root, PathParser.parse("Parent"), "");
    WikiPage old = WikiPageUtil.addPage(parent, PathParser.parse("Old"), "Old page");
    WikiPageUtil.addPage(old, PathParser.parse("Kid"), """
      See .Parent.Old and [notes](.Parent.Old#top)
      ```agentnesse
      include .Parent.Old
      ```
      ```agentnesse
      scenarios .Parent.Old
      ```
      """);
    WikiPageUtil.addPage(parent, PathParser.parse("Other"), "Also .Parent.Old.Kid and [kid](.Parent.Old.Kid#end)");

    rename(context, "Parent.Old", "New");

    String kid = parent.getChildPage("New").getChildPage("Kid").getData().getContent();
    assertTrue(kid, kid.contains(".Parent.New"));
    assertTrue(kid, kid.contains("[notes](.Parent.New#top)"));
    assertTrue(kid, kid.contains("include .Parent.New"));
    assertTrue(kid, kid.contains("scenarios .Parent.New"));
    assertTrue(kid, !kid.contains(".Parent.Old"));
    String other = parent.getChildPage("Other").getData().getContent();
    assertTrue(other, other.contains(".Parent.New.Kid"));
    assertTrue(other, other.contains("[kid](.Parent.New.Kid#end)"));
    assertEquals(List.of(), ReferenceIndex.dangling(root));
  }

  @Test
  public void moveRewritesThePath() throws Exception {
    FitNesseContext context = FitNesseUtil.makeTestContext();
    WikiPage root = context.getRootPage();
    WikiPage from = WikiPageUtil.addPage(root, PathParser.parse("From"), "");
    WikiPageUtil.addPage(from, PathParser.parse("Page"), "body");
    WikiPageUtil.addPage(root, PathParser.parse("Dest"), "see .From.Page");
    MockRequest request = new MockRequest();
    request.setResource("From.Page");
    request.addInput("newLocation", "Dest");
    request.addInput("refactorReferences", "");
    new MovePageResponder().makeResponse(context, request);
    assertTrue(root.getChildPage("Dest").getChildPage("Page") != null);
    assertTrue(root.getChildPage("Dest").getData().getContent().contains(".Dest.Page"));
    assertEquals(List.of(), ReferenceIndex.dangling(root));
  }

  @Test
  public void randomTreesHaveNoDanglingReferencesAfterRenameOrMove() throws Exception {
    Random random = new Random(8);
    for (int trial = 0; trial < 20; trial++) {
      FitNesseContext context = FitNesseUtil.makeTestContext();
      WikiPage root = context.getRootPage();
      List<WikiPage> pages = grow(root, random);
      wire(pages, random);
      assertEquals(List.of(), ReferenceIndex.dangling(root));
      WikiPage subject = pages.get(random.nextInt(pages.size()));
      if (random.nextBoolean()) {
        rename(context, subject.getFullPath().toString(), "Renamed" + trial);
      } else {
        WikiPage dest = pages.get(random.nextInt(pages.size()));
        if (dest == subject || dest.getFullPath().startsWith(subject.getFullPath())) {
          continue;
        }
        MockRequest request = new MockRequest();
        request.setResource(subject.getFullPath().toString());
        request.addInput("newLocation", dest.getFullPath().toString());
        request.addInput("refactorReferences", "");
        new MovePageResponder().makeResponse(context, request);
      }
      assertEquals(ReferenceIndex.dangling(root).toString(), List.of(), ReferenceIndex.dangling(root));
    }
  }

  private static void rename(FitNesseContext context, String path, String newName) throws Exception {
    MockRequest request = new MockRequest();
    request.setResource(path);
    request.addInput("newName", newName);
    request.addInput("refactorReferences", "");
    new RenamePageResponder().makeResponse(context, request);
  }

  private static List<WikiPage> grow(WikiPage root, Random random) throws Exception {
    List<WikiPage> pages = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      WikiPage parent = pages.isEmpty() || random.nextBoolean() ? root : pages.get(random.nextInt(pages.size()));
      pages.add(WikiPageUtil.addPage(parent, PathParser.parse("Page" + (char) ('A' + i)), "notes"));
    }
    return pages;
  }

  private static void wire(List<WikiPage> pages, Random random) {
    String[] forms = {"path", "link", "include", "scenarios"};
    for (WikiPage page : pages) {
      StringBuilder content = new StringBuilder("notes\n");
      for (int i = 0; i < 3; i++) {
        WikiPage target = pages.get(random.nextInt(pages.size()));
        String path = "." + target.getFullPath();
        switch (forms[random.nextInt(forms.length)]) {
          case "path" -> content.append("see ").append(path).append(" here\n");
          case "link" -> content.append("[go](").append(path).append("#part)\n");
          case "include" -> content.append("```agentnesse\ninclude ").append(path).append("\n```\n");
          default -> content.append("```agentnesse\nscenarios ").append(path).append("\n```\n");
        }
      }
      var data = page.getData();
      data.setContent(content.toString());
      page.commit(data);
    }
  }
}
