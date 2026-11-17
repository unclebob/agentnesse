package agnesse.wiki.search;

import static agnesse.wiki.PageData.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import agnesse.components.TraversalListener;
import agnesse.wiki.PageData;
import agnesse.wiki.PageType;
import agnesse.wiki.TestAdjunct;
import agnesse.wiki.WikiPage;

public class AttributeWikiPageFinder extends WikiPageFinder {

  private static final List<String> setUpPageNames = TestAdjunct.SET_UPS;
  private static final List<String> tearDownPageNames = TestAdjunct.TEAR_DOWNS;

  private List<PageType> requestedPageTypes;
  private Map<String, Boolean> attributes;
  private List<String> suites;

  private static List<String> splitSuitesIntoArray(String suitesInput) {
    if (suitesInput == null)
      return null;

    if (isEmpty(suitesInput))
      return Collections.emptyList();

    return Arrays.asList(suitesInput.split("\\s*,\\s*"));
  }

  private static boolean isEmpty(String checkedString) {
    for (char character : checkedString.toCharArray()) {
      if (!Character.isWhitespace(character))
        return false;
    }
    return true;
  }

  public AttributeWikiPageFinder(TraversalListener<? super WikiPage> observer,
      List<PageType> requestedPageTypes, Map<String, Boolean> attributes,
      List<String> suites) {
    super(observer);

    this.requestedPageTypes = requestedPageTypes;
    this.attributes = attributes;
    this.suites = suites;
  }

  public AttributeWikiPageFinder(TraversalListener<? super WikiPage> observer,
      List<PageType> pageTypes, Map<String, Boolean> attributes, String suites) {
    this(observer, pageTypes, attributes, splitSuitesIntoArray(suites));
  }

  @Override
  protected boolean pageMatches(WikiPage page) {

    if (!meetsSetUpTearDownConditions(page)) {
      return false;
    }

    PageData pageData = page.getData();

    if (!pageIsOfRequestedPageType(page, requestedPageTypes)) {
      return false;
    }

    for (Map.Entry<String, Boolean> input : attributes.entrySet()) {
      if (TestAdjunct.SET_UP.equals(input.getKey()) || TestAdjunct.TEAR_DOWN.equals(input.getKey()))
        continue;

      if (!attributeMatchesInput(pageData.hasAttribute(input.getKey()), input
          .getValue()))
        return false;
    }

    return suitesMatchInput(pageData, suites);
  }

  private boolean meetsSetUpTearDownConditions(WikiPage page) {

    if (attributes.containsKey(TestAdjunct.SET_UP) && attributes.containsKey(TestAdjunct.TEAR_DOWN)) {
      Boolean setupWanted = attributes.get(TestAdjunct.SET_UP);
      Boolean teardownWanted = attributes.get(TestAdjunct.TEAR_DOWN);

      if (setupWanted && teardownWanted) {
        return isSetUpPage(page) || isTearDownPage(page);
      }

      return isSetUpPage(page) == setupWanted && isTearDownPage(page) == teardownWanted;

    }

    if (attributes.containsKey(TestAdjunct.SET_UP)) {
      return isSetUpPage(page) == attributes.get(TestAdjunct.SET_UP);
    }

    if (attributes.containsKey(TestAdjunct.TEAR_DOWN)) {
      return isTearDownPage(page) == attributes.get(TestAdjunct.TEAR_DOWN);
    }

    return true;
  }

  private boolean isTearDownPage(WikiPage page) {
    return tearDownPageNames.contains(page.getName());
  }

  private boolean isSetUpPage(WikiPage page) {
    return setUpPageNames.contains(page.getName());
  }

  private boolean pageIsOfRequestedPageType(WikiPage page,
      List<PageType> requestedPageTypesEnum) {
	
	if (requestedPageTypesEnum == null || requestedPageTypesEnum.isEmpty()) {
		return true;
	}
	
    PageType pageType = PageType.fromWikiPage(page);

    return (requestedPageTypesEnum.contains(pageType));
  }

  protected boolean attributeMatchesInput(boolean attributeSet,
      boolean inputValueOn) {
    return !inputValueOn || attributeSet == inputValueOn;
  }

  private boolean suitesMatchInput(PageData pageData, List<String> suites) {
    if (suites == null)
      return true;

    List<String> suitesProperty = splitSuitesIntoArray(pageData.getAttribute(PropertySUITES));

    if (suites.isEmpty() != isEmptyOrNull(suitesProperty))
      return false;

    for (String suite : suites) {
      if (!suitesProperty.contains(suite))
        return false;
    }
    return true;
  }

  private boolean isEmptyOrNull(List<String> stringList) {
    return stringList == null || stringList.isEmpty();
  }

}
