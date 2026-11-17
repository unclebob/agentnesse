// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package agnesse.testrunner;

import agnesse.wiki.TestAdjunct;
import agnesse.wiki.PageCrawler;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPagePath;
import agnesse.wiki.WikiPageProperty;
import agnesse.wiki.WikiPageUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SuiteContentsFinder {
  private static final Logger LOG = Logger.getLogger(SuiteContentsFinder.class.getName());

  private final WikiPage pageToRun;
  private final WikiPage wikiRootPage;
  private final SuiteFilter suiteFilter;

  public SuiteContentsFinder(final WikiPage pageToRun, final SuiteFilter suiteFilter, WikiPage root) {
    this.pageToRun = pageToRun;
    wikiRootPage = root;
    this.suiteFilter = (suiteFilter != null) ? suiteFilter : SuiteFilter.MATCH_ALL;
  }

  public List<WikiPage> getAllPagesToRunForThisSuite() {
    return getAllTestPagesUnder();
  }

  private List<WikiPage> getAllTestPagesUnder() {
    List<WikiPage> testPages = addTestPagesToSuite(pageToRun, suiteFilter);

    testPages.sort((p1, p2) -> {
      try {
        WikiPagePath path1 = p1.getFullPath();
        WikiPagePath path2 = p2.getFullPath();

        return path1.compareTo(path2);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Unable to compare " + p1 + " and " + p2, e);
        return 0;
      }
    });

    return testPages;
  }

  private List<WikiPage> addTestPagesToSuite(WikiPage page, SuiteFilter suiteFilter) {
    List<WikiPage> testPages = new LinkedList<>();
    boolean includePage = isTopPage(page) || !isPruned(page);
    if (suiteFilter.isMatchingTest(page) && includePage && !structural(page)) {
      testPages.add(page);
    }
    addXrefPages(testPages, page);

    SuiteFilter suiteFilterForChildren = includePage ? suiteFilter.getFilterForTestsInSuite(page) : SuiteFilter.NO_MATCHING;

    for (WikiPage child : page.getChildren()) {
      testPages.addAll(addTestPagesToSuite(child, suiteFilterForChildren));
    }
    return testPages;
  }

  private boolean isPruned(WikiPage page) {
    return page.getData().hasAttribute(WikiPageProperty.PRUNE);
  }

  private boolean isTopPage(WikiPage page) {
    return page == pageToRun;
  }

  /**
   * SetUp, TearDown, and the scenario library are pulled into a test. They are
   * not tests of their own, even when their tables are Slim tables.
   */
  private static boolean structural(WikiPage page) {
    String name = page.getName();
    return TestAdjunct.named(name);
  }

  private void addXrefPages(List<WikiPage> pages, WikiPage thePage) {
    List<String> pageReferences = WikiPageUtil.getXrefPages(thePage);
    if (pageReferences.isEmpty()) {
      return;
    }
    PageCrawler pageCrawler = thePage.getPageCrawler();
    for (String pageReference : pageReferences) {
      WikiPagePath path = PathParser.parse(pageReference);
      WikiPage referencedPage = pageCrawler.getSiblingPage(path);
      if (referencedPage != null && suiteFilter.isMatchingTest(referencedPage) && !structural(referencedPage))
        pages.add(referencedPage);
    }
  }
}
