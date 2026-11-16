package agnesse.junit;

import java.lang.annotation.Annotation;

import agnesse.testrunner.WikiTestPage;
import agnesse.testsystems.TestPage;
import agnesse.wiki.WikiPage;

/**
 * Annotation used to pass AgNesse page information to test listeners.
 */
public class AgNessePageAnnotation implements Annotation {
  private final TestPage testPage;
  private final WikiPage wikiPage;

  /**
   * Creates new.
   *
   * @param wikiPage page describing test.
   */
  public AgNessePageAnnotation(WikiPage wikiPage) {
    this.testPage = null;
    this.wikiPage = wikiPage;
  }

  /**
   * Creates new.
   *
   * @param testPage page describing test.
   */
  public AgNessePageAnnotation(TestPage testPage) {
    this.testPage = testPage;
    if (testPage instanceof WikiTestPage) {
      wikiPage = ((WikiTestPage) testPage).getSourcePage();
    } else {
      this.wikiPage = null;
    }
  }

  /**
   * @return wiki page for current test.
   */
  public WikiPage getWikiPage() {
    return wikiPage;
  }

  /**
   * @return current test page.
   */
  public TestPage getTestPage() {
    return testPage;
  }

  @Override
  public Class<? extends AgNessePageAnnotation> annotationType() {
    return AgNessePageAnnotation.class;
  }
}
