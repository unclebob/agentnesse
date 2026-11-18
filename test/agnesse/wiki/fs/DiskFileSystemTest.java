package agnesse.wiki.fs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import agnesse.testutil.AgNesseUtil;

import static org.junit.Assert.*;

public class DiskFileSystemTest {

  private File rootDir;

  @Before
  public void setUp() {
    rootDir = AgNesseUtil.createTemporaryFolder();
  }

  @Test
  public void findFilesAndFolders() throws IOException {
    new File(rootDir, "DirName").createNewFile();
    new File(rootDir, "filename").createNewFile();
    new File(rootDir, "CVS").createNewFile();

    List<String> listing = Arrays.asList(new DiskFileSystem().list(rootDir));

    assertEquals(listing.toString(), 2, listing.size());
    assertTrue("no DirName found", listing.contains("DirName"));
    assertTrue("no filename found", listing.contains("filename"));
  }

  @Test
  public void aDirectoryInsideAFileCannotBeMade() throws IOException {
    File file = new File(rootDir, "not-a-dir");
    assertTrue(file.createNewFile());

    try {
      new DiskFileSystem().makeDirectory(new File(file, "child"));
      fail("expected the directory to be refused");
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("make directory failed"));
      assertTrue(e.getMessage().contains("not-a-dir"));
    }
  }

  @Test
  public void aRenameIntoAMissingDirectoryIsRefused() throws IOException {
    File source = new File(rootDir, "filename");
    assertTrue(source.createNewFile());
    File destination = new File(new File(rootDir, "missing"), "renamed");

    try {
      new DiskFileSystem().rename(destination, source);
      fail("expected the rename to be refused");
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("file rename failed"));
      assertTrue(e.getMessage().contains("filename"));
    }
  }
}
