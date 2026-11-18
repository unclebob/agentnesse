package agnesse.wiki.search;

import agnesse.markdown.FrontMatter;
import agnesse.wiki.HitCollector;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.InMemoryPage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class FrontMatterFinderTest {
  private WikiPage root;

  @Before
  public void setUp() throws Exception {
    root = InMemoryPage.makeRoot("RooT");
    add("TestReady", "test", List.of("ready", "slow"), "How to Start", false);
    add("TestOther", "test", List.of("ready"), "nope", false);
    add("TestPruned", "test", List.of("ready", "slow"), "How to Start", true);
    add("TestMissingTag", "test", List.of("ready"), "How to Start", false);
    add("SuitePruned", "suite", List.of("ready"), null, true);
    add("StaticHelp", "static", List.of(), "other help", false);
  }

  @Test
  public void noCriteriaFindsNothing() {
    HitCollector hits = search(null, "  ", "", "\t");
    hits.assertPagesFound();
  }

  @Test
  public void everyCriterionHasToMatch() {
    HitCollector hits = search("test", "ready, slow", "start", "false");
    hits.assertPagesFound("TestReady");
  }

  @Test
  public void blankHelpAndPruneDoNotFilter() {
    HitCollector hits = search(null, "ready, ,slow", "  ", "");
    hits.assertPagesFound("TestReady", "TestPruned");
  }

  @Test
  public void aTypeAloneMatchesEveryPageOfThatType() {
    HitCollector hits = search("test", null, null, null);
    hits.assertPagesFound("TestReady", "TestOther", "TestPruned", "TestMissingTag");
  }

  @Test
  public void helpMatchIgnoresCaseAndSkipsAMissingHelpText() {
    HitCollector hits = search(null, null, "OTHER", null);
    hits.assertPagesFound("StaticHelp");
  }

  @Test
  public void pruneTrueMatchesOnlyPrunedPages() {
    HitCollector hits = search(null, null, null, "true");
    hits.assertPagesFound("TestPruned", "SuitePruned");
  }

  @Test
  public void aNonBooleanPruneValueMeansNotPruned() {
    HitCollector hits = search("suite", null, null, "yes");
    hits.assertPagesFound();
  }

  private void add(String name, String type, List<String> tags, String help, boolean prune) throws Exception {
    WikiPageUtil.addPage(root, PathParser.parse(name), FrontMatter.write(type, tags, help, prune, "body"));
  }

  private HitCollector search(String type, String tags, String help, String prune) {
    HitCollector hits = new HitCollector();
    new FrontMatterFinder(hits, type, tags, help, prune).search(root);
    return hits;
  }
}
