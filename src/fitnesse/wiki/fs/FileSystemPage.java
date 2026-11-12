// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.wiki.fs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import fitnesse.markdown.MarkdownPage;
import fitnesse.wiki.*;
import fitnesse.wikitext.VariableSource;
import util.FileUtil;

import static java.lang.String.format;

/**
 * This is the "old style" page format. content is stored as: WikiPageName/content.txt and WikiPageName/properties.xml.
 *
 * @see fitnesse.wiki.fs.WikiFilePage
 */
public class FileSystemPage extends BaseWikitextPage implements FileBasedWikiPage {

  static final String contentFilename = "content.txt";
  static final String propertiesFilename = "properties.xml";

  private final File path;
  private final transient WikiFiles versionsController;
  private final transient SubWikiPageFactory subWikiPageFactory;
  private final String versionName;
  private transient PageData pageData;

  public FileSystemPage(final File path, final String name,
                        final WikiFiles versionsController, final SubWikiPageFactory subWikiPageFactory,
                        final VariableSource variableSource) {
    super(name, variableSource);
    this.path = path;
    this.versionsController = versionsController;
    this.subWikiPageFactory = subWikiPageFactory;
    this.versionName = null;
  }

  private FileSystemPage(final File path, final String name, final FileSystemPage parent) {
    this(path, name, parent, null, parent.versionsController, parent.subWikiPageFactory, parent.getVariableSource());
  }

  private FileSystemPage(FileSystemPage page, String versionName) {
    this(page.getFileSystemPath(), page.getName(), (page.isRoot() ? null : page.getParent()), versionName,
            page.versionsController, page.subWikiPageFactory, page.getVariableSource());
  }

  protected FileSystemPage(final File path, final String name, final WikiPage parent, final String versionName,
                         final WikiFiles versionsController, final SubWikiPageFactory subWikiPageFactory,
                         final VariableSource variableSource) {
    super(name, parent, variableSource);
    this.path = path;
    this.versionsController = versionsController;
    this.subWikiPageFactory = subWikiPageFactory;
    this.versionName = versionName;
  }

  @Override
  public void removeChildPage(final String name) {
    final WikiPage childPage = getChildPage(name);
    if (childPage != null) {
      childPage.remove();
    }
  }

  @Override
  public void remove() {
    try {
      versionsController.delete(getFileSystemPath());
    } catch (IOException e) {
      throw new WikiPageLoadException(format("Could not remove page %s", new WikiPagePath(this).toString()), e);
    }
  }

  @Override
  public WikiPage addMarkdownChild(String name) {
    FileSystemSubWikiPageFactory children = (FileSystemSubWikiPageFactory) subWikiPageFactory;
    return new MarkdownPage(new File(getFileSystemPath(), name + MarkdownPage.EXTENSION), name, this,
      children.fileSystem(), versionsController, getVariableSource(), children.pages());
  }

  public WikiPage addChildPage(String pageName) {
    WikiPage page = getChildPage(pageName);
    if (page == null) {
      page = createPage(pageName);
    }
    return page;
  }

  private WikiPage createPage(final String pageName) {
    if ("true".equalsIgnoreCase(getVariable("wiki.page.old.style"))) {
      return new FileSystemPage(new File(getFileSystemPath(), pageName), pageName, this);
    } else {
      return new WikiFilePage(new File(getFileSystemPath(), pageName + WikiFilePage.FILE_EXTENSION), pageName, this,
        null, versionsController, subWikiPageFactory, getVariableSource());
    }
  }

  @Override
  public List<WikiPage> getChildren() {
    return subWikiPageFactory.getChildren(this);
  }

  @Override
  public WikiPage getChildPage(String childName) {
    return subWikiPageFactory.getChildPage(this, childName);
  }

  @Override
  public PageData getData() {
    if (pageData == null) {
      try {
        pageData = getDataVersion();
      } catch (IOException e) {
        throw new WikiPageLoadException("Could not load page data for page " + path.getPath(), e);
      }
    }
    return new PageData(pageData);
  }

  @Override
  public File getFileSystemPath() {
    return this.path;
  }

