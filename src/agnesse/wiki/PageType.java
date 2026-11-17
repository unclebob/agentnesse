package agnesse.wiki;

public enum PageType {

  SUITE("Suite"),
  TEST("Test"),
  TEST_ADJUNCT(TestAdjunct.TYPE),
  STATIC("Static");

  public static PageType fromString(String typeDescriptor) {
    for (PageType type: PageType.values()) {
      if (type.description.equalsIgnoreCase(typeDescriptor)) {
        return type;
      }
    }

    throw new IllegalArgumentException("unknown page type descriptor: " + typeDescriptor);

  }

  public static PageType fromWikiPage(WikiPage page) {
    PageData data = page.getData();
    if (data.hasAttribute(SUITE.toString())) {
      return SUITE;
    }

    if (data.hasAttribute(TEST.toString())) {
      return TEST;
    }

    if (data.hasAttribute(TEST_ADJUNCT.toString())) {
      return TEST_ADJUNCT;
    }

    return STATIC;
  }

  public String frontMatter() {
    return description.toLowerCase();
  }

  public static PageType parseFrontMatter(String type) {
    if (type == null) {
      return null;
    }
    for (PageType pageType : values()) {
      if (pageType.frontMatter().equals(type)) {
        return pageType;
      }
    }
    return null;
  }

  public static void applyTo(WikiPageProperty properties, String type) {
    for (PageType pageType : values()) {
      properties.remove(pageType.toString());
    }
    PageType parsed = parseFrontMatter(type);
    if (parsed != null && parsed != STATIC) {
      properties.set(parsed.toString());
    }
  }

  public static String storedType(PageData data) {
    if (data.hasAttribute(SUITE.toString())) {
      return SUITE.frontMatter();
    }
    if (data.hasAttribute(TEST.toString())) {
      return TEST.frontMatter();
    }
    if (data.hasAttribute(TEST_ADJUNCT.toString())) {
      return TEST_ADJUNCT.frontMatter();
    }
    return STATIC.frontMatter();
  }

  public static PageType getPageTypeForPageName(String pageName) {
    for (PageType type: values()) {
      if (type.validForPageName(pageName))
        return type;
    }
    return STATIC;
  }

  private String description;

  PageType(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return description;
  }

  public boolean validForPageName(String pageName) {
    if (this == SUITE) {
      return suiteName(pageName);
    }
    if (this == TEST) {
      return testName(pageName);
    }
    if (this == TEST_ADJUNCT) {
      return TestAdjunct.named(pageName);
    }
    return true;
  }

  private static boolean suiteName(String pageName) {
    return (pageName.startsWith(SUITE.toString())
        && !pageName.equals(TestAdjunct.SUITE_SET_UP)
        && !pageName.equals(TestAdjunct.SUITE_TEAR_DOWN))
        || pageName.endsWith(SUITE.toString())
        || pageName.endsWith("Examples");
  }

  private static boolean testName(String pageName) {
    return pageName.startsWith(TEST.toString())
        || pageName.endsWith(TEST.toString())
        || (pageName.startsWith("Example") && !pageName.startsWith("Examples"))
        || pageName.endsWith("Example");
  }

}
