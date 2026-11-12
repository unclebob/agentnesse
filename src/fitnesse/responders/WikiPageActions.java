package fitnesse.responders;

import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPagePath;

// Work in progress, WikiPage, versions, directory should each have specific actions instances.
public class WikiPageActions {

  private WikiPage page;

  public WikiPageActions(WikiPage page) {
    super();
    this.page = page;
  }

  public boolean isTestPage() {
    return hasAction("Test");
  }

  public boolean isSuitePage() {
    return hasAction("Suite");
  }

  public boolean isDefaultPage() {
    return !(isTestPage() || isSuitePage());
  }

  public boolean isWithEdit() {
    return hasAction("Edit");
  }

  public boolean isWithRefactor() {
    return hasAction("Refactor");
  }

  public boolean isWithSearch() {
    return hasAction("Search");
  }

  public boolean isWithFiles() {
    return hasAction("Files");
  }

  public boolean isWithVariablesScope() { return page != null; }

  public boolean isWithUserGuide() {
    return page != null;
  }

  private boolean hasAction(String action) {
    PageData data = getData();
    return data != null && data.hasAttribute(action);
  }

  private PageData getData() {
    if (page != null) {
      return page.getData();
    }
    return null;
  }

  public String getLocalPageName() {
    if (page != null) {
      WikiPagePath localPagePath = page.getFullPath();
      return PathParser.render(localPagePath);
    }
    return null;
  }
}
