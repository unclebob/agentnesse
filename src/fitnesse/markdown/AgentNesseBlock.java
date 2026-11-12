package fitnesse.markdown;

import fitnesse.html.HtmlTag;
import fitnesse.html.HtmlText;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A fenced {@code agentnesse} block. The first word is the command. Slim
 * tables are not blocks; they are bare Markdown tables.
 */
public final class AgentNesseBlock {
  public static final Set<String> COMMANDS = Set.of(
    "define", "include", "scenarios", "path", "contents", "alias");

  private final String command;
  private final String argument;

  private AgentNesseBlock(String command, String argument) {
    this.command = command;
    this.argument = argument;
  }

  public static AgentNesseBlock parse(String body) {
    String text = body == null ? "" : body.replaceFirst("\\n\\z", "");
    int newline = text.indexOf('\n');
    String first = (newline < 0 ? text : text.substring(0, newline)).trim();
    String rest = newline < 0 ? "" : text.substring(newline + 1);
    if (first.isEmpty()) {
      throw new IllegalArgumentException("agentnesse block needs a command");
    }
    int space = first.indexOf(' ');
    String command = (space < 0 ? first : first.substring(0, space)).toLowerCase(Locale.ROOT);
    String inline = space < 0 ? "" : first.substring(space + 1).trim();
    if (!COMMANDS.contains(command)) {
      throw new IllegalArgumentException("unknown agentnesse command \"" + command + "\"");
    }
    return new AgentNesseBlock(command, join(inline, rest));
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
    div.addAttribute("class", "agentnesse-" + command);
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
    div.addAttribute("class", "agentnesse-contents");
    div.add(list);
    return div.htmlInline() + "\n";
  }

  private String includeHtml() {
    String name = argument;
    String more = "";
    int newline = argument.indexOf('\n');
    if (newline >= 0) {
      name = argument.substring(0, newline);
      more = argument.substring(newline + 1);
    }
    HtmlTag summary = new HtmlTag("summary");
    summary.add(new HtmlText(name));
    HtmlTag details = new HtmlTag("details");
    details.addAttribute("class", "agentnesse-include");
    details.add(summary);
    if (!more.isEmpty()) {
      details.add(new HtmlText(more));
    }
    return details.htmlInline() + "\n";
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
