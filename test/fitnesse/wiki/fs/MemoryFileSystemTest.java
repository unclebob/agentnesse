package fitnesse.wiki.fs;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fitnesse.wiki.fs.MemoryFileSystem;
import org.junit.Before;
import org.junit.Test;

public class MemoryFileSystemTest {

  private static final File pageOne = new File("PageOne");
  private static final File contentFile = new File(pageOne, "body.md");

  MemoryFileSystem fileSystem;

  @Before
  public void setUp() {
    fileSystem = new MemoryFileSystem();

    fileSystem.makeDirectory(pageOne);
    fileSystem.makeFile(contentFile, "content");
  }

  @Test
  public void shouldAddPages() {
    assertTrue(fileSystem.exists(pageOne));
    assertTrue(fileSystem.exists(contentFile));
  }

  @Test
  public void shouldDeletePages() {
    fileSystem.delete(pageOne);

    assertFalse(fileSystem.exists(pageOne));
    assertFalse(fileSystem.exists(contentFile));
  }

  @Test
  public void deletingAPageDoesNotDeleteAPageWhoseNameStartsWithThatName() {
    File pageOneNew = new File("PageOneNew");
    File pageOneFile = new File("PageOne.md");
    fileSystem.makeDirectory(pageOneNew);
    fileSystem.makeFile(new File(pageOneNew, "child.md"), "child");
    fileSystem.makeFile(pageOneFile, "page");

    fileSystem.delete(pageOne);

    assertFalse(fileSystem.exists(pageOne));
    assertFalse(fileSystem.exists(contentFile));
    assertTrue(fileSystem.exists(pageOneNew));
    assertTrue(fileSystem.exists(new File(pageOneNew, "child.md")));
    assertTrue(fileSystem.exists(pageOneFile));
  }
}
