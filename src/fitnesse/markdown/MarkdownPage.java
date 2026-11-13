package fitnesse.markdown;

import fitnesse.wiki.ApplicationVariableSource;
import fitnesse.wiki.BaseWikiPage;
import fitnesse.wiki.PageData;
import fitnesse.wiki.PageType;
import fitnesse.wiki.PageVariableSource;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPagePath;
import fitnesse.wiki.VersionInfo;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageLoadException;
import fitnesse.wiki.WikiPageProperty;
import fitnesse.wiki.WikiPageFactory;
import fitnesse.wiki.WikiWordReference;
import fitnesse.wiki.CompositeVariableSource;
import fitnesse.wiki.ParentPageVariableSource;
import fitnesse.wiki.fs.FileBasedWikiPage;
import fitnesse.wiki.fs.FileSystem;
import fitnesse.wiki.fs.FileSystemSubWikiPageFactory;
import fitnesse.wiki.fs.FileVersion;
import fitnesse.wiki.fs.SubWikiPageFactory;
import fitnesse.wiki.fs.WikiFiles;
import fitnesse.wiki.VariableSource;

import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

  private String render(String content) {
    return MarkdownRenderer.render(expand(TableSeparators.supply(escapeWikiPrefixes(content))), this::wikiWordTarget, contentsLinks(), this::includedHtml);
  }

  private String includedHtml(String name) {
    String pageName = name;
    int breakAt = pageName.indexOf('\n');
    if (breakAt >= 0) {
      pageName = pageName.substring(0, breakAt);
    }
    WikiPagePath includedPath = PathParser.parse(pageName.trim());
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
    Matcher matcher = VARIABLE.matcher(content);
    StringBuilder expanded = new StringBuilder();
    while (matcher.find()) {
      String value = getVariable(matcher.group(1));
      if (value != null) {
        value = value.replace("\n", "<br>");
      }
      matcher.appendReplacement(expanded, Matcher.quoteReplacement(value == null ? matcher.group() : value));
    }
    matcher.appendTail(expanded);
    return expanded.toString();
  }

  private static String escapeWikiPrefixes(String content) {
    StringBuilder out = new StringBuilder();
    boolean fence = false;
    for (String line : content.split("\n", -1)) {
      String trimmed = line.trim();
      if (trimmed.startsWith("```")) {
        fence = !fence;
      }
      if (!fence && !trimmed.startsWith("|")) {
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
    for (String line : content.split("\n", -1)) {
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
      if ("agentnesse".equals(code.getInfo())) {
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
      AgentNesseBlock block = AgentNesseBlock.parse(literal);
      if ("include".equals(block.command())) {
        String name = block.argument().trim();
        int split = firstBreak(name);
        names.add(split < 0 ? name : name.substring(0, split).trim());
      }
    } catch (IllegalArgumentException ignored) {
      // An unknown block is not an include.
    }
  }

  private Map<String, String> defines() {
    Map<String, String> values = new LinkedHashMap<>();
    String content = getData().getContent();
    collectDefines(Parser.builder().build().parse(content), values);
    for (String line : content.split("\n", -1)) {
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
    String rest = line.substring("!define ".length()).trim();
    int space = rest.indexOf(' ');
    if (space < 0) {
      values.putIfAbsent(rest, "");
      return;
    }
    String name = rest.substring(0, space);
    String value = rest.substring(space + 1).trim();
    if (value.length() >= 2 && ((value.startsWith("{") && value.endsWith("}")) || (value.startsWith("(") && value.endsWith(")")))) {
      value = value.substring(1, value.length() - 1);
    }
    values.putIfAbsent(name, value);
  }

  private void collectDefines(Node node, Map<String, String> values) {
    if (node instanceof FencedCodeBlock) {
      FencedCodeBlock code = (FencedCodeBlock) node;
      if ("agentnesse".equals(code.getInfo())) {
        rememberDefine(values, code.getLiteral());
      }
    }
    Node child = node.getFirstChild();
    while (child != null) {
      collectDefines(child, values);
      child = child.getNext();
    }
  }

  private static int firstBreak(String argument) {
    int space = argument.indexOf(' ');
    int newline = argument.indexOf('\n');
    if (space < 0) {
      return newline;
    }
    if (newline < 0) {
      return space;
    }
    return Math.min(space, newline);
  }

  private void rememberDefine(Map<String, String> values, String literal) {
    try {
      AgentNesseBlock block = AgentNesseBlock.parse(literal);
      if (!"define".equals(block.command())) {
        return;
      }
      String argument = block.argument().trim();
      int split = firstBreak(argument);
      if (split < 0) {
        values.put(argument, "");
      } else {
        values.put(argument.substring(0, split).trim(), argument.substring(split + 1).trim());
      }
    } catch (IllegalArgumentException ignored) {
      // An unknown block does not define a variable.
    }
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
    if (matter.user() != null) {
      properties.set(WikiPageProperty.LAST_MODIFYING_USER, matter.user());
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
