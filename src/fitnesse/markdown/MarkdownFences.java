package fitnesse.markdown;

/**
 * Tracks a CommonMark fence. A fence closes on a line of backticks at least as
 * long as the opener, with nothing after the backticks. A line such as
 * {@code ```agentnesse} inside a fence is content.
 */
public final class MarkdownFences {
  private int opener;

  public boolean inside() {
    return opener > 0;
  }

  public boolean boundary(String line) {
    String trimmed = line.trim();
    int ticks = 0;
    while (ticks < trimmed.length() && trimmed.charAt(ticks) == '`') {
      ticks++;
    }
    if (ticks < 3) {
      return false;
    }
    boolean onlyTicks = trimmed.substring(ticks).isBlank();
    if (opener == 0) {
      opener = ticks;
      return true;
    }
    if (onlyTicks && ticks >= opener) {
      opener = 0;
      return true;
    }
    return false;
  }
}
