package fitnesse.responders.run;

import fitnesse.wiki.WikiPage;

import java.util.List;

import static fitnesse.wiki.WikiPageUtil.isTestPage;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class TestResponder extends SuiteResponder {

  @Override
  protected List<WikiPage> getPagesToRun() {
    if (isTestPage(page)) {
      return singletonList(page);
    } else {
      return emptyList();
    }
  }

}
