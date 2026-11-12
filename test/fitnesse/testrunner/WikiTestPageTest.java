// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.testrunner;

import fitnesse.testsystems.TestPage;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import fitnesse.wiki.fs.InMemoryPage;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static util.RegexTestCase.assertNotSubString;
import static util.RegexTestCase.assertSubString;

public class WikiTestPageTest {
  private WikiPage root;
  private WikiPage wikiPage;
  private Properties properties;

  @Before
  public void setUp() throws Exception {
    properties = new Properties();
    root = InMemoryPage.makeRoot("RooT", properties);
    wikiPage = addPage("TestPage", define("TEST_SYSTEM", "slim") + "the content");
    addPage("SetUp", "setup");
    addPage("TearDown", "teardown");
    addPage("SuiteSetUp", "suite-setup-body");
    addPage("SuiteTearDown", "suite-teardown-body");

    WikiPage subPage = WikiPageUtil.addPage(wikiPage, PathParser.parse("SubPage"), "sub page");
    WikiPageUtil.addPage(wikiPage, PathParser.parse("ScenarioLibrary"), "scenario library 2");

    WikiPageUtil.addPage(subPage, PathParser.parse("TestSubPage"), "sub test page");
    WikiPageUtil.addPage(subPage, PathParser.parse("ScenarioLibrary"), "scenario library 3");
  }

  private static String define(String name, String value) {
    return "```agentnesse\ndefine " + name + "\n" + value + "\n```\n";
  }

  private WikiPage addPage(String pageName, String content) throws Exception {
    return WikiPageUtil.addPage(root, PathParser.parse(pageName), content);
  }

  @Test
  public void testIncludeSetupTearDownOutsideOfSuite()
    throws Exception {
    TestPage testPage = new WikiTestPage(wikiPage);
    String html = testPage.getHtml();
    assertSubString(">SetUp<", html);
    assertSubString("setup", html);
    assertSubString(">TearDown<", html);
    assertSubString("teardown", html);
    assertSubString("the content", html);
    assertSubString("agentnesse-include", html);
    assertNotSubString(">SuiteSetUp<", html);
    assertNotSubString("suite-setup-body", html);
    assertNotSubString(".SuiteTearDown", html);
    assertNotSubString("suitTearDown", html);
  }

  @Test
  public void testIncludeSetupTearDownInsideOfSuite() {
    TestPage test = new TestPageWithSuiteSetUpAndTearDown(wikiPage);
    String html = test.getHtml();
    assertSubString(">SetUp<", html);
    assertSubString("setup", html);
    assertSubString(">TearDown<", html);
    assertSubString("teardown", html);
    assertSubString("the content", html);
    assertSubString("agentnesse-include", html);
    assertSubString(">SuiteSetUp<", html);
    assertSubString("suite-setup-body", html);
    assertSubString(">SuiteTearDown<", html);
    assertSubString("suite-teardown-body", html);
    assertEquals("SetUp occurs more than once", html.indexOf(" SetUp"), html.lastIndexOf(" SetUp"));
    assertEquals("TearDown occurs more than once", html.indexOf(" TearDown"), html.lastIndexOf(" TearDown"));
  }

  @Test
  public void testIncludeOneSetupTearDownInsideOfSuiteWhenIncludingAll() {
    properties.setProperty("ALL_UNCLE_SUITE_SETUPS", "true");
    testIncludeSetupTearDownInsideOfSuite();
  }

  @Test
  public void includeScenarioLibraryBrother() throws Exception {
    WikiPage slimTestPage = addPage("SlimTest", define("TEST_SYSTEM", "slim"));
    TestPage testPage = new WikiTestPage(slimTestPage);
    addPage("ScenarioLibrary", "scenario library");
    String html = testPage.getHtml();
    assertSubString("scenario library", html);
  }

  @Test
  public void includeScenarioLibraryUnclesInOrder() throws Exception {
    addPage("TestPage.TestPageChild", "child");
    addPage("TestPage.ScenarioLibrary", "child library");
    WikiPage nephew = addPage("TestPage.TestPageChild.TestPageGrandChild", define("TEST_SYSTEM", "slim"));
    addPage("TestPage.TestPageChild.ScenarioLibrary", "grand child library");

    TestPage testPage = new WikiTestPage(nephew);
    String html = testPage.getHtml();
    assertSubString("child library", html);
    assertSubString("grand child library", html);
    assertTrue(html.indexOf("grand child library") > html.indexOf("child library"));
  }

