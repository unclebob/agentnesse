package agnesse.wiki.search;

public interface CompositePageFinder extends PageFinder {

  void add(PageFinder finder);

}
