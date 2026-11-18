package agnesse.wiki;

import agnesse.wiki.fs.InMemoryPage;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WikiVariableSourceTest {
  @Test
  public void applicationVariablesComeFromTheInnerSourceOrADefault() {
    ApplicationVariableSource empty = new ApplicationVariableSource(name -> Optional.empty());
    assertEquals("-1", empty.findVariable("AGNESSE_PORT").get());
    assertEquals("", empty.findVariable("AGNESSE_ROOTPATH").get());
    assertEquals("", empty.findVariable("AGNESSE_VERSION").get());
    assertFalse(empty.findVariable("OTHER").isPresent());

    ApplicationVariableSource filled = new ApplicationVariableSource(name -> Optional.of(name + "-v"));
    assertEquals("AGNESSE_PORT-v", filled.findVariable("AGNESSE_PORT").get());
    assertEquals("AGNESSE_ROOTPATH-v", filled.findVariable("AGNESSE_ROOTPATH").get());
    assertEquals("AGNESSE_VERSION-v", filled.findVariable("AGNESSE_VERSION").get());

    assertFalse(new ApplicationVariableSource(null).findVariable("AGNESSE_PORT").isPresent());
  }

  @Test
  public void compositeSkipsNullSourcesAndStopsAtTheFirstValue() {
    VariableSource miss = name -> Optional.empty();
    VariableSource hit = name -> Optional.of("hit");
    assertEquals("hit", new CompositeVariableSource(null, miss, hit).findVariable("x").get());
    assertFalse(new CompositeVariableSource(miss, null).findVariable("x").isPresent());
    assertFalse(new CompositeVariableSource().findVariable("x").isPresent());
  }

  @Test
  public void pageVariablesNameTheRunningPage() throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage child = WikiPageUtil.addPage(root, PathParser.parse("Parent.Child"), "");
    PageVariableSource source = new PageVariableSource(child);
    assertEquals("Child", source.findVariable("RUNNING_PAGE_NAME").get());
    assertEquals("Parent", source.findVariable("RUNNING_PAGE_PATH").get());
    assertFalse(source.findVariable("OTHER").isPresent());

    PageVariableSource rootSource = new PageVariableSource(root);
    assertEquals("RooT", rootSource.findVariable("RUNNING_PAGE_NAME").get());
    assertEquals("", rootSource.findVariable("RUNNING_PAGE_PATH").get());
  }

  @Test
  public void parentVariablesComeFromTheParentPage() throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage parent = WikiPageUtil.addPage(root, PathParser.parse("Parent"),
      "```agnesse\ndefine y\nY\n```\n");
    WikiPage child = WikiPageUtil.addPage(parent, PathParser.parse("Child"), "");

    assertEquals("Y", new ParentPageVariableSource(child).findVariable("y").get());
    assertFalse(new ParentPageVariableSource(child).findVariable("missing").isPresent());
    assertFalse(new ParentPageVariableSource(root).findVariable("y").isPresent());
    assertTrue(root.isRoot());
  }
}
