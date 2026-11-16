package agnesse.testrunner.run;

import agnesse.wiki.WikiPage;

import java.util.List;

public interface PageListSetUpTearDownProcessor {
  List<WikiPage> addSuiteSetUpsAndTearDowns(List<WikiPage> pageList);
}
