package fitnesse.wiki.fs;

import fitnesse.wiki.*;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class FileSystemPageFactoryTest {
  private FileSystem fileSystem;
  private FileSystemPageFactory fileSystemPageFactory;
  private WikiPage rootPage;

  @Before
  public void SetUp() throws Exception {
    fileSystem = new MemoryFileSystem();
    fileSystemPageFactory = new FileSystemPageFactory(fileSystem, new WikiFiles(fileSystem));
    fileSystem.makeFile(new File("./somepath/content.txt"), "");
    rootPage = fileSystemPageFactory.makePage(new File("./somepath"), "somepath", null, new SystemVariableSource());
  }

  @Test
  public void DirectoryWithoutHtmlFilesIsWikiFilePage() throws Exception {
    fileSystem.makeFile(new File("./somepath/WikiPage/myfile.txt"), "stuff");
    fileSystem.makeFile(new File("./somepath/OtherPage/myfile.html"), "stuff");
    WikiPage page = rootPage.addChildPage("WikiPage");
    assertEquals(WikiFilePage.class, page.getClass());
  }

  @Test
  public void DirectoryWithContentIsFileSystemPage() throws Exception {
    fileSystem.makeFile(new File("./somepath/WikiPage/content.txt"), "stuff");
    fileSystem.makeFile(new File("./somepath/WikiPage/subsuite/myfile.html"), "stuff");
    WikiPage page = rootPage.addChildPage("WikiPage");
    assertEquals(FileSystemPage.class, page.getClass());
  }

  @Test
  public void FileWithWikiExtensionIsNewFileSystemPage() throws Exception {
    fileSystem.makeFile(new File("./somepath/WikiPage.wiki"), "stuff");
    fileSystem.makeFile(new File("./somepath/WikiPage/subsuite/myfile.html"), "stuff");
    WikiPage page = rootPage.addChildPage("WikiPage");
    assertEquals(WikiFilePage.class, page.getClass());
  }

  @Test
  public void NestedWikiFileIsNewFileSystemPage() throws Exception {
    fileSystem.makeFile(new File("./somepath/WikiPage/content.txt"), "stuff");
    fileSystem.makeFile(new File("./somepath/WikiPage/subsuite.wiki"), "stuff");
    fileSystem.makeFile(new File("./somepath/WikiPage/subsuite/myfile.html"), "stuff");
    WikiPage page = rootPage.getPageCrawler().getPage(PathParser.parse("WikiPage.subsuite"));
    assertEquals(WikiFilePage.class, page.getClass());
  }

  @Test
  public void NestedWikiFileInOldStyleDirectoryIsNewFileSystemPage() throws Exception {
    fileSystem.makeFile(new File("./somepath/WikiPage.wiki"), "stuff");
    fileSystem.makeFile(new File("./somepath/WikiPage/subsuite.wiki"), "stuff");
    fileSystem.makeFile(new File("./somepath/WikiPage/subsuite/myfile.html"), "stuff");
    WikiPage page = rootPage.getPageCrawler().getPage(PathParser.parse("WikiPage.subsuite"));
    assertEquals(WikiFilePage.class, page.getClass());
  }

  @Test
  public void OldStyleDirectoryInNestedWikiFileIsFileSystemPage() throws Exception {
    fileSystem.makeFile(new File("./somepath/WikiPage.wiki"), "stuff");
    fileSystem.makeFile(new File("./somepath/WikiPage/SubPage/content.txt"), "stuff");
    fileSystem.makeFile(new File("./somepath/WikiPage/SubPage/subsuite/myfile.html"), "stuff");
    WikiPage page = rootPage.getPageCrawler().getPage(PathParser.parse("WikiPage.SubPage"));
    assertEquals(FileSystemPage.class, page.getClass());
  }

}
