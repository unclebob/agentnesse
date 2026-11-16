package agnesse.testrunner.run;

import agnesse.wiki.WikiPage;

import java.util.List;

public class PagesByTestSystemTestRunFactory implements TestRunFactory {
  @Override
  public boolean canRun(List<WikiPage> pages) {
    return true;
  }

  @Override
  public TestRun createRun(List<WikiPage> pages) {
    return new PerTestSystemTestRun(pages);
  }
}
