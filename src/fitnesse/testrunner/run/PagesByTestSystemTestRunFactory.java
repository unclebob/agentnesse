package fitnesse.testrunner.run;

import fitnesse.wiki.WikiPage;

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
