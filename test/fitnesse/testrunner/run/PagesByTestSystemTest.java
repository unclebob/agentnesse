package fitnesse.testrunner.run;

import fitnesse.FitNesseContext;
import fitnesse.testrunner.WikiPageIdentity;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.testsystems.TestPage;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PagesByTestSystemTest {
  private WikiPage suite;
  private FitNesseContext context;

  @Before
  public void setUp() throws Exception {
    context = FitNesseUtil.makeTestContext();
    suite = WikiPageUtil.addPage(context.getRootPage(), PathParser.parse("SuitePage"), "This is the test suite\n");
  }

  @Test
  public void testPagesForTestSystemAreSurroundedBySuiteSetupAndTeardown() throws Exception {
    WikiPage root = context.getRootPage();
    WikiPage testPage = addTestPage(suite, "TestOne", "My test");
    WikiPage slimPage = addTestPage(suite, "AaSlimTest", "!define TEST_SYSTEM {slim}\n" +
            "|!-DT:fitnesse.slim.test.TestSlim-!|\n" +
            "|string|get string arg?|\n" +
            "|wow|wow|\n");
    WikiPage setUp = WikiPageUtil.addPage(root, PathParser.parse("SuiteSetUp"), "suite set up");
    WikiPage tearDown = WikiPageUtil.addPage(root, PathParser.parse("SuiteTearDown"), "suite tear down");

    LinkedList<WikiPage> testPages = new LinkedList<>();
    testPages.add(setUp);
    testPages.add(slimPage);
    testPages.add(testPage);
    testPages.add(tearDown);

    PagesByTestSystem pagesByTestSystem = new PagesByTestSystem(testPages);
    Collection<WikiPageIdentity> descriptors = pagesByTestSystem.identities();
    WikiPageIdentity slimDescriptor = new WikiPageIdentity(slimPage);

    assertEquals(slimDescriptor, new WikiPageIdentity(testPage));
    assertTrue(descriptors.contains(slimDescriptor));
    assertEquals(1, descriptors.size());

    List<TestPage> slimList = pagesByTestSystem.testPagesForIdentity(slimDescriptor);

    assertEquals(4, slimList.size());
    assertEquals(setUp, ((WikiTestPage) slimList.get(0)).getSourcePage());
    assertEquals(slimPage, ((WikiTestPage) slimList.get(1)).getSourcePage());
    assertEquals(testPage, ((WikiTestPage) slimList.get(2)).getSourcePage());
    assertEquals(tearDown, ((WikiTestPage) slimList.get(3)).getSourcePage());

    assertEquals(4, pagesByTestSystem.totalTestsToRun());
    assertEquals(4, pagesByTestSystem.testsToRun().size());
  }

  @Test
  public void testInserterChosen() {
    suite = WikiPageUtil.addPage(context.getRootPage(),
      PathParser.parse("SuitePage"),
      "This is the altered test suite\n!define ALL_UNCLE_SUITE_SETUPS {true}\n");

    PageListSetUpTearDownProcessor processor = PagesByTestSystem.createProcessor(Collections.singletonList(suite));
    assertEquals(PageListSetUpTearDownInserter.class, processor.getClass());
  }

  @Test
  public void testSurrounderChosen() {
    PageListSetUpTearDownProcessor processor = PagesByTestSystem.createProcessor(Collections.singletonList(suite));
    assertEquals(PageListSetUpTearDownSurrounder.class, processor.getClass());
  }

  private WikiPage addTestPage(WikiPage page, String name, String content) {
    WikiPage testPage = WikiPageUtil.addPage(page, PathParser.parse(name), content);
    PageData data = testPage.getData();
    data.setAttribute("Test");
    testPage.commit(data);
    return testPage;
  }

}
