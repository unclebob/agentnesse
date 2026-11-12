package fitnesse.wiki.fs;

import fitnesse.wiki.VersionInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * Writes wiki files in place. There is no page history.
 */
public class WikiFiles {

  private final FileSystem fileSystem;

  public WikiFiles(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  public WikiFiles() {
    this(new DiskFileSystem());
  }

  public FileVersion[] getRevisionData(String label, File... files) {
    FileVersion[] versions = new FileVersion[files.length];
    int counter = 0;
    for (File file : files) {
      if (fileSystem.exists(file)) {
        versions[counter++] = new CurrentFile(file);
      }
    }
    return versions;
  }

  public Collection<VersionInfo> history(File... files) {
    return Collections.emptyList();
  }

  public VersionInfo makeVersion(FileVersion... fileVersions) throws IOException {
    for (FileVersion fileVersion : fileVersions) {
      ensureDirectory(fileVersion.getFile().getParentFile());
      try (InputStream content = fileVersion.getContent()) {
        fileSystem.makeFile(fileVersion.getFile(), content);
      }
    }
    return VersionInfo.makeVersionInfo(fileVersions[0].getAuthor(), fileVersions[0].getLastModificationTime());
  }

  public void delete(File... files) throws IOException {
    for (File file : files) {
      fileSystem.delete(file);
    }
  }

  public VersionInfo addDirectory(FileVersion dir) throws IOException {
    File filePath = dir.getFile();
    ensureDirectory(filePath);
    return VersionInfo.makeVersionInfo(dir.getAuthor(), new Date(fileSystem.lastModified(filePath)));
  }

  public void rename(FileVersion fileVersion, File oldFile) throws IOException {
    fileSystem.rename(fileVersion.getFile(), oldFile);
  }

  private void ensureDirectory(File filePath) throws IOException {
    if (!fileSystem.exists(filePath)) {
      fileSystem.makeDirectory(filePath);
    }
  }

  private class CurrentFile implements FileVersion {
    private final File file;

    private CurrentFile(File file) {
      this.file = file;
    }

    @Override
    public File getFile() {
      return file;
    }

    @Override
    public InputStream getContent() throws IOException {
      return new BufferedInputStream(fileSystem.getInputStream(file));
    }

    @Override
    public String getAuthor() {
      return "";
    }

    @Override
    public Date getLastModificationTime() {
      return new Date(fileSystem.lastModified(file));
    }
  }
}
