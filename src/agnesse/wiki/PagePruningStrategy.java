package agnesse.wiki;

public interface PagePruningStrategy {
    boolean skipPageAndChildren(WikiPage page);
}
