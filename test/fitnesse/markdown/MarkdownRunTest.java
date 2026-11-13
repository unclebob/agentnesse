package fitnesse.markdown;

import fitnesse.testrunner.ClassPathBuilder;
import fitnesse.testrunner.TestPageWithSuiteSetUpAndTearDown;
import fitnesse.testsystems.slim.HtmlCells;
import fitnesse.wiki.SystemVariableSource;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.fs.FileSystemPageFactory;
import fitnesse.wiki.fs.MemoryFileSystem;
import fitnesse.wiki.fs.WikiFiles;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MarkdownRunTest {
  private MemoryFileSystem fileSystem;
  private WikiPage root;

  @Before
  public void setUp() throws Exception {
    fileSystem = new MemoryFileSystem();
    fileSystem.makeFile(new File("root/_root.md"), "");
    root = new FileSystemPageFactory(fileSystem, new WikiFiles(fileSystem))
      .makePage(new File("root"), "root", null, new SystemVariableSource());
  }

  @Test
  public void classpathComesFromPathBlocksOnThisPageAndItsParents() throws Exception {
    fileSystem.makeFile(new File("root/Parent.md"), """
      ```agentnesse
      path
      from-parent.jar
      ```
      ```agentnesse
      define SUT
      !path from-define.jar
      ```
      """);
    fileSystem.makeFile(new File("root/Parent/TestChild.md"), """
      ```agentnesse
      path
      from-child.jar
      ```
      ${SUT}
      """);

    var classpath = new ClassPathBuilder().getClassPath(root.getChildPage("Parent").getChildPage("TestChild"));

    assertEquals("from-child.jar", classpath.get(0));
    assertEquals("from-define.jar", classpath.get(1));
    assertEquals("from-parent.jar", classpath.get(2));
  }

  @Test
  public void aMarkdownTestRunIncludesSetupAndSuiteSetupByName() throws Exception {
    fileSystem.makeFile(new File("root/SetUp.md"), """
      | import | |
      | --- | --- |
      | fitnesse.fixtures | |
      """);
    fileSystem.makeFile(new File("root/SuiteSetUp.md"), """
      | script | |
      | --- | --- |
      | suite setup ran | |
      """);
    fileSystem.makeFile(new File("root/TestChild.md"), """
      | script | |
      | --- | --- |
      | check | x |
      """);

    String html = new TestPageWithSuiteSetUpAndTearDown(root.getChildPage("TestChild")).getHtml();
    assertTrue(HtmlCells.contains(html, "import"));
    assertTrue(HtmlCells.contains(html, "suite setup ran"));
    assertTrue(HtmlCells.contains(html, "check"));
    assertTrue(html.contains("agentnesse-include"));
  }

  @Test
  public void anIncludeBlockInsertsTheOtherPageCollapsed() throws Exception {
    fileSystem.makeFile(new File("root/Included.md"), """
      | query:Employees | |
      | --- | --- |
      | name | hired |
      """);
    fileSystem.makeFile(new File("root/TestHost.md"), """
      ```agentnesse
      include Included
      ```
      """);

    String html = root.getChildPage("TestHost").getHtml();

    assertTrue(html.contains("<details class=\"agentnesse-include\"><summary>Included</summary>"));
    assertEquals("query:Employees", HtmlCells.cell(html, 0, 0, 0));
  }
}
