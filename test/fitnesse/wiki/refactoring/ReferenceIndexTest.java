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
  public void siblingNameOnTheMovedPageKeepsItsTarget() throws Exception {
    FitNesseContext context = tree();
    WikiPage parent = page(context, "ParentPage");
    child(parent, "PageStay", "<!-- id:stay -->\nnotes");
    child(parent, "PageMove", "see PageStay and [go](PageStay#part)\n```agentnesse\ninclude PageStay\n```\n```agentnesse\nscenarios PageStay\n```\n");
    move(context, "ParentPage.PageMove", "OtherPage");
    assertEquals(List.of("move#0=>stay", "move#1=>stay", "move#2=>stay", "move#3=>stay"),
      ReferenceIndex.targets(context.getRootPage()));
  }

  @Test
  public void ancestorBelowTheRootSurvivesLeavingTheBranch() throws Exception {
    FitNesseContext context = FitNesseUtil.makeTestContext();
    WikiPage outer = WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("OuterPage"), "<!-- id:outer -->\nnotes");
    WikiPage parent = WikiPageUtil.addPage(outer, PathParser.parse("ParentPage"), "<!-- id:parent -->\nnotes");
    child(parent, "PageMove", "see <ParentPage here");
    WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("OtherPage"), "<!-- id:other -->\nnotes");
    move(context, "OuterPage.ParentPage.PageMove", "OtherPage");
    assertEquals(List.of("move#0=>parent"), ReferenceIndex.targets(context.getRootPage()));
  }

  @Test
  public void bareSiblingWikiWordFollowsAMovedPage() throws Exception {
    FitNesseContext context = tree();
    WikiPage parent = page(context, "ParentPage");
    WikiPage stay = child(parent, "PageStay", "see PageMove here");
    child(parent, "PageMove", "<!-- id:move -->\nnotes");
    move(context, "ParentPage.PageMove", "OtherPage");
    assertEquals(List.of("stay#0=>move"), ReferenceIndex.targets(context.getRootPage()));
  }

  @Test
  public void childReferenceFollowsAMovedChild() throws Exception {
    FitNesseContext context = tree();
    WikiPage parent = page(context, "ParentPage");
    write(parent, "see >PageMove and [go](^PageMove#part)\n```agentnesse\ninclude >PageMove\n```\n```agentnesse\nscenarios ^PageMove\n```\n");
    child(parent, "PageMove", "<!-- id:move -->\nnotes");
    move(context, "ParentPage.PageMove", "OtherPage");
    assertEquals(List.of("parent#0=>move", "parent#1=>move", "parent#2=>move", "parent#3=>move"),
      ReferenceIndex.targets(context.getRootPage()));
  }

  @Test
  public void ancestorSearchSurvivesLeavingTheBranch() throws Exception {
    FitNesseContext context = tree();
    WikiPage parent = page(context, "ParentPage");
    WikiPage mover = child(parent, "PageMove", "see <ParentPage and [up](<ParentPage#top)\n```agentnesse\ninclude <ParentPage\n```\n```agentnesse\nscenarios <ParentPage\n```\n");
    move(context, "ParentPage.PageMove", "OtherPage");
    assertEquals(List.of("move#0=>parent", "move#1=>parent", "move#2=>parent", "move#3=>parent"),
      ReferenceIndex.targets(context.getRootPage()));
  }

  @Test
  public void caretSubpageStaysOnTheChildAfterRename() throws Exception {
    FitNesseContext context = tree();
    WikiPage parent = page(context, "ParentPage");
    WikiPage mover = child(parent, "PageMove", "see ^PageKid and [kid](>PageKid#end)\n```agentnesse\ninclude ^PageKid\n```\n```agentnesse\nscenarios >PageKid\n```\n");
    child(mover, "PageKid", "<!-- id:kid -->\nnotes");
    rename(context, "ParentPage.PageMove", "RenamedPage");
    assertEquals(List.of("move#0=>kid", "move#1=>kid", "move#2=>kid", "move#3=>kid"),
      ReferenceIndex.targets(context.getRootPage()));
  }

  @Test
  public void randomTreesHaveNoDanglingReferencesAfterRenameOrMove() throws Exception {
    Random random = new Random(8);
    for (int trial = 0; trial < 40; trial++) {
      FitNesseContext context = FitNesseUtil.makeTestContext();
      WikiPage root = context.getRootPage();
      List<WikiPage> pages = grow(root, random);
      wire(pages, random);
      List<String> before = ReferenceIndex.targets(root);
      assertTrue(before.toString(), before.stream().noneMatch(line -> line.contains("MISSING")));
      WikiPage subject = pages.get(random.nextInt(pages.size()));
      if (random.nextBoolean()) {
        rename(context, subject.getFullPath().toString(), "MovedTo" + (char) ('A' + trial));
      } else {
        WikiPage dest = pages.get(random.nextInt(pages.size()));
        if (dest == subject || dest.getFullPath().startsWith(subject.getFullPath())) {
          continue;
        }
        move(context, subject.getFullPath().toString(), dest.getFullPath().toString());
      }
      assertEquals(before, ReferenceIndex.targets(root));
    }
  }

  private static FitNesseContext tree() throws Exception {
    FitNesseContext context = FitNesseUtil.makeTestContext();
    WikiPage root = context.getRootPage();
    WikiPageUtil.addPage(root, PathParser.parse("OtherPage"), "<!-- id:other -->\nnotes");
    return context;
  }

  private static WikiPage page(FitNesseContext context, String name) throws Exception {
    return WikiPageUtil.addPage(context.getRootPage(), PathParser.parse(name), "<!-- id:" + id(name) + " -->\nnotes");
  }

  private static WikiPage child(WikiPage parent, String name, String content) throws Exception {
    String marked = content.contains("<!-- id:") ? content : "<!-- id:" + id(name) + " -->\n" + content;
    return WikiPageUtil.addPage(parent, PathParser.parse(name), marked);
  }

  private static void write(WikiPage page, String body) {
    var data = page.getData();
    data.setContent("<!-- id:" + id(page.getName()) + " -->\n" + body);
    page.commit(data);
  }

  private static String id(String name) {
    return name.replace("Page", "").replace("Parent", "parent").replace("Other", "other").toLowerCase();
  }

  private static void move(FitNesseContext context, String path, String newParent) throws Exception {
    MockRequest request = new MockRequest();
    request.setResource(path);
    request.addInput("newLocation", newParent);
    request.addInput("refactorReferences", "");
    new MovePageResponder().makeResponse(context, request);
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
      String name = "Page" + (char) ('A' + i);
      pages.add(WikiPageUtil.addPage(parent, PathParser.parse(name), "<!-- id:" + name + " -->\nnotes"));
    }
    return pages;
  }

  private static void wire(List<WikiPage> pages, Random random) {
    String[] forms = {"path", "link", "include", "scenarios"};
    for (WikiPage page : pages) {
      StringBuilder content = new StringBuilder(page.getData().getContent());
      if (!content.toString().endsWith("\n")) {
        content.append('\n');
      }
      mention(content, "." + pages.get(random.nextInt(pages.size())).getFullPath(), forms[random.nextInt(forms.length)]);
      List<WikiPage> siblings = siblings(page);
      if (!siblings.isEmpty()) {
        mention(content, siblings.get(random.nextInt(siblings.size())).getName(), forms[random.nextInt(forms.length)]);
      }
      List<WikiPage> children = page.getChildren();
      if (!children.isEmpty()) {
        String child = children.get(random.nextInt(children.size())).getName();
        mention(content, ">" + child, forms[random.nextInt(forms.length)]);
        mention(content, "^" + child, forms[random.nextInt(forms.length)]);
      }
      WikiPage ancestor = ancestor(page, random);
      if (ancestor != null) {
        mention(content, "<" + ancestor.getName(), forms[random.nextInt(forms.length)]);
      }
      var data = page.getData();
      data.setContent(content.toString());
      page.commit(data);
    }
  }

  private static void mention(StringBuilder content, String reference, String form) {
    switch (form) {
      case "path" -> content.append("see ").append(reference).append(" here\n");
      case "link" -> content.append("[go](").append(reference).append("#part)\n");
      case "include" -> content.append("```agentnesse\ninclude ").append(reference).append("\n```\n");
      default -> content.append("```agentnesse\nscenarios ").append(reference).append("\n```\n");
    }
  }

  private static List<WikiPage> siblings(WikiPage page) {
    List<WikiPage> siblings = new ArrayList<>();
    for (WikiPage sibling : page.getParent().getChildren()) {
      if (sibling != page) {
        siblings.add(sibling);
      }
    }
    return siblings;
  }

  private static WikiPage ancestor(WikiPage page, Random random) {
    List<WikiPage> ancestors = new ArrayList<>();
    for (WikiPage current = page.getParent(); current != null && !current.isRoot(); current = current.getParent()) {
      ancestors.add(current);
    }
    return ancestors.isEmpty() ? null : ancestors.get(random.nextInt(ancestors.size()));
  }
}
