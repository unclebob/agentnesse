package agnesse.testsystems.slim;

import agnesse.testsystems.TestPage;

/** A listener that wants each Slim table after it has been executed. */
public interface TableFinished {
  void tableFinished(TestPage page, MarkdownTable table);
}
