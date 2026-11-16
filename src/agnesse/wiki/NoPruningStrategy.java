package agnesse.wiki;

public class NoPruningStrategy implements PagePruningStrategy {
    @Override
    public boolean skipPageAndChildren(WikiPage page) {
        return false;
    }
}
