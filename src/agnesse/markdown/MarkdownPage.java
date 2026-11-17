package agnesse.markdown;

import agnesse.wiki.ApplicationVariableSource;
import agnesse.wiki.BaseWikiPage;
import agnesse.wiki.PageData;
import agnesse.wiki.PageType;
import agnesse.wiki.PageVariableSource;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPagePath;
import agnesse.wiki.VersionInfo;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageLoadException;
import agnesse.wiki.WikiPageProperty;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.WikiPageFactory;
import agnesse.wiki.WikiWordReference;
import agnesse.wiki.CompositeVariableSource;
import agnesse.wiki.ParentPageVariableSource;
import agnesse.wiki.fs.FileBasedWikiPage;
import agnesse.wiki.fs.FileSystem;
import agnesse.wiki.fs.FileSystemSubWikiPageFactory;
import agnesse.wiki.fs.FileVersion;
import agnesse.wiki.fs.SubWikiPageFactory;
import agnesse.wiki.fs.WikiFiles;
import agnesse.wiki.VariableSource;

import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One Markdown file. Children are not stored on this page yet.
 */
public class MarkdownPage extends BaseWikiPage implements FileBasedWikiPage {
  public static final String EXTENSION = ".md";
  public static final String ROOT_FILE = "_root.md";
  private static final Pattern VARIABLE = Pattern.compile("(?<!\\\\)\\$\\{([^}]+)}");

  private final File path;
  private final FileSystem fileSystem;
  private final WikiFiles versions;
  private final VariableSource variableSource;
  private final WikiPageFactory pages;
  private PageData pageData;
  private String renderingName;

  public MarkdownPage(File path, String name, WikiPage parent, FileSystem fileSystem, WikiFiles versions,
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
    return addMarkdownChild(name);
  }

  @Override
  public WikiPage addMarkdownChild(String name) {
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
      if (ROOT_FILE.equals(path.getName())) {
        versions.delete(path);
      } else {
        versions.delete(path, getFileSystemPath());
      }
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

  public List<String> paths() {
    return PathLines.from(expand(getData().getContent()));
  }

  @Override
  public String getHtml() {
    return render(getData().getContent());
  }

  public String getHtmlWithin(WikiPage context) {
    String previous = renderingName;
    renderingName = context.getName();
    try {
      return getHtml();
    } finally {
      renderingName = previous;
    }
  }

  /**
   * This page's markdown for Slim. Variables are expanded, and a pipe or a
   * newline inside a value stays inside the cell that names it. Includes are
   * not inlined; each included page is its own document.
   */
  public String expandedForSlim(WikiPage context) {
    String previous = renderingName;
    renderingName = context == null ? null : context.getName();
    try {
      return expandSlim(getData().getContent());
    } finally {
      renderingName = previous;
    }
  }

  private String expandSlim(String content) {
    return substitute(content, true);
  }

  private String render(String content) {
    return MarkdownRenderer.render(expand(TableSeparators.supply(escapeWikiPrefixes(content))), this::wikiWordTarget,
      contentsLinks(), this::includedHtml, WikiPageUtil.rendersSlimTables(this));
  }

  private String includedHtml(String name) {
    WikiPagePath includedPath = PathParser.parse(name.split("\n", 2)[0].trim());
    if (includedPath == null) {
      return "";
    }
    WikiPage included = getPageCrawler().getSiblingPage(includedPath);
    if (included == null || samePage(included)) {
      return "";
    }
    return included.getHtml();
  }

  private boolean samePage(WikiPage included) {
    return PathParser.render(included.getFullPath()).equals(PathParser.render(getFullPath()));
  }

  private List<ContentsLink> contentsLinks() {
    List<ContentsLink> links = new ArrayList<>();
    for (WikiPage child : getChildren()) {
      links.add(new ContentsLink(child.getName(), PathParser.render(child.getFullPath())));
    }
    return links;
  }

  private String expand(String content) {
    return substitute(content, false);
  }

  private String substitute(String content, boolean escapePipes) {
    Matcher matcher = VARIABLE.matcher(content);
    StringBuilder expanded = new StringBuilder();
    while (matcher.find()) {
      String value = getVariable(matcher.group(1));
      if (value != null) {
        if (escapePipes) {
          value = value.replace("|", "\\|");
        }
        value = value.replace("\n", "<br>");
      }
      matcher.appendReplacement(expanded, Matcher.quoteReplacement(value == null ? matcher.group() : value));
    }
    matcher.appendTail(expanded);
    return expanded.toString();
  }

  private static boolean plainLine(boolean fence, String trimmed) {
    return !fence && !trimmed.startsWith("|");
  }

  private static String escapeWikiPrefixes(String content) {
    StringBuilder out = new StringBuilder();
    boolean fence = false;
    for (String line : content.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("```")) {
        fence = !fence;
      }
      if (plainLine(fence, trimmed)) {
        line = line.replaceAll("<(?=[A-Z][a-zA-Z0-9])", "\\\\<");
      }
      out.append(line).append('\n');
    }
    return out.toString();
  }

