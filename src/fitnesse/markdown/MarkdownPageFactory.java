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

  public MarkdownPageFactory(FileSystem fileSystem, WikiFiles versions) {
    this.fileSystem = fileSystem;
    this.versions = versions;
  }

  @Override
  public boolean supports(File path) {
    return isPageFile(path) || isPageFile(siblingFile(path));
  }

  @Override
  public WikiPage makePage(File path, String pageName, WikiPage parent, VariableSource variableSource) {
    if (isPageFile(path)) {
      return new MarkdownPage(path, pageName.substring(0, pageName.length() - MarkdownPage.EXTENSION.length()), parent, fileSystem, versions);
    }
    return new MarkdownPage(siblingFile(path), pageName, parent, fileSystem, versions);
  }

  private boolean isPageFile(File path) {
    return path.getName().endsWith(MarkdownPage.EXTENSION)
      && fileSystem.exists(path)
      && !fileSystem.isDirectory(path);
  }

  private File siblingFile(File path) {
    return new File(path.getPath() + MarkdownPage.EXTENSION);
  }
}