  @Test
  public void includesSetupTearDownUnclesInOrder() throws Exception {
    addPage("TestPage.SuiteSetUp", "child set up");
    addPage("TestPage.SuiteTearDown", "child tear down");
    WikiPage nephew = addPage("TestPage.TestPageChild.TestPageGrandChild", define("TEST_SYSTEM", "slim"));
    addPage("TestPage.TestPageChild.SuiteSetUp", "grand child set up");
    addPage("TestPage.TestPageChild.SuiteTearDown", "grand child tear down");

    properties.setProperty("ALL_UNCLE_SUITE_SETUPS", "true");

    TestPage test = new TestPageWithSuiteSetUpAndTearDown(wikiPage);
    String html = test.getHtml();
    assertSubString("suite-setup-body", html);
    assertSubString("child set up", html);
    assertSubString("suite-teardown-body", html);
    assertSubString("child tear down", html);
    assertTrue("Uncle SuiteSetUp before brother", html.indexOf("suite-setup-body") < html.indexOf("child set up"));
    assertTrue("Uncle SuiteTearDown after brother", html.indexOf("suite-teardown-body") > html.indexOf("child tear down"));
    assertNotSubString("grand child set up", html);
    assertNotSubString("grand child tear down", html);
  }

  @Test
  public void shouldNotContainScenarioLibrarySectionIfThereAreNone() throws Exception {
    WikiPage slimTestPage = addPage("SlimTest", define("TEST_SYSTEM", "slim"));
    TestPage testPage = new WikiTestPage(slimTestPage);
    String html = testPage.getHtml();
    assertNotSubString("Scenario Libraries", html);
  }

  @Test
  public void shouldNotContainSuiteSetUpTearDownSectionIfThereAreNone() throws Exception {
    properties.setProperty("ALL_UNCLE_SUITE_SETUPS", "true");
    WikiPage slimTestPage = addPage("SlimTest", define("TEST_SYSTEM", "slim"));
    TestPage testPage = new WikiTestPage(slimTestPage);
    String html = testPage.getHtml();
    assertNotSubString("Suite Set Ups", html);
    assertNotSubString("Suite Tear Downs", html);
    assertNotSubString(".SuiteSetUp", html);
    assertNotSubString(".SuiteTearDown", html);
  }

  @Test
  public void shouldReturnDecoratedContentForWikitextPages() throws Exception {
    WikiPage slimTestPage = addPage("SlimTest", define("TEST_SYSTEM", "slim"));
    TestPage testPage = new WikiTestPage(slimTestPage);
    String content = testPage.getContent();
    assertSubString(define("TEST_SYSTEM", "slim"), content);
  }

  @Test
  public void shouldNotIncludeScenarioLibrariesIfNotSlimTest() throws Exception {
    addPage("ScenarioLibrary", "scenario library");
    WikiPage someTest = addPage("SomeTest", "some test");
    TestPage testPage = new WikiTestPage(someTest);
    String html = testPage.getHtml();
    assertNotSubString("scenario library", html);
  }

  @Test
  public void shouldIncludeScenarioLibrariesIfIncludeVariableSetAndNotSlimTest() throws Exception {
    WikiPage slimTestPage = addPage("SlimTest", define("INCLUDE_SCENARIO_LIBRARIES", "true"));
    TestPage testPage = new WikiTestPage(slimTestPage);
    addPage("ScenarioLibrary", "scenario library");
    String html = testPage.getHtml();
    assertSubString("scenario library", html);
  }

  @Test
  public void shouldNotIncludeScenarioLibrariesIfSlimTestAndIncludeVariableSetToFalse() throws Exception {
    WikiPage slimTestPage = addPage("SlimTest", define("TEST_SYSTEM", "slim") + define("INCLUDE_SCENARIO_LIBRARIES", "false"));
    TestPage testPage = new WikiTestPage(slimTestPage);
    addPage("ScenarioLibrary", "scenario library");
    String html = testPage.getHtml();
    assertNotSubString("scenario library", html);
  }

  @Test
  public void testPathSeparatorVariable() throws Exception {
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("TestPage"),
            define("PATH_SEPARATOR", "|") + """
            ```agentnesse
            path
            fitnesse.jar
            my.jar
            ```
            """);
    PageData data = page.getData();
    page.commit(data);

    String expected = "fitnesse.jar" + "|" + "my.jar";
    assertEquals(expected, new WikiTestPage(page).getClassPath().toString());
  }

}
