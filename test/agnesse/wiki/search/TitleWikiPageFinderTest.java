package agnesse.wiki.search;

import agnesse.wiki.HitCollector;
import agnesse.wiki.PageData;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.InMemoryPage;
import org.junit.Before;
import org.junit.Test;

public class TitleWikiPageFinderTest {
  WikiPage root;

  private HitCollector hits = new HitCollector();

  @Before
  public void setUp() throws Exception {
    root = InMemoryPage.makeRoot("RooT");
    WikiPageUtil.addPage(root, PathParser.parse("PageOne"), "has PageOne content");
    WikiPageUtil.addPage(root, PathParser.parse("PageOne.PageOneChild"), "PageChild is a child of PageOne");
    WikiPage pageTwo = WikiPageUtil.addPage(root, PathParser.parse("PageTwo"), "PageTwo has a bit of content too\n^PageOneChild");
    PageData data = pageTwo.getData();
    pageTwo.commit(data);
  }

  @Test
  public void titleSearch() throws Exception {
    TitleWikiPageFinder searcher = new TitleWikiPageFinder("one", hits);
    searcher.search(root);
    hits.assertPagesFound("PageOne", "PageOneChild");
  }
}
