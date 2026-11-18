package agnesse.wiki;

import java.io.File;
import java.io.IOException;
import java.util.List;

import agnesse.wiki.fs.InMemoryPage;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class WikiPageUtilTest {

  @Test
  public void shouldResolveFileUris() {
    assertThat(WikiPageUtil.resolveFileUri("file:/tmp/someFile", new File(".")), equalTo(new File("/tmp/someFile")));
    assertThat(WikiPageUtil.resolveFileUri("file:///tmp/someFile", new File(".")), equalTo(new File("/tmp/someFile")));
    //assertThat(WikiPageUtil.resolveFileUri("file:////tmp/someFile", new File(".")), equalTo(new File ("/tmp/someFile")));
  }

  @Test
  public void shouldSupportRelativeLinksWithDoubleSlash() throws IOException {
    assertThat(WikiPageUtil.resolveFileUri("file:tmp/someFile", new File(".")), equalTo(new File(new File(".").getCanonicalFile(), "/tmp/someFile")));
    assertThat(WikiPageUtil.resolveFileUri("file://tmp/someFile", new File(".")), equalTo(new File(new File(".").getCanonicalFile(), "/tmp/someFile")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldOnlyHandleFileUris() throws IOException {
    assertThat(WikiPageUtil.resolveFileUri("jiberish:/tmp/someFile", new File(".")), equalTo(new File(new File(".").getCanonicalFile(), "/tmp/someFile")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void aRelativeFileUriWithoutARootIsRejected() {
    WikiPageUtil.resolveFileUri("file:tmp/someFile", null);
  }

  @Test
  public void slimTablesRenderForTestsAndTestAdjuncts() throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("Plain"), "");
    assertFalse(WikiPageUtil.rendersSlimTables(page));
    assertFalse(WikiPageUtil.isTestPage(page));

    PageData data = page.getData();
    data.setAttribute("Test");
    page.commit(data);
    assertTrue(WikiPageUtil.isTestPage(page));
    assertTrue(WikiPageUtil.rendersSlimTables(page));

    data = page.getData();
    data.removeAttribute("Test");
    data.setAttribute(TestAdjunct.TYPE);
    page.commit(data);
    assertFalse(WikiPageUtil.isTestPage(page));
    assertTrue(WikiPageUtil.rendersSlimTables(page));
    assertTrue(TestAdjunct.is(page));
  }

  @Test
  public void testGetCrossReferences() throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("PageName"), "!see XrefPage\r\n");
    List<?> xrefs = WikiPageUtil.getXrefPages(page);
    assertEquals("XrefPage", xrefs.get(0));
  }

  @Test
  public void testGetCrossReferencesWithAlias() throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("PageName"), "!see [[xref page][XrefPage]]\r\n");
    List<?> xrefs = WikiPageUtil.getXrefPages(page);
    assertEquals("XrefPage", xrefs.get(0));
  }

  @Test
  public void testGetCrossReferencesFromAMarkdownLink() throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("PageName"), "See [the page](XrefPage).\n");
    List<?> xrefs = WikiPageUtil.getXrefPages(page);
    assertEquals("XrefPage", xrefs.get(0));
  }

  @Test
  public void testGetCrossReferencesWithMalformedAlias() throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("PageName"), "!see [[starts like alias but is not\r\n");
    List<?> xrefs = WikiPageUtil.getXrefPages(page);
    assertEquals(0, xrefs.size());
  }

}
