package fitnesse.markdown.convert;

import fitnesse.markdown.FrontMatter;
import fitnesse.wiki.PageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WikiToMarkdown {
  private static final Set<String> INHERITED = Set.of(
    "SetUp", "TearDown", "SuiteSetUp", "SuiteTearDown", "PageHeader", "PageFooter");
  private static final String[] UNSUPPORTED = {
    "!today", "!headings", "${=", "!{", "!style", "!note", "!anchor", "!lastmodified", "!meta", "!line", "!see"
  };
  private static final Pattern HELP = Pattern.compile("<Help>(.*?)</Help>", Pattern.DOTALL);
  private static final Pattern SUITES = Pattern.compile("<Suites>(.*?)</Suites>", Pattern.DOTALL);

  private WikiToMarkdown() {
  }

  public static String convert(String pageName, String wiki, String propertiesXml, List<String> notes) {
    String body = new Body(notes).convert(wiki == null ? "" : wiki);
    return FrontMatter.write(type(pageName, propertiesXml), tags(propertiesXml), help(propertiesXml),
      propertiesXml != null && propertiesXml.contains("<Prune"), body);
  }

  private static String type(String pageName, String xml) {
    if (xml != null && xml.contains("<Suite")) {
      return "suite";
    }
    if (xml != null && xml.contains("<Test")) {
      return "test";
    }
    return PageType.getPageTypeForPageName(pageName).name().toLowerCase();
  }

  private static String help(String xml) {
    return text(xml, HELP);
  }

  private static List<String> tags(String xml) {
    String suites = text(xml, SUITES);
    if (suites == null) {
      return List.of();
    }
    List<String> tags = new ArrayList<>();
    for (String tag : suites.split(",")) {
      if (!tag.isBlank()) {
        tags.add(tag.trim());
      }
    }
    return tags;
  }

  private static String text(String xml, Pattern pattern) {
    if (xml == null) {
      return null;
    }
    Matcher matcher = pattern.matcher(xml);
    return matcher.find() ? matcher.group(1).trim() : null;
  }

  private static final class Body {
    final List<String> notes;
    String[] lines = new String[0];
    int index;
    int end;

    private Body(List<String> notes) {
      this.notes = notes;
    }

    private String convert(String wiki) {
      if (wiki.isEmpty()) {
        return "";
      }
      lines = wiki.split("\n", -1);
      end = lines.length;
      if (wiki.endsWith("\n") && lines[end - 1].isEmpty()) {
        end--;
      }
      StringBuilder out = new StringBuilder();
      while (index < end) {
        String piece = next();
        if (piece == null) {
          continue;
        }
        if (out.length() > 0) {
          out.append('\n');
        }
        out.append(piece);
      }
      if (!out.isEmpty()) {
        out.append('\n');
      }
      return out.toString();
    }

    private static final String UNHANDLED = "\0";

    private String next() {
      String line = lines[index];
      if (line.trim().isEmpty()) {
        index++;
        return "";
      }
      String command = command(line);
      return command == UNHANDLED ? plain(line) : command;
    }

    private String command(String line) {
      if (unsupported(line)) {
        index++;
        return line;
      }
      if (Collapsible.starts(line)) {
        return new Collapsible(this).convert();
      }
      if (line.trim().startsWith("!define")) {
        return define();
      }
      if (line.trim().startsWith("!include")) {
        return include();
      }
      if (line.trim().startsWith("!path")) {
        return command("path", "!path");
      }
      if (line.trim().startsWith("!contents")) {
        return contents();
      }
      return UNHANDLED;
    }

    private String plain(String line) {
      if (heading(line) > 0) {
        return headingLine(line);
      }
      if (line.trim().startsWith("#")) {
        index++;
        return "<!-- " + line.trim().substring(1).trim() + " -->";
      }
      if (line.trim().startsWith("!img")) {
        index++;
        return image(line.trim());
      }
      if (line.trim().matches("----+")) {
        index++;
        return "---";
      }
      if (tableLine(line)) {
        return table();
      }
      index++;
      return Inline.of(list(line), notes);
    }

    private String define() {
      String block = takeUntilClosed();
      return Define.format(block, notes);
    }

    private String takeUntilClosed() {
      StringBuilder block = new StringBuilder(lines[index]);
      while (!Define.closed(block.toString()) && index + 1 < end) {
        index++;
        block.append('\n').append(lines[index]);
      }
      index++;
      return block.toString();
    }

    private String include() {
      String rest = lines[index++].trim().substring("!include".length()).trim();
      rest = options(rest);
      if (rest.startsWith(">") || rest.startsWith("<")) {
        rest = rest.substring(1).trim();
      }
      if (rest.startsWith(".")) {
        rest = rest.substring(1);
      }
      String name = rest.split("\\s+")[0];
      String last = name.substring(name.lastIndexOf('.') + 1);
      if (INHERITED.contains(last)) {
        return null;
      }
      return fence("include " + name);
    }

    private String options(String rest) {
      while (rest.startsWith("-")) {
        String option = rest.split("\\s+")[0];
        if ("-seamless".equals(option)) {
          notes.add("include -seamless");
        }
        rest = rest.substring(option.length()).trim();
      }
      return rest;
    }

    private String command(String name, String keyword) {
      String rest = lines[index++].trim().substring(keyword.length()).trim();
      return fence(name + "\n" + rest);
    }

    private String contents() {
      String rest = lines[index++].trim().substring("!contents".length()).trim();
      if (!rest.isEmpty()) {
        notes.add("contents options not converted: " + rest);
      }
      return fence("contents");
    }

    private String headingLine(String line) {
      int level = heading(line);
      String title = line.trim().substring(2).trim().replace("!help", "").trim();
      index++;
      return "#".repeat(level) + " " + Inline.of(title, notes);
    }

    private String image(String line) {
      String url = line.replaceFirst("^!img(?:-[lr])?\\s+", "");
      return "![](" + url + ")";
    }

    private String table() {
      List<String> raw = new ArrayList<>();
      int open = 0;
      while (index < end && (open > 0 || raw.isEmpty() || tableLine(lines[index]))) {
        String line = lines[index++];
        raw.add(line);
        open += literals(line);
      }
      return Table.toMarkdown(String.join("\n", raw), notes);
    }

    private boolean unsupported(String line) {
      boolean found = false;
      for (String token : UNSUPPORTED) {
        if (line.contains(token)) {
          notes.add(token);
          found = true;
        }
      }
      return found;
    }

    private static int heading(String line) {
      String trimmed = line.trim();
      if (trimmed.length() < 2 || trimmed.charAt(0) != '!') {
        return 0;
      }
      char level = trimmed.charAt(1);
      if (level < '1' || level > '6') {
        return 0;
      }
      return level - '0';
    }

    private static boolean tableLine(String line) {
      String trimmed = line.trim();
      if (trimmed.startsWith("-!")) {
        trimmed = trimmed.substring(2).trim();
      }
      if (trimmed.startsWith("!")) {
        trimmed = trimmed.substring(1).trim();
      }
      return trimmed.startsWith("|");
    }

    private static int literals(String line) {
      int open = 0;
      for (int i = 0; i + 1 < line.length(); i++) {
        if (line.charAt(i) == '!' && line.charAt(i + 1) == '-') {
          open++;
          i++;
        } else if (line.charAt(i) == '-' && line.charAt(i + 1) == '!') {
          open--;
          i++;
        }
      }
      return open;
    }

    private static String list(String line) {
      return line.replaceFirst("^(\\s*)\\* ", "$1- ");
    }

    private static String fence(String body) {
      return "```agentnesse\n" + body + "\n```";
    }
  }

  private static final class Define {
    private Define() {
    }

    private static boolean closed(String block) {
      int nameEnd = nameEnd(block);
      if (nameEnd < 0) {
        return true;
      }
      String rest = block.substring(nameEnd).trim();
      if (rest.isEmpty() || opener(rest) == 0) {
        return !block.endsWith("\n") || opener(rest) == 0;
      }
      return depth(rest) == 0;
    }

    private static String format(String block, List<String> notes) {
      int nameEnd = nameEnd(block);
      String name = block.substring("!define".length(), nameEnd).trim();
      String rest = block.substring(nameEnd).trim();
      String value = value(rest, notes);
      String body = value.isEmpty() ? "define " + name : "define " + name + "\n" + value;
      return "```agentnesse\n" + body + "\n```";
    }

    private static int nameEnd(String block) {
      int start = block.indexOf("!define");
      if (start < 0) {
        return -1;
      }
      int name = start + "!define".length();
      while (name < block.length() && Character.isWhitespace(block.charAt(name))) {
        name++;
      }
      while (name < block.length() && !Character.isWhitespace(block.charAt(name))) {
        name++;
      }
      return name;
    }

    private static String value(String rest, List<String> notes) {
      if (rest.isEmpty()) {
        return "";
      }
      if (opener(rest) != 0) {
        return innerValue(inside(rest), notes);
      }
      if (rest.matches("[A-Za-z][A-Za-z0-9]*")) {
        return "${" + rest + "}";
      }
      return rest;
    }

    private static String innerValue(String inner, List<String> notes) {
      String trimmed = inner.trim();
      if (trimmed.startsWith("!-") && trimmed.endsWith("-!")) {
        return stripEdges(trimmed.substring(2, trimmed.length() - 2));
      }
      if (looksLikeWiki(trimmed)) {
        return new Body(notes).convert(trimmed).strip();
      }
      return trimmed;
    }

    private static String stripEdges(String value) {
      if (value.startsWith("\n")) {
        value = value.substring(1);
      }
      if (value.endsWith("\n")) {
        value = value.substring(0, value.length() - 1);
      }
      return value;
    }

    private static char opener(String rest) {
      char first = rest.charAt(0);
      if (first == '{' || first == '(' || first == '[') {
        return first;
      }
      return 0;
    }

    private static String inside(String rest) {
      Frame frame = new Frame(opener(rest));
      StringBuilder inner = new StringBuilder();
      for (int i = 0; i < rest.length() && !frame.finished(); i++) {
        if (frame.keep(rest, i)) {
          inner.append(rest.charAt(i));
        }
      }
      return inner.toString();
    }

    private static int depth(String rest) {
      Frame frame = new Frame(opener(rest));
      for (int i = 0; i < rest.length(); i++) {
        frame.keep(rest, i);
      }
      return frame.depth();
    }

    private static final class Frame {
      private final char open;
      private final char close;
      private int depth;
      private boolean literal;
      private boolean finished;

      private Frame(char open) {
        this.open = open;
        this.close = close(open);
      }

      private boolean keep(String rest, int i) {
        literal = mark(rest, i, literal);
        if (literal || finished) {
          return !finished;
        }
        if (rest.charAt(i) == open) {
          depth++;
          return depth > 1;
        }
        if (rest.charAt(i) == close) {
          depth--;
          finished = depth == 0;
          return !finished;
        }
        return true;
      }

      private boolean finished() {
        return finished;
      }

      private int depth() {
        return depth;
      }
    }

    private static boolean mark(String rest, int i, boolean literal) {
      if (!literal && rest.startsWith("!-", i)) {
        return true;
      }
      if (literal && rest.startsWith("-!", i)) {
        return false;
      }
      return literal;
    }

    private static char close(char open) {
      if (open == '{') {
        return '}';
      }
      if (open == '(') {
        return ')';
      }
      return ']';
    }
  }

  private static boolean looksLikeWiki(String text) {
    return text.contains("\n") || text.startsWith("|") || text.startsWith("!");
  }

  private static final class Collapsible {
    private final Body body;

    private Collapsible(Body body) {
      this.body = body;
    }

    private static boolean starts(String line) {
      return line.trim().matches("!\\*+[<>]?\\s*.*");
    }

    private String convert() {
      String line = body.lines[body.index++].trim();
      boolean collapsed = line.startsWith("!**>") || line.startsWith("!**<");
      if (line.startsWith("!**<")) {
        body.notes.add("invisible collapsible");
      }
      String title = line.replaceFirst("^!\\*+[<>]?\\s*", "");
      StringBuilder inner = new StringBuilder();
      while (body.index < body.end && !body.lines[body.index].trim().matches("\\*+!")) {
        inner.append(body.lines[body.index++]).append('\n');
      }
      if (body.index < body.end) {
        body.index++;
      }
      String converted = new Body(body.notes).convert(inner.toString()).strip();
      String open = collapsed ? "" : "open ";
      return "```agentnesse\ncollapsible " + open + title + "\n" + converted + "\n```";
    }
  }

  private static final class Table {
    private Table() {
    }

    private static String toMarkdown(String raw, List<String> notes) {
      List<List<String>> rows = rows(raw, notes);
      int width = 0;
      for (List<String> row : rows) {
        width = Math.max(width, row.size());
      }
      StringBuilder out = new StringBuilder();
      out.append(render(pad(rows.get(0), width))).append('\n');
      out.append(dashes(width)).append('\n');
      for (int i = 1; i < rows.size(); i++) {
        out.append(render(pad(rows.get(i), width))).append('\n');
      }
      return "\n" + out.substring(0, out.length() - 1);
    }

    private static List<List<String>> rows(String raw, List<String> notes) {
      List<List<String>> rows = new ArrayList<>();
      for (String row : splitRows(raw)) {
        if (!row.isBlank()) {
          rows.add(cells(row, notes));
        }
      }
      return rows;
    }

    private static List<String> splitRows(String raw) {
      List<String> rows = new ArrayList<>();
      StringBuilder row = new StringBuilder();
      boolean literal = false;
      for (int i = 0; i < raw.length(); i++) {
        if (!literal && raw.startsWith("!-", i)) {
          literal = true;
        } else if (literal && raw.startsWith("-!", i)) {
          literal = false;
        }
        if (!literal && raw.charAt(i) == '\n') {
          rows.add(row.toString());
          row.setLength(0);
          continue;
        }
        row.append(raw.charAt(i));
      }
      rows.add(row.toString());
      return rows;
    }

    private static List<String> cells(String row, List<String> notes) {
      String line = withoutBang(row.trim());
      List<String> cells = new ArrayList<>();
      StringBuilder cell = new StringBuilder();
      boolean literal = false;
      boolean started = false;
      for (int i = 0; i < line.length(); i++) {
        literal = literal(line, i, literal);
        if (!literal && line.charAt(i) == '|') {
          started = cell(cells, cell, started, notes);
          continue;
        }
        cell.append(line.charAt(i));
      }
      return cells;
    }

    private static boolean literal(String line, int i, boolean literal) {
      if (!literal && line.startsWith("!-", i)) {
        return true;
      }
      if (literal && line.startsWith("-!", i)) {
        return false;
      }
      return literal;
    }

    private static String withoutBang(String line) {
      if (line.startsWith("-!")) {
        line = line.substring(2).trim();
      }
      return line.startsWith("!") ? line.substring(1).trim() : line;
    }

    private static boolean cell(List<String> cells, StringBuilder cell, boolean started, List<String> notes) {
      if (started) {
        cells.add(renderCell(cell.toString(), notes));
        cell.setLength(0);
      }
      return true;
    }

    private static String renderCell(String raw, List<String> notes) {
      String trimmed = raw.trim();
      String literal = wholeLiteral(trimmed);
      String text = literal == null ? Inline.of(trimmed, notes) : literalText(literal, notes);
      return escape(text);
    }

    private static String wholeLiteral(String text) {
      if (text.startsWith("!-") && text.endsWith("-!") && text.indexOf("-!") == text.length() - 2) {
        return text.substring(2, text.length() - 2);
      }
      return null;
    }

    private static String literalText(String inner, List<String> notes) {
      if (!looksLikeWiki(inner)) {
        return inner;
      }
      return new Body(notes).convert(inner).strip();
    }

    private static String escape(String text) {
      StringBuilder out = new StringBuilder();
      String source = text.replace("\n", "{{n}}");
      for (int i = 0; i < source.length(); i++) {
        char current = source.charAt(i);
        if (current == '\\' || current == '|' || current == '*' || current == '_' || current == '`') {
          out.append('\\');
        }
        out.append(current);
      }
      return out.toString();
    }

    private static List<String> pad(List<String> row, int width) {
      List<String> padded = new ArrayList<>(row);
      while (padded.size() < width) {
        padded.add("{{pad}}");
      }
      return padded;
    }

    private static String render(List<String> cells) {
      StringBuilder out = new StringBuilder("|");
      for (String cell : cells) {
        out.append(cell.isEmpty() ? " |" : " " + cell + " |");
      }
      return out.toString();
    }

    private static String dashes(int width) {
      StringBuilder out = new StringBuilder("|");
      for (int i = 0; i < width; i++) {
        out.append(" --- |");
      }
      return out.toString();
    }
  }

  private static final class Inline {
    private Inline() {
    }

    private static String of(String text, List<String> notes) {
      for (String token : UNSUPPORTED) {
        if (text.contains(token)) {
          notes.add(token);
          return text;
        }
      }
      StringBuilder out = new StringBuilder();
      int i = 0;
      while (i < text.length()) {
        int next = consume(text, i, out);
        if (next == i) {
          out.append(text.charAt(i));
          i++;
        } else {
          i = next;
        }
      }
      return alias(out.toString());
    }

    private static int consume(String text, int i, StringBuilder out) {
      if (text.startsWith("!-", i)) {
        return literal(text, i, out);
      }
      if (text.startsWith("[[", i)) {
        return link(text, i, out);
      }
      if (text.startsWith("'''", i)) {
        return wrapped(text, i, "'''", "**", out);
      }
      if (text.startsWith("''", i)) {
        return wrapped(text, i, "''", "*", out);
      }
      return i;
    }

    private static int literal(String text, int i, StringBuilder out) {
      int end = text.indexOf("-!", i + 2);
      if (end < 0) {
        return i;
      }
      out.append('`').append(text, i + 2, end).append('`');
      return end + 2;
    }

    private static int link(String text, int i, StringBuilder out) {
      int middle = text.indexOf("][", i + 2);
      int end = middle < 0 ? -1 : text.indexOf("]]", middle + 2);
      if (middle < 0 || end < 0) {
        return i;
      }
      String target = text.substring(middle + 2, end);
      if (target.startsWith(".")) {
        target = target.substring(1);
      }
      out.append('[').append(text, i + 2, middle).append("](").append(target).append(')');
      return end + 2;
    }

    private static int wrapped(String text, int i, String marker, String markdown, StringBuilder out) {
      int end = text.indexOf(marker, i + marker.length());
      if (end < 0) {
        return i;
      }
      out.append(markdown).append(of(text.substring(i + marker.length(), end), new ArrayList<>())).append(markdown);
      return end + marker.length();
    }

    private static String alias(String text) {
      return text.replaceAll("(^|\\s)>([A-Z][a-z0-9]+(?:[A-Z][a-z0-9]+)+)", "$1$2");
    }
  }
}
