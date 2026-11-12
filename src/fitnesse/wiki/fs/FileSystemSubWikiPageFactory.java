package fitnesse.wiki.fs;

import java.io.File;
import java.util.*;

import fitnesse.wiki.*;
import fitnesse.wiki.VariableSource;

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
    rejectDuplicate(parent, childName);
    return makeChildPage(new File(parent, childName), childName, page);
  }

  private void rejectDuplicate(File parent, String childName) {
    String base = pageBase(childName);
    if (base == null) {
      return;
    }
    File markdown = new File(parent, base + ".md");
    File wiki = new File(parent, base + OldPageFiles.WIKI);
    File content = new File(new File(parent, base), OldPageFiles.CONTENT);
    if (isFile(markdown) && isFile(wiki)) {
      throw new WikiPageLoadException(markdown.getPath() + " and " + wiki.getPath() + " both exist");
    }
    if (isFile(markdown) && isFile(content)) {
      throw new WikiPageLoadException(markdown.getPath() + " and " + content.getPath() + " both exist");
    }
  }

  private String pageBase(String childName) {
    if (childName.endsWith(".md")) {
      return childName.substring(0, childName.length() - 3);
    }
    if (childName.endsWith(OldPageFiles.WIKI)) {
      return childName.substring(0, childName.length() - OldPageFiles.WIKI.length());
    }
    if (childName.indexOf('.') >= 0) {
      return null;
    }
    return childName;
  }

  private boolean isFile(File path) {
    return fileSystem.exists(path) && !fileSystem.isDirectory(path);
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
