package agnesse.wiki;

public class PrunedPagePruningStrategy implements PagePruningStrategy {
    @Override
    public boolean skipPageAndChildren(WikiPage page) {
        return page.getData().hasAttribute(PageData.PropertyPRUNE);
    }
}
