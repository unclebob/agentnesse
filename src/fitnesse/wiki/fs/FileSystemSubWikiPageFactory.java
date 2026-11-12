package fitnesse.wiki.fs;

import java.io.File;
import java.util.*;

import fitnesse.wiki.*;
import fitnesse.wikitext.VariableSource;

public class FileSystemSubWikiPageFactory implements SubWikiPageFactory {

  private final File rootPath;
  private final FileSystem fileSystem;
  private final VariableSource variableSource;
  private final WikiPageFactory factory;

  public FileSystemSubWikiPageFactory(File rootPath, FileSystem fileSystem, VariableSource variableSource,
                                      WikiPageFactory factory) {
    this.rootPath = rootPath;
    this.fileSystem = fileSystem;
    this.variableSource = variableSource;
    this.factory = factory;
  }

  @Override
  public List<WikiPage> getChildren(FileBasedWikiPage page) {
    return new ArrayList<>(getNormalChildren(page));
  }

  private Set<WikiPage> getNormalChildren(FileBasedWikiPage page) {
    final File thisDir = page.getFileSystemPath();
    final Set<WikiPage> children = new TreeSet<>();
    if (fileSystem.exists(thisDir)) {
      final String[] subFiles = fileSystem.list(thisDir);
      for (String subFile : subFiles) {
        WikiPage maybeChildPage = getChildPage(page, subFile);
        if (maybeChildPage != null) {
          children.add(maybeChildPage);
        }
      }
    }
    return children;
  }

  @Override
  public WikiPage getChildPage(FileBasedWikiPage page, String childName) {
    File parent = page.getFileSystemPath();

    return makeChildPage(new File(parent, childName), childName, page);
  }

  public FileSystem fileSystem() {
    return fileSystem;
  }

  public WikiPageFactory pages() {
    return factory;
  }

  private WikiPage makeChildPage(File path, String childName, FileBasedWikiPage parent) {
     return factory.makePage(path, childName, parent, variableSource);
  }
}
