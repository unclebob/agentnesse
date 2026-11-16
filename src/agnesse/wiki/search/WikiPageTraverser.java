package agnesse.wiki.search;

import agnesse.components.TraversalListener;
import agnesse.wiki.WikiPage;

class WikiPageTraverser implements TraversalListener<WikiPage> {

  private final WikiPageFinder finder;
  private final TraversalListener<? super WikiPage> observer;

  WikiPageTraverser(WikiPageFinder finder, TraversalListener<? super WikiPage> observer){
    this.finder = finder;
    this.observer = observer;
  }

  @Override
  public void process(WikiPage page) {
    if (finder.pageMatches(page)) {
      observer.process(page);
    }
  }
}
