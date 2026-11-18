// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class FileUtilTest {
  @Test
  public void closeIgnoresANullStream() {
    FileUtil.close(null);
  }

  @Test
  public void closeSwallowsAnIoFailureAndReportsTheStream() {
    Logger log = Logger.getLogger(FileUtil.class.getName());
    List<String> messages = new ArrayList<>();
    Handler handler = new Handler() {
      @Override
      public void publish(LogRecord record) {
        messages.add(record.getMessage());
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() {
      }
    };
    Level previous = log.getLevel();
    log.setLevel(Level.INFO);
    log.addHandler(handler);
    Closeable broken = new Closeable() {
      @Override
      public void close() throws IOException {
        throw new IOException("nope");
      }

      @Override
      public String toString() {
        return "broken-stream";
      }
    };
    try {
      FileUtil.close(broken);
    } finally {
      log.removeHandler(handler);
      log.setLevel(previous);
    }
    assertEquals(List.of("Unable to close broken-stream"), messages);
  }

  @Test
  public void deleteFileReportsADirectoryThatStillHasChildren() throws Exception {
    File dir = FileUtil.createDir("temp-delete-fails");
    FileUtil.createFile(new File(dir, "child.txt"), "x");
    try {
      FileUtil.deleteFile(dir);
      fail("expected delete to fail");
    } catch (IOException e) {
      assertEquals("Could not delete '" + dir.getAbsolutePath() + "'", e.getMessage());
    } finally {
      FileUtil.deleteFileSystemDirectory(dir);
    }
  }

  @Test
  public void writesAndReadsLines() throws Exception {
    File file = FileUtil.createFile("temp-lines.txt", "");
    FileUtil.writeLinesToFile(file, Arrays.asList("one", "two"));
    assertEquals(Arrays.asList("one", "two"), FileUtil.getFileLines(file));
    file.delete();
  }

  @Test
  public void testCreateDir() throws Exception {
    File dir = FileUtil.createDir("temp1");
    assertTrue(dir.exists());
    assertTrue(dir.isDirectory());
    FileUtil.deleteFileSystemDirectory(dir);
  }

  @Test
  public void createFileWithComplexPath() throws Exception {
    File file = FileUtil.createFile("temp/sub1/sub2/sub3/file", "");
    assertTrue(file.exists());
    FileUtil.deleteFileSystemDirectory("temp");
  }

  @Test
  public void testGetDirectoryListingEmpty() throws Exception {
    File dir = FileUtil.createDir("temp2");
    assertEquals(0, FileUtil.getDirectoryListing(dir).length);
    FileUtil.deleteFileSystemDirectory(dir);
  }

  @Test
  public void testOrganizeFilesOneFile() throws Exception {
    File dir = FileUtil.createDir("temp3");
    File file = createFileInDir(dir, "file.txt");
    assertEquals(1, FileUtil.getDirectoryListing(dir).length);
    assertEquals(file, FileUtil.getDirectoryListing(dir)[0]);
    FileUtil.deleteFileSystemDirectory(dir);
  }

  @Test
    public void testOrganizeFilesFiveFiles() throws Exception {
    File dir = FileUtil.createDir("temp4");
    File file3 = createFileInDir(dir, "dFile.txt");
    File file1 = createFileInDir(dir, "bFile.txt");
    File file4 = createFileInDir(dir, "eFile.txt");
    File file0 = createFileInDir(dir, "aFile.txt");
    File file2 = createFileInDir(dir, "cFile.txt");
    assertEquals(5, FileUtil.getDirectoryListing(dir).length);
    assertEquals(file0, FileUtil.getDirectoryListing(dir)[0]);
    assertEquals(file1, FileUtil.getDirectoryListing(dir)[1]);
    assertEquals(file2, FileUtil.getDirectoryListing(dir)[2]);
    assertEquals(file3, FileUtil.getDirectoryListing(dir)[3]);
    assertEquals(file4, FileUtil.getDirectoryListing(dir)[4]);
    FileUtil.deleteFileSystemDirectory(dir);
  }

  @Test
    public void testOrganizeFilesOneSubDir() throws Exception {
    File dir = FileUtil.createDir("temp5");
    File subDir = createSubDir(dir, "subDir");
    assertEquals(1, FileUtil.getDirectoryListing(dir).length);
    assertEquals(subDir, FileUtil.getDirectoryListing(dir)[0]);
    FileUtil.deleteFileSystemDirectory(dir);
  }

  @Test
    public void testOrganizeFilesFiveSubDirs() throws Exception {
    File dir = FileUtil.createDir("temp6");
    File dir3 = createSubDir(dir, "dDir");
    File dir1 = createSubDir(dir, "bDir");
    File dir4 = createSubDir(dir, "eDir");
    File dir0 = createSubDir(dir, "aDir");
    File dir2 = createSubDir(dir, "cDir");
    assertEquals(5, FileUtil.getDirectoryListing(dir).length);
    assertEquals(dir0, FileUtil.getDirectoryListing(dir)[0]);
    assertEquals(dir1, FileUtil.getDirectoryListing(dir)[1]);
    assertEquals(dir2, FileUtil.getDirectoryListing(dir)[2]);
    assertEquals(dir3, FileUtil.getDirectoryListing(dir)[3]);
    assertEquals(dir4, FileUtil.getDirectoryListing(dir)[4]);
    FileUtil.deleteFileSystemDirectory(dir);
  }

  @Test
    public void testOrganizeFilesMixOfFilesAndDirs() throws IOException {
    File dir = FileUtil.createDir("temp7");
    File dir3 = createSubDir(dir, "dDir");
    File file3 = createFileInDir(dir, "dFile.txt");
    File file0 = createFileInDir(dir, "aFile.txt");
    File dir1 = createSubDir(dir, "bDir");
    File file4 = createFileInDir(dir, "eFile.txt");
    File dir4 = createSubDir(dir, "eDir");
    File dir0 = createSubDir(dir, "aDir");
    File file1 = createFileInDir(dir, "bFile.txt");
    File dir2 = createSubDir(dir, "cDir");
    File file2 = createFileInDir(dir, "cFile.txt");
    assertEquals(10, FileUtil.getDirectoryListing(dir).length);
    assertEquals(dir0, FileUtil.getDirectoryListing(dir)[0]);
    assertEquals(dir1, FileUtil.getDirectoryListing(dir)[1]);
    assertEquals(dir2, FileUtil.getDirectoryListing(dir)[2]);
    assertEquals(dir3, FileUtil.getDirectoryListing(dir)[3]);
    assertEquals(dir4, FileUtil.getDirectoryListing(dir)[4]);
    assertEquals(file0, FileUtil.getDirectoryListing(dir)[5]);
    assertEquals(file1, FileUtil.getDirectoryListing(dir)[6]);
    assertEquals(file2, FileUtil.getDirectoryListing(dir)[7]);
    assertEquals(file3, FileUtil.getDirectoryListing(dir)[8]);
    assertEquals(file4, FileUtil.getDirectoryListing(dir)[9]);
    FileUtil.deleteFileSystemDirectory(dir);
  }

  @Test
  public void readsFileContentFromAPath() throws IOException {
    File file = FileUtil.createFile("temp-content/note.txt", "hello");
    assertEquals("hello", FileUtil.getFileContent(file.getPath()));
    FileUtil.deleteFileSystemDirectory("temp-content");
  }

  @Test
  public void reportsWhetherADirectoryIsEmpty() throws IOException {
    File dir = FileUtil.createDir("temp-empty");
    assertTrue(FileUtil.isEmpty(dir));
    createFileInDir(dir, "note.txt");
    assertFalse(FileUtil.isEmpty(dir));
    FileUtil.deleteFileSystemDirectory(dir);
  }

  private File createFileInDir(File dir, String fileName) throws IOException {
    return FileUtil.createFile(new File(dir.getPath(), fileName), "");
  }

  private File createSubDir(File dir, String subDirName) {
    File d = new File(dir.getPath(), subDirName);
    d.mkdirs();
    return d;
  }

}

