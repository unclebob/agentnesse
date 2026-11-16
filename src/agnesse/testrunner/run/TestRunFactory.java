package agnesse.testrunner.run;

import agnesse.wiki.WikiPage;

import java.util.List;

public interface TestRunFactory {
  boolean canRun(List<WikiPage> pages);

  TestRun createRun(List<WikiPage> pages);
}