  @Override
  public VersionInfo commit(final PageData data) {
    // Note: RecentChanges is not handled by the versionsController?
    resetCache();
    try {
      return versionsController.makeVersion(new ContentFileVersion(data), new PropertiesFileVersion(data));
    } catch (IOException e) {
      throw new WikiPageLoadException(e);
    }
  }

  @Override
  protected void resetCache() {
    super.resetCache();
    pageData = null;
  }

  @Override
  public Collection<VersionInfo> getVersions() {
    return versionsController.history(contentFile(), propertiesFile());
  }

  private PageData getDataVersion() throws IOException {
    FileVersion[] versions = versionsController.getRevisionData(versionName, contentFile(), propertiesFile());
    String content = "";
    WikiPageProperty properties = null;
    try {
      for (FileVersion version : versions) {
        if (version == null) continue;
        if (contentFilename.equals(version.getFile().getName())) {
          content = loadContent(version);
        } else if (propertiesFilename.equals(version.getFile().getName())) {
          properties = loadAttributes(version);
        }
      }
    } catch (IOException e) {
      throw new WikiPageLoadException(e);
    }

    if (properties == null) {
      properties = defaultPageProperties();
    }
    return new PageData(content, properties);
  }

  @Override
  public WikiPage getVersion(String versionName) {
    throw new WikiPageLoadException("Page versions are not kept: " + versionName);
  }

  @Override
  public String toString() {
    try {
      return getClass().getName() + " at " + this.getFileSystemPath() + "#" + (versionName != null ? versionName : "latest");
    } catch (final Exception e) {
      return super.toString();
    }
  }

  private File contentFile() {
    return new File(getFileSystemPath(), contentFilename);
  }

  private File propertiesFile() {
    return new File(getFileSystemPath(), propertiesFilename);
  }

  private String loadContent(final FileVersion fileVersion) throws IOException {
    try (InputStream content = fileVersion.getContent()) {
      return FileUtil.toString(content);
    }
  }

  private WikiPageProperties loadAttributes(final FileVersion fileVersion) throws IOException {
    final WikiPageProperties props = new WikiPageProperties();
    try (InputStream content = fileVersion.getContent()) {
      props.loadFromXmlStream(content);
    }
    props.setLastModificationTime(fileVersion.getLastModificationTime());
    return props;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof FileSystemPage)) return false;

    FileSystemPage that = (FileSystemPage) other;

    return versionName != null ? versionName.equals(that.versionName) : that.versionName == null && super.equals(that);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (versionName != null ? versionName.hashCode() : 0);
    return result;
  }

  class ContentFileVersion implements FileVersion {

    final PageData data;

    ContentFileVersion(PageData pageData) {
      this.data = new PageData(pageData);
    }

    @Override
    public File getFile() {
      return contentFile();
    }

    @Override
    public InputStream getContent() throws UnsupportedEncodingException {
      String content = data.getContent();

      if (content == null) {
        return new ByteArrayInputStream("".getBytes());
      }

      final String separator = System.getProperty("line.separator");

      if (content.endsWith("|")) {
        content += separator;
      }

      //First replace every windows style to unix
      content = content.replaceAll("\r\n", "\n");
      //Then do the replace to match the OS.  This works around
      //a strange behavior on windows.
      content = content.replaceAll("\n", separator);

      return new ByteArrayInputStream(content.getBytes(FileUtil.CHARENCODING));
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

  class PropertiesFileVersion implements FileVersion {

    final PageData data;

    PropertiesFileVersion(PageData pageData) {
      this.data = new PageData(pageData);
    }

    @Override
    public File getFile() {
      return propertiesFile();
    }

    @Override
    public InputStream getContent() throws IOException {
      WikiPageProperties propertiesToSave = new WikiPageProperties(data.getProperties());
      removeAlwaysChangingProperties(propertiesToSave);
      return new ByteArrayInputStream(propertiesToSave.toXml().getBytes(FileUtil.CHARENCODING));
    }

    @Override
    public String getAuthor() {
      return data.getAttribute(WikiPageProperty.LAST_MODIFYING_USER);
    }

    @Override
    public Date getLastModificationTime() {
      return data.getProperties().getLastModificationTime();
    }

    private void removeAlwaysChangingProperties(WikiPageProperties properties) {
      properties.remove(WikiPageProperty.LAST_MODIFIED);
    }
  }
}
