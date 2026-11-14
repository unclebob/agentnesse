package fitnesse.testsystems.slim;

import fitnesse.testsystems.TestPage;

/** A listener that wants each Slim table after it has been executed. */
public interface TableFinished {
  void tableFinished(TestPage page, MarkdownTable table);
}
