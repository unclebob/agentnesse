package fitnesse.markdown.convert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WikiToMarkdownTest {

  @Test
  public void aTableBecomesAGfmTablePaddedToTheWidestRow() {
    String markdown = convert("Suite", """
      | script | echo fixture |
      | check | echo | Hello | Hello |
      """, "");
    assertTrue(markdown.contains("""
      | script | echo fixture | {{pad}} | {{pad}} |
      | --- | --- | --- | --- |
      | check | echo | Hello | Hello |
      """));
  }

  @Test
  public void aLiteralTableMarkerIsStillATable() {
    String markdown = convert("SetUp", """
      !|Import|
      |fitnesse.fixtures|
      """, "<Test/>");
    assertTrue(markdown.contains("""
      | Import |
      | --- |
      | fitnesse.fixtures |
      """));
    assertTrue(markdown.contains("\"type\": \"test\""));
  }

  @Test
  public void aPipeInsideACellIsEscaped() {
    String markdown = convert("P", "| decision:X |\n| !-a|b-! |\n", "");
    assertTrue(markdown.contains("| a\\|b |"));
  }

  @Test
  public void aDefineBecomesABlockAndALiteralValueStaysRaw() {
    String markdown = convert("Root", """
      !define SLIM_PORT {1}
      !define SUT_PATH (!-
      !path lib/*.jar
      -!)
      """, "");
    assertTrue(markdown.contains("""
      ```agentnesse
      define SLIM_PORT
      1
      ```
      """));
    assertTrue(markdown.contains("""
      ```agentnesse
      define SUT_PATH
      !path lib/*.jar
      ```
      """));
  }

  @Test
  public void setupIsOmittedAndAnotherIncludeStays() {
    String markdown = convert("Page", """
      !include -c .FitNesse.SuiteAcceptanceTests.SetUp
      !include >IncludedPage
      """, "");
    assertTrue(!markdown.contains("include FitNesse"));
    assertTrue(markdown.contains("""
      ```agentnesse
      include IncludedPage
      ```
      """));
  }

  @Test
  public void pathAndContentsBecomeBlocks() {
    List<String> notes = new ArrayList<>();
    String markdown = WikiToMarkdown.convert("Page", """
      !path lib/*.jar
      !contents -R2 -g
      """, "", notes);
    assertTrue(markdown.contains("""
      ```agentnesse
      path
      lib/*.jar
      ```
      """));
    assertTrue(markdown.contains("""
      ```agentnesse
      contents
      ```
      """));
    assertTrue(notes.stream().anyMatch(note -> note.contains("-R2")));
  }

  @Test
  public void headingsLiteralsCommentsLinksAndCollapsiblesConvert() {
    String markdown = convert("Page", """
      !2 Title
      A !-word-! here
      # a comment
      [[text][http://example.com]]
      !**> Closed
      hidden
      *!
      """, "");
    assertTrue(markdown.contains("## Title"));
    assertTrue(markdown.contains("A `word` here"));
    assertTrue(markdown.contains("<!-- a comment -->"));
    assertTrue(markdown.contains("[text](http://example.com)"));
    assertTrue(markdown.contains("""
      ```agentnesse
      collapsible Closed
      hidden
      ```
      """));
  }

  @Test
  public void propertiesBecomeFrontMatter() {
    String markdown = convert("Anything", "Hello\n", """
      <properties>
        <Suite/>
        <Help>Hello</Help>
        <Suites>smoke, slow</Suites>
        <Prune/>
      </properties>
      """);
    assertTrue(markdown.startsWith("""
      {
        "type": "suite",
        "tags": ["smoke","slow"],
        "help": "Hello",
        "prune": true
      }
      """));
  }

  @Test
  public void anUnsupportedConstructIsReportedAndLeftUntouched() {
    List<String> notes = new ArrayList<>();
    String markdown = WikiToMarkdown.convert("Page", "Today is !today.\n", "", notes);
    assertTrue(markdown.contains("Today is !today."));
    assertTrue(notes.stream().anyMatch(note -> note.contains("!today")));
  }

  @Test
  public void aPageWrittenInsideACellBecomesMarkdown() {
    String markdown = convert("Stop", """
      | script |
      | given page | MyPage | with content | !-
      | import |
      | fitnesse.slim.test |
      -! |
      """, "");
    assertTrue(markdown.contains("{{n}}"));
    assertTrue(markdown.contains("\\| import \\|") || markdown.contains("import"));
  }

  @Test
  public void aLiteralCloseBeforeATableIsStillATable() {
    String markdown = convert("Page", """
      !|Table:ReturnsNull|
      ||

      -!|script|
      |pass|
      """, "");
    assertTrue(markdown.contains("| script |\n| --- |\n| pass |"));
  }

  @Test
  public void aTableAfterASentenceStartsOnItsOwnLine() {
    String markdown = convert("Page", """
      Get the fixture
      |Library|
      |fitnesse.slim.test.Division|
      """, "");
    assertTrue(markdown.contains("\n\n| Library |\n| --- |\n| fitnesse.slim.test.Division |\n"));
  }

  private static String convert(String name, String wiki, String properties) {
    return WikiToMarkdown.convert(name, wiki, properties, new ArrayList<>());
  }
}