  private String wikiWordTarget(String wikiWord) {
    String expanded = WikiWordReference.expandPrefix(this, wikiWord);
    WikiPagePath path = PathParser.parse(expanded);
    if (path == null) {
      return null;
    }
    WikiPage page = expanded.startsWith(".")
      ? getPageCrawler().getPage(path)
      : getParent().getPageCrawler().getPage(path);
    if (page == null) {
      return null;
    }
    return PathParser.render(page.getFullPath());
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
    VariableSource source = new CompositeVariableSource(
      this::pageNames,
      new ApplicationVariableSource(variableSource),
      new PageVariableSource(this),
      this::defined,
      new ParentPageVariableSource(this),
      variableSource);
    return source.findVariable(name).orElse(null);
  }

  private Optional<String> pageNames(String name) {
    if ("PAGE_NAME".equals(name)) {
      return Optional.of(renderingName != null ? renderingName : getName());
    }
    if ("PAGE_PATH".equals(name)) {
      return Optional.of(getFullPath().parentPath().toString());
    }
    return Optional.empty();
  }

  private Optional<String> defined(String name) {
    return Optional.ofNullable(definedVariables().get(name));
  }

  public Map<String, String> definedVariables() {
    Map<String, String> values = new LinkedHashMap<>();
    collectDefined(this, values, new HashSet<>());
    return values;
  }

  private static void collectDefined(WikiPage page, Map<String, String> values, Set<WikiPage> seen) {
    if (!(page instanceof MarkdownPage markdown) || !seen.add(page)) {
      return;
    }
    markdown.defines().forEach(values::putIfAbsent);
    for (String name : markdown.includes()) {
      WikiPagePath path = PathParser.parse(name.trim());
      if (path == null) {
        continue;
      }
      WikiPage included = page.getPageCrawler().getSiblingPage(path);
      if (included != null) {
        collectDefined(included, values, seen);
      }
    }
  }

