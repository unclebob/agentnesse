package agnesse.wiki.search;

import agnesse.components.TraversalListener;
import agnesse.wiki.NoPruningStrategy;
import agnesse.wiki.WikiPage;

public abstract class WikiPageFinder implements PageFinder {

  private WikiPageTraverser traverser;

  protected WikiPageFinder(TraversalListener<? super WikiPage> observer) {
    this.traverser = new WikiPageTraverser(this, observer);
  }

  protected abstract boolean pageMatches(WikiPage page);

  @Override
  public void search(WikiPage page) {
    page.getPageCrawler().traverse(traverser, new NoPruningStrategy());
  }
}

