package fitnesse.wiki.fs;

import java.io.File;
import java.util.Properties;

import fitnesse.markdown.MarkdownPage;
import fitnesse.markdown.MarkdownPageFactory;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.VariableSource;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageFactory;
import fitnesse.wiki.WikiPageLoadException;

/**
 * Loads Markdown pages from the file system.
 */
public class FileSystemPageFactory implements WikiPageFactory {
  private final FileSystem fileSystem;
  private final WikiFiles versionsController;
  private final MarkdownPageFactory markdown;

  public FileSystemPageFactory() {
    this(new DiskFileSystem(), new WikiFiles());
  }

  public FileSystemPageFactory(Properties properties) {
    this();
  }

  public FileSystemPageFactory(FileSystem fileSystem, WikiFiles versionsController) {
    this.fileSystem = fileSystem;
    this.versionsController = versionsController;
    this.markdown = new MarkdownPageFactory(fileSystem, versionsController, this);
  }

  @Override
  public WikiPage makePage(File path, String pageName, WikiPage parent, VariableSource variableSource) {
    rejectRootConflict(path);
    if (markdown.supports(path)) {
      return markdown.makePage(path, pageName, parent, variableSource);
    }
    if (parent == null || (parent instanceof FileBasedWikiPage && isWikiWordDirectory(path))) {
      return new MarkdownPage(new File(path, MarkdownPage.ROOT_FILE), pageName, parent,
        fileSystem, versionsController, variableSource, this);
    }
    return null;
  }

  private void rejectRootConflict(File path) {
    File content = new File(path, OldPageFiles.CONTENT);
    File rootMarkdown = new File(path, MarkdownPage.ROOT_FILE);
    if (isFile(content) && isFile(rootMarkdown)) {
      throw new WikiPageLoadException(content.getPath() + " and " + rootMarkdown.getPath() + " both exist");
    }
  }

  private boolean isWikiWordDirectory(final File path) {
    return fileSystem.isDirectory(path) && PathParser.isSingleWikiWord(path.getName());
  }

  private boolean isFile(File path) {
    return fileSystem.exists(path) && !fileSystem.isDirectory(path);
  }

  WikiFiles getVersionsController() {
    return versionsController;
  }
}
