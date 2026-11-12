package fitnesse.wiki.search;

import fitnesse.wiki.HitCollector;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageProperty;
import fitnesse.wiki.WikiPageUtil;
import fitnesse.wiki.fs.InMemoryPage;
import org.junit.Before;
import org.junit.Test;

public class WhereUsedPageFinderTest {
  private WikiPage root;
  private WikiPage pageOne;
  private WikiPage pageTwo;
  private WikiPage pageThree;
  private WikiPage pageTwoChild;
  private WikiPage pageTwoChild2;

  private HitCollector hits = new HitCollector();

  @Before
  public void setUp() {
    root = InMemoryPage.makeRoot("RooT");
    pageOne = WikiPageUtil.addPage(root, PathParser.parse("PageOne"), "this is page one, uncle of PageTwo.ChildPage");
    pageTwo = WikiPageUtil.addPage(root, PathParser.parse("PageTwo"), "I am Page Two my brother is PageOne. I have a >ChildPage. (and SomeMissingPage)");
    pageThree = WikiPageUtil.addPage(root, PathParser.parse("PageThree"), "This is !-PageThree-!, I Have \n!include PageTwo");
    pageTwoChild = WikiPageUtil.addPage(pageTwo, PathParser.parse("ChildPage"), "I am Child page, my uncle is .PageOne ");
    pageTwoChild2 = WikiPageUtil.addPage(pageTwo, PathParser.parse("ChildPage2"), "I am another Child page");
  }

  @Test
  public void testFindReferencingPagesOnSiblingAndChild() {
    WhereUsedPageFinder whereUsed = new WhereUsedPageFinder(pageOne, hits);
    whereUsed.search(root);
    hits.assertPagesFound(pageTwo.getName(), pageTwoChild.getName());
  }

  @Test
  public void testFindReferencingPagesOnSibling() {
    WhereUsedPageFinder whereUsed = new WhereUsedPageFinder(pageTwo, hits);
    whereUsed.search(root);
    hits.assertPagesFound(pageThree.getName());
  }

  @Test
  public void testFindReferencingPagesNotReferenced() {
    WhereUsedPageFinder whereUsed = new WhereUsedPageFinder(pageThree, hits);
    whereUsed.search(root);
    hits.assertPagesFound();
  }

  @Test
  public void testFindReferencingPagesFromChild() {
    WhereUsedPageFinder whereUsed = new WhereUsedPageFinder(pageTwoChild, hits);
    whereUsed.search(root);
    hits.assertPagesFound(pageOne.getName(), pageTwo.getName());
  }

  @Test
  public void testObserving() {
    WhereUsedPageFinder whereUsed = new WhereUsedPageFinder(pageOne, hits);
    whereUsed.search(root);
    hits.assertPagesFound(pageTwo.getName(), pageTwoChild.getName());
  }

  @Test
  public void testOnlyOneReferencePerPage() {
    WhereUsedPageFinder whereUsed = new WhereUsedPageFinder(pageThree, hits);
    WikiPage newPage = WikiPageUtil.addPage(root, PathParser.parse("NewPage"), "one reference to PageThree.  Two reference to PageThree");
    whereUsed.search(root);
    hits.assertPagesFound(newPage.getName());
  }

  @Test
  public void testWordsNotFoundInPreprocessedText() {
    WhereUsedPageFinder whereUsed = new WhereUsedPageFinder(pageThree, hits);
    WikiPageUtil.addPage(root, PathParser.parse("NewPage"), "{{{ PageThree }}}");
    whereUsed.search(root);
    hits.assertPagesFound();
  }

  @Test
  public void testFindReferencingPagesWithLinksWithAlternateText() {
    WhereUsedPageFinder whereUsed = new WhereUsedPageFinder(pageThree, hits);
    WikiPage newPage = WikiPageUtil.addPage(root, PathParser.parse("NewPage"), "I enjoy being a sibling of [[the third page][PageThree]]");
    whereUsed.search(root);
    hits.assertPagesFound(newPage.getName());
  }

  @Test
  public void pleaseMindPagesWithSuffixAreNotFound() {
    WhereUsedPageFinder whereUsed = new WhereUsedPageFinder(pageThree, hits);
    WikiPage newPage = WikiPageUtil.addPage(root, PathParser.parse("NewPage"), "I enjoy being a sibling of [[the third page][PageThree?edit]]");
    whereUsed.search(root);
    hits.assertPagesFound(newPage.getName());
  }

  @Test
  public void testFinderShouldDealWithOtherLinks() {
    WhereUsedPageFinder whereUsed = new WhereUsedPageFinder(pageThree, hits);
    WikiPage newPage = WikiPageUtil.addPage(root, PathParser.parse("NewPage"), "I enjoy being a sibling of [[the third page][http://fitnesse.org]]");
    whereUsed.search(root);
    hits.assertPagesFound();
  }

}
