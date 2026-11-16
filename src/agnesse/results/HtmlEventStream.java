package agnesse.results;

import java.io.IOException;

/** Turns each NDJSON line into a script the browser runs as the page streams. */
public final class HtmlEventStream implements Appendable {
  private final Appendable out;
  private final StringBuilder pending = new StringBuilder();

  public HtmlEventStream(Appendable out) {
    this.out = out;
  }

  @Override
  public Appendable append(CharSequence csq) throws IOException {
    return append(csq, 0, csq.length());
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end) throws IOException {
    for (int i = start; i < end; i++) {
      append(csq.charAt(i));
    }
    return this;
  }

  @Override
  public Appendable append(char c) throws IOException {
    if (c == '\n') {
      write(pending.toString());
      pending.setLength(0);
    } else if (c != '\r') {
      pending.append(c);
    }
    return this;
  }

  private void write(String line) throws IOException {
    if (line.isBlank()) {
      return;
    }
    out.append("<script>AgNesseResults.apply(");
    out.append(line.replace("<", "\\u003c"));
    out.append(");</script>\n");
  }
}
