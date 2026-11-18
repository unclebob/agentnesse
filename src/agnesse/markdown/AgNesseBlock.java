package agnesse.markdown;

import agnesse.html.HtmlTag;
import agnesse.html.HtmlText;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A fenced {@code agnesse} block. The first word is the command. Slim
 * tables are not blocks; they are bare Markdown tables.
 */
public final class AgNesseBlock {
  public static final Set<String> COMMANDS = Set.of(
    "define", "include", "scenarios", "path", "contents", "alias");

  private final String command;
  private final String argument;

  private AgNesseBlock(String command, String argument) {
    this.command = command;
    this.argument = argument;
  }

  public static AgNesseBlock parse(String body) {
    String text = body == null ? "" : body.replaceFirst("\\n\\z", "");
    int newline = text.indexOf('\n');
    String first;
    String rest;
    if (newline == -1) {
      first = text.trim();
      rest = "";
    } else {
      first = text.substring(0, newline).trim();
      rest = text.substring(newline + 1);
    }
    if (first.isEmpty()) {
      throw new IllegalArgumentException("agnesse block needs a command");
    }
    String[] words = first.split(" ", 2);
    String command = words[0].toLowerCase(Locale.ROOT);
    String inline = words.length == 2 ? words[1].trim() : "";
    if (!COMMANDS.contains(command)) {
      throw new IllegalArgumentException("unknown agnesse command \"" + command + "\"");
    }
    return new AgNesseBlock(command, join(inline, rest));
  }

  public String command() {
    return command;
  }

  public String argument() {
    return argument;
  }

  public String html() {
    if ("include".equals(command)) {
      return includeHtml();
    }
    if ("contents".equals(command)) {
      return contentsHtml(List.of());
    }
    HtmlTag div = new HtmlTag("div");
    div.addAttribute("class", "agnesse-" + command);
    div.add(new HtmlText(argument));
    return div.htmlInline() + "\n";
  }

  public static String contentsHtml(List<ContentsLink> links) {
    HtmlTag list = new HtmlTag("ul");
    for (ContentsLink link : links) {
      HtmlTag anchor = new HtmlTag("a", link.name());
      anchor.addAttribute("href", link.href());
      HtmlTag item = new HtmlTag("li");
      item.add(anchor);
      list.add(item);
    }
    HtmlTag div = new HtmlTag("div");
    div.addAttribute("class", "agnesse-contents");
    div.add(list);
    return div.htmlInline() + "\n";
  }

  public String includeWith(String body, String pagePath) {
    return agnesse.html.CollapsibleSection.html(includeName(), pagePath, body, false, true);
  }

  static String firstLine(String argument) {
    String text = argument == null ? "" : argument;
    int newline = text.indexOf('\n');
    return (newline < 0 ? text : text.substring(0, newline)).trim();
  }

  private String includeName() {
    String text = firstLine(argument);
    if (text.startsWith(">")) {
      return text.substring(1).trim();
    }
    return text;
  }

  private String includeHtml() {
    int newline = argument.indexOf('\n');
    String more = newline == -1 ? "" : argument.substring(newline + 1);
    return agnesse.html.CollapsibleSection.html(
      includeName(), null, agnesse.html.HtmlUtil.escapeHTML(more), false, true);
  }

  private static String join(String inline, String rest) {
    if (inline.isEmpty()) {
      return rest;
    }
    if (rest.isEmpty()) {
      return inline;
    }
    return inline + "\n" + rest;
  }
}
