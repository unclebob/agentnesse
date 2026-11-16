package agnesse.testrunner;

import agnesse.testsystems.TestPage;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageDummy;
import agnesse.wiki.WikiPageUtil;

public class WikiTestPageUtil {

  public static String makePageHtml(WikiTestPage page){
    return WikiPageUtil.getHeaderPageHtml(page.getSourcePage()) + page.getHtml();
  }

  public static WikiPage getSourcePage(TestPage testPage) {
    if (testPage instanceof WikiTestPage) {
      return ((WikiTestPage) testPage).getSourcePage();
    }
    return new WikiPageDummy();
  }
}