  private List<String> includes() {
    List<String> names = new ArrayList<>();
    String content = getData().getContent();
    collectIncludes(Parser.builder().build().parse(content), names);
    for (String line : content.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("!include ")) {
        names.add(trimmed.substring("!include ".length()).trim());
      }
    }
    return names;
  }

  private void collectIncludes(Node node, List<String> names) {
    if (node instanceof FencedCodeBlock) {
      FencedCodeBlock code = (FencedCodeBlock) node;
      if ("agnesse".equals(code.getInfo())) {
        rememberInclude(names, code.getLiteral());
      }
    }
    Node child = node.getFirstChild();
    while (child != null) {
      collectIncludes(child, names);
      child = child.getNext();
    }
  }

  private void rememberInclude(List<String> names, String literal) {
    try {
      AgNesseBlock block = AgNesseBlock.parse(literal);
      if ("include".equals(block.command())) {
        names.add(block.argument().trim().split("[ \\n]", 2)[0].trim());
      }
    } catch (IllegalArgumentException ignored) {
      // An unknown block is not an include.
    }
  }

  private Map<String, String> defines() {
    Map<String, String> values = new LinkedHashMap<>();
    String content = getData().getContent();
    collectDefines(Parser.builder().build().parse(content), values);
    for (String line : content.split("\n")) {
      rememberBangDefine(values, line.trim());
    }
    expandDefines(values);
    return values;
  }

  private void expandDefines(Map<String, String> values) {
    for (Map.Entry<String, String> entry : new ArrayList<>(values.entrySet())) {
      String expanded = expandDefineValue(entry.getValue(), values);
      entry.setValue(expanded);
      values.put(entry.getKey(), expanded);
    }
  }

  private String expandDefineValue(String value, Map<String, String> values) {
    Matcher matcher = VARIABLE.matcher(value);
    StringBuilder expanded = new StringBuilder();
    while (matcher.find()) {
      String name = matcher.group(1);
      String replacement = values.get(name);
      if (replacement == null && variableSource != null) {
        replacement = variableSource.findVariable(name).orElse(null);
      }
      matcher.appendReplacement(expanded, Matcher.quoteReplacement(replacement == null ? matcher.group() : replacement));
    }
    matcher.appendTail(expanded);
    return expanded.toString();
  }

  private static void rememberBangDefine(Map<String, String> values, String line) {
    if (!line.startsWith("!define ")) {
      return;
    }
    String[] parts = line.substring("!define ".length()).trim().split(" ", 2);
    String value = parts.length < 2 ? "" : unwrapDefined(parts[1].trim());
    values.putIfAbsent(parts[0], value);
  }

  private static String unwrapDefined(String value) {
    if (wrapped(value, '{', '}') || wrapped(value, '(', ')')) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

  private static boolean wrapped(String value, char open, char close) {
    return value.length() >= 2 && value.charAt(0) == open && value.charAt(value.length() - 1) == close;
  }

  private void collectDefines(Node node, Map<String, String> values) {
    if (node instanceof FencedCodeBlock) {
      FencedCodeBlock code = (FencedCodeBlock) node;
      if ("agnesse".equals(code.getInfo())) {
        rememberDefine(values, code.getLiteral());
      }
    }
    Node child = node.getFirstChild();
    while (child != null) {
      collectDefines(child, values);
      child = child.getNext();
    }
  }

  private void rememberDefine(Map<String, String> values, String literal) {
    try {
      AgNesseBlock block = AgNesseBlock.parse(literal);
      if (!"define".equals(block.command())) {
        return;
      }
      String[] parts = block.argument().trim().split("[ \\n]", 2);
      values.put(parts[0].trim(), parts.length < 2 ? "" : parts[1].trim());
    } catch (IllegalArgumentException ignored) {
      // An unknown block does not define a variable.
    }
  }

  private PageData load() {
    String file = readFile();
    FrontMatter matter = FrontMatter.read(file);
    WikiPageProperty properties = propertiesFrom(matter);
    properties.setLastModificationTime(new Date(fileSystem.lastModified(path)));
    PageData data = new PageData(matter.body(), properties);
    data.setFrontMatterPrefix(matter.prefix());
    return data;
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
    if (matter.user() != null) {
      properties.set(WikiPageProperty.LAST_MODIFYING_USER, matter.user());
    }
    return properties;
  }

  private String resolvedType(FrontMatter matter) {
    if (matter.type() != null) {
      return matter.type();
    }
    return PageType.getPageTypeForPageName(getName()).frontMatter();
  }

  private void applyType(WikiPageProperty properties, String type) {
    PageType.applyTo(properties, type);
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
      String body = data.getContent() == null ? "" : data.getContent();
      String text = data.preserveFrontMatter()
        ? data.getFrontMatterPrefix() + body
        : FrontMatter.write(FrontMatter.typeFrom(data), FrontMatter.tagsFrom(data), data.getAttribute(WikiPageProperty.HELP),
          data.hasAttribute(WikiPageProperty.PRUNE), data.getAttribute(WikiPageProperty.LAST_MODIFYING_USER),
          data.getContent());
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
