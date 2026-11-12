package fitnesse.markdown;

import fitnesse.wiki.BaseWikiPage;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PageType;
import fitnesse.wiki.VersionInfo;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageLoadException;
import fitnesse.wiki.WikiPageProperty;
import fitnesse.wiki.WikiPageFactory;
import fitnesse.wiki.fs.FileBasedWikiPage;
import fitnesse.wiki.fs.FileSystem;
import fitnesse.wiki.fs.FileSystemSubWikiPageFactory;
import fitnesse.wiki.fs.FileVersion;
import fitnesse.wiki.fs.SubWikiPageFactory;
import fitnesse.wiki.fs.WikiFiles;
import fitnesse.wikitext.VariableSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * One Markdown file. Children are not stored on this page yet.
 */
public class MarkdownPage extends BaseWikiPage implements FileBasedWikiPage {
  static final String EXTENSION = ".md";
  static final String ROOT_FILE = "_root.md";

  private final File path;
  private final FileSystem fileSystem;
  private final WikiFiles versions;
  private final VariableSource variableSource;
  private final WikiPageFactory pages;
  private PageData pageData;

  MarkdownPage(File path, String name, WikiPage parent, FileSystem fileSystem, WikiFiles versions,
               VariableSource variableSource, WikiPageFactory pages) {
    super(name, parent);
    this.path = path;
    this.fileSystem = fileSystem;
    this.versions = versions;
    this.variableSource = variableSource;
    this.pages = pages;
  }

  @Override
  public File getFileSystemPath() {
    if (ROOT_FILE.equals(path.getName())) {
      return path.getParentFile();
    }
    String pathStr = path.getPath();
    return new File(pathStr.substring(0, pathStr.length() - EXTENSION.length()));
  }

  @Override
  public WikiPage addChildPage(String name) {
    return new MarkdownPage(new File(getFileSystemPath(), name + EXTENSION), name, this, fileSystem, versions, variableSource, pages);
  }

  @Override
  public WikiPage getChildPage(String name) {
    return subPages().getChildPage(this, name);
  }

  @Override
  public void removeChildPage(String name) {
    WikiPage child = getChildPage(name);
    if (child != null) {
      child.remove();
    }
  }

  @Override
  public void remove() {
    try {
      versions.delete(path);
    } catch (IOException e) {
      throw new WikiPageLoadException("Could not remove " + getName(), e);
    }
  }

  @Override
  public List<WikiPage> getChildren() {
    return subPages().getChildren(this);
  }

  private SubWikiPageFactory subPages() {
    return new FileSystemSubWikiPageFactory(getFileSystemPath(), fileSystem, variableSource, pages);
  }

  @Override
  public PageData getData() {
    if (pageData == null) {
      pageData = load();
    }
    return new PageData(pageData);
  }

  @Override
  public Collection<VersionInfo> getVersions() {
    return Collections.emptyList();
  }

  @Override
  public WikiPage getVersion(String versionName) {
    throw new WikiPageLoadException("Page versions are not kept: " + versionName);
  }

  @Override
  public String getHtml() {
    return getData().getContent();
  }

  @Override
  public VersionInfo commit(PageData data) {
    pageData = null;
    try {
      return versions.makeVersion(new MarkdownFile(data));
    } catch (IOException e) {
      throw new WikiPageLoadException("Could not write " + path.getPath(), e);
    }
  }

  @Override
  public String getVariable(String name) {
    return null;
  }

  private PageData load() {
    FrontMatter matter = FrontMatter.read(readFile());
    WikiPageProperty properties = propertiesFrom(matter);
    properties.setLastModificationTime(new Date(fileSystem.lastModified(path)));
    return new PageData(matter.body(), properties);
  }

  private String readFile() {
    if (!fileSystem.exists(path)) {
      return "";
    }
    try {
      return fileSystem.getContent(path);
    } catch (IOException e) {
      throw new WikiPageLoadException("Could not read " + path.getPath(), e);
    }
  }

  private WikiPageProperty propertiesFrom(FrontMatter matter) {
    WikiPageProperty properties = new WikiPageProperty();
    properties.set(WikiPageProperty.EDIT);
    properties.set(WikiPageProperty.PROPERTIES);
    properties.set(WikiPageProperty.REFACTOR);
    properties.set(WikiPageProperty.WHERE_USED);
    properties.set(WikiPageProperty.RECENT_CHANGES);
    properties.set(WikiPageProperty.FILES);
    properties.set(WikiPageProperty.VERSIONS);
    properties.set(WikiPageProperty.SEARCH);
    applyType(properties, resolvedType(matter));
    if (!matter.tags().isEmpty()) {
      properties.set(WikiPageProperty.SUITES, String.join(", ", matter.tags()));
    }
    if (matter.help() != null) {
      properties.set(WikiPageProperty.HELP, matter.help());
    }
    if (matter.prune()) {
      properties.set(WikiPageProperty.PRUNE);
    }
    return properties;
  }

  private String resolvedType(FrontMatter matter) {
    if (matter.type() != null) {
      return matter.type();
    }
    return PageType.getPageTypeForPageName(getName()).toString().toLowerCase();
  }

  private String typeOf(PageData data) {
    if (data.hasAttribute(PageType.SUITE.toString())) {
      return "suite";
    }
    if (data.hasAttribute(PageType.TEST.toString())) {
      return "test";
    }
    return "static";
  }

  private List<String> tagsOf(PageData data) {
    String suites = data.getAttribute(WikiPageProperty.SUITES);
    if (suites == null || suites.isBlank()) {
      return List.of();
    }
    return Arrays.stream(suites.split("\\s*,\\s*")).filter(tag -> !tag.isEmpty()).toList();
  }

  private void applyType(WikiPageProperty properties, String type) {
    if ("test".equals(type)) {
      properties.set(PageType.TEST.toString());
    } else if ("suite".equals(type)) {
      properties.set(PageType.SUITE.toString());
    }
  }

  private final class MarkdownFile implements FileVersion {
    private final PageData data;

    private MarkdownFile(PageData data) {
      this.data = new PageData(data);
    }

    @Override
    public File getFile() {
      return path;
    }

    @Override
    public InputStream getContent() {
      String text = FrontMatter.write(typeOf(data), tagsOf(data), data.getAttribute(WikiPageProperty.HELP),
        data.hasAttribute(WikiPageProperty.PRUNE), data.getContent());
      return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getAuthor() {
      return data.getAttribute(WikiPageProperty.LAST_MODIFYING_USER);
    }

    @Override
    public Date getLastModificationTime() {
      return data.getProperties().getLastModificationTime();
    }
  }
}
