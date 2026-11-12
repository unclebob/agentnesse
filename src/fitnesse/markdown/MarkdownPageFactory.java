package fitnesse.markdown;

import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageFactory;
import fitnesse.wiki.fs.FileSystem;
import fitnesse.wiki.fs.WikiFiles;
import fitnesse.wikitext.VariableSource;

import java.io.File;

public class MarkdownPageFactory implements WikiPageFactory {
  private final FileSystem fileSystem;
  private final WikiFiles versions;
  private final WikiPageFactory pages;

  public MarkdownPageFactory(FileSystem fileSystem, WikiFiles versions, WikiPageFactory pages) {
    this.fileSystem = fileSystem;
    this.versions = versions;
    this.pages = pages;
  }

  @Override
  public boolean supports(File path) {
    return isPageFile(path) || isPageFile(siblingFile(path)) || isRootFile(new File(path, MarkdownPage.ROOT_FILE));
  }

  @Override
  public WikiPage makePage(File path, String pageName, WikiPage parent, VariableSource variableSource) {
    File file = pageFile(path);
    String name = file.getName().equals(MarkdownPage.ROOT_FILE)
      ? pageName
      : (file == path ? pageName.substring(0, pageName.length() - MarkdownPage.EXTENSION.length()) : pageName);
    return new MarkdownPage(file, name, parent, fileSystem, versions, variableSource, pages);
  }

  private File pageFile(File path) {
    if (isPageFile(path)) {
      return path;
    }
    if (isPageFile(siblingFile(path))) {
      return siblingFile(path);
    }
    return new File(path, MarkdownPage.ROOT_FILE);
  }

  private boolean isPageFile(File path) {
    return path.getName().endsWith(MarkdownPage.EXTENSION)
      && !MarkdownPage.ROOT_FILE.equals(path.getName())
      && isFile(path);
  }

  private boolean isRootFile(File path) {
    return MarkdownPage.ROOT_FILE.equals(path.getName()) && isFile(path);
  }

  private boolean isFile(File path) {
    return fileSystem.exists(path) && !fileSystem.isDirectory(path);
  }

  private File siblingFile(File path) {
    return new File(path.getPath() + MarkdownPage.EXTENSION);
  }
}
