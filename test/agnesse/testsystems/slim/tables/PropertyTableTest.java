package agnesse.testsystems.slim.tables;

import agnesse.results.BlockJson;
import agnesse.testrunner.WikiPageDescriptor;
import agnesse.testrunner.WikiTestPage;
import agnesse.testsystems.TestPage;
import agnesse.testsystems.TestSummary;
import agnesse.testsystems.TestSystemListener;
import agnesse.testsystems.slim.HtmlSlimTestSystem;
import agnesse.testsystems.slim.InProcessSlimClientBuilder;
import agnesse.testsystems.slim.MarkdownTable;
import agnesse.testsystems.slim.MarkdownTableScanner;
import agnesse.testsystems.slim.SlimClient;
import agnesse.testsystems.slim.SlimCommunicationException;
import agnesse.testsystems.slim.SlimVersionMismatch;
import agnesse.testsystems.slim.SlimTestContextImpl;
import agnesse.testsystems.slim.TableFinished;
import agnesse.testsystems.slim.property.CellLanguage;
import agnesse.testsystems.slim.property.Seed;
import agnesse.testsystems.slim.results.CellRecord;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.InMemoryPage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class PropertyTableTest {
  @Test
  public void aPassingPropertyIsOneAssertionAndKeepsThePage() throws Exception {
    String source = """
      | property:agnesse.slim.test.TestSlim |
      | trials | 4 |
      | seed | 42 |
      | string | return int? |
      | one of:red,green,blue | 7 |
      """;
    Ran ran = slim(source);
    assertEquals(source, ran.page.getData().getContent());
    assertEquals(1, ran.summary.getRight());
    assertEquals(0, ran.summary.getWrong());
    assertEquals(0, ran.summary.getExceptions());
    assertEquals(1, BlockJson.from(ran.table).getJSONObject("counts").getInt("right"));
    assertEquals(5, ran.table.getRowCount());
    assertEquals("4 trials", ran.table.cellRecord(1, 1).actual());
    assertEquals("pass", ran.table.cellRecord(1, 1).status());
    assertEquals("42", ran.table.cellRecord(2, 1).actual());
    assertNull(ran.table.cellRecord(2, 1).status());
    assertEquals("seed", ran.table.originalCell(0, 2));
    assertEquals(2, ran.table.getColumnCountInRow(2));
  }

  @Test
  public void aFailureShowsTheFirstUnshrunkRowAndCountsOnce() throws Exception {
    Ran ran = slim("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 3 |
      | seed | 42 |
      | string | return int? |
      | one of:red,green,blue | 8 |
      """);
    String drawn = CellLanguage.parse("one of:red,green,blue").draw(new Seed(42).random()).cell();
    assertEquals(1, ran.summary.getWrong());
    assertEquals(0, ran.summary.getRight());
    assertEquals(1, BlockJson.from(ran.table).getJSONObject("counts").getInt("wrong"));
    assertEquals(6, ran.table.getRowCount());
    assertEquals("red", ran.table.originalCell(0, 5));
    assertEquals("red", ran.table.cellRecord(5, 0).actual());
    assertEquals("blue", drawn);
    CellRecord failed = ran.table.cellRecord(5, 1);
    assertEquals("fail", failed.status());
    assertEquals("7", failed.actual());
    assertEquals("8", failed.expected());
    assertTrue(ran.table.cellRecord(1, 1).actual().contains("shrunk"));
    assertNull(ran.table.cellRecord(1, 1).status());
  }

  @Test
  public void aLaterFailureIsTheRowThatIsShown() throws Exception {
    String spec = "int:0..5";
    Seed seed = new Seed(1);
    String first = draw(spec, seed, 0);
    String second = draw(spec, seed, 1);
    Ran ran = slim("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 2 |
      | seed | 1 |
      | string | get string arg? |
      | %s | 0 |
      """.formatted(spec));
    assertEquals(1, ran.summary.getWrong());
    assertEquals("1", ran.table.originalCell(0, ran.table.getRowCount() - 1));
    assertEquals("0", first);
    assertEquals("3", second);
    assertEquals("0", ran.table.cellRecord(ran.table.getRowCount() - 1, 1).expected());
    assertEquals("1", ran.table.cellRecord(ran.table.getRowCount() - 1, 1).actual());
    assertTrue(ran.table.cellRecord(1, 1).actual().contains("shrunk"));
  }

  @Test
  public void onlyTheFirstFailingOutputCounts() throws Exception {
    Ran ran = slim("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 1 |
      | seed | 1 |
      | string | return string? | return int? |
      | one of:a | nope | 8 |
      """);
    assertEquals(1, ran.summary.getWrong());
    assertEquals(0, ran.summary.getRight());
    int row = ran.table.getRowCount() - 1;
    assertEquals("fail", ran.table.cellRecord(row, 1).status());
    assertEquals("string", ran.table.cellRecord(row, 1).actual());
    assertNull(ran.table.cellRecord(row, 2).status());
  }

  @Test
  public void anEmptyOracleDoesNotFailTheProperty() throws Exception {
    Ran ran = slim("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 2 |
      | seed | 3 |
      | string | return string? | # keep |
      | one of:a,b | | x |
      """);
    assertEquals(1, ran.summary.getRight());
    assertEquals(0, ran.summary.getWrong());
    assertEquals(5, ran.table.getRowCount());
  }

  @Test
  public void anOmittedSeedIsShownAndNotSaved() throws Exception {
    String source = """
      | property:agnesse.slim.test.TestSlim |
      | trials | 2 |
      | string | return int? |
      | one of:red,blue | 7 |
      """;
    Ran ran = slim(source);
    assertEquals(source, ran.page.getData().getContent());
    assertEquals(1, ran.summary.getRight());
    assertEquals("2 trials", ran.table.cellRecord(1, 1).actual());
    String seed = ran.table.cellRecord(ran.table.getRowCount() - 1, 1).actual();
    assertTrue(seed.matches("-?\\d+"));
    assertFalse(source.contains(seed));
  }

  @Test
  public void theSameSeedSendsTheSameCells() throws Exception {
    String page = """
      | property:agnesse.slim.test.TestSlim |
      | seed | 42 |
      | trials | 2 |
      | n | flag | word | items | color | return int? |
      | int!=0:1..9 | boolean | string length 1..1 | list of int:3..3 size 1..1 | one of:red,blue | 7 |
      """;
    List<String> first = setterArgs(open(page).assertions);
    List<String> second = setterArgs(open(page).assertions);
    assertEquals(first, second);
    assertEquals(drawnSequence(42L), first);
  }

  @Test
  public void anOmittedTrialCountRunsOneHundred() throws Exception {
    Opened opened = open("""
      | property:agnesse.slim.test.TestSlim |
      | string | return int? |
      | int:0..1 | 0 |
      """);
    assertEquals(100, count(opened.assertions, "execute"));
    assertEquals(100, count(opened.assertions, "setString"));
    assertEquals(1, count(opened.assertions, "make"));
  }

  @Test
  public void oneTrialStillRuns() throws Exception {
    Opened opened = open("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 1 |
      | seed | 5 |
      | string | return int? |
      | int:1..1 | 7 |
      """);
    assertEquals(1, count(opened.assertions, "execute"));
  }

  @Test
  public void aMissingResultIsAnError() throws Exception {
    Opened opened = open("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 1 |
      | seed | 1 |
      | string | return int? |
      | one of:a | 7 |
      """);
    SlimAssertion.evaluateExpectations(opened.assertions, Map.of());
    finish(opened);
    assertEquals(1, opened.context.getTestSummary().getExceptions());
    assertEquals(0, opened.context.getTestSummary().getRight());
  }

  @Test
  public void aThrownOracleIsAnErrorAndAStopIsAFailure() throws Exception {
    Ran thrown = slim("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 2 |
      | seed | 1 |
      | string | throw normal? |
      | one of:a | true |
      """);
    assertEquals(1, thrown.summary.getExceptions());
    assertEquals(0, thrown.summary.getWrong());
    assertEquals("a", thrown.table.originalCell(0, thrown.table.getRowCount() - 1));
    assertFalse(thrown.table.cellRecord(1, 1).actual().contains("shrunk"));
    assertEquals(1, thrown.calls);

    Ran stopped = slim("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 1 |
      | seed | 1 |
      | string | throw stopping? |
      | one of:a | true |
      """);
    assertEquals(1, stopped.summary.getWrong());
    assertEquals(0, stopped.summary.getExceptions());
    assertEquals(1, stopped.calls);
    assertEquals(0, stopped.shrinkRounds);
    settled(stopped);
  }

  @Test
  public void aMissingSetterIsAnErrorAndAThrowingExecuteIsAnError() throws Exception {
    Ran missing = slim("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 1 |
      | seed | 1 |
      | missing | return int? |
      | one of:a | 7 |
      """);
    assertEquals(1, missing.summary.getExceptions());
    assertEquals(0, missing.summary.getRight());

    Ran exploded = slim("""
      | property:agnesse.slim.test.PropertyProbe |
      | trials | 1 |
      | seed | 4 |
      | n | ok? |
      | int:1..1 | true |
      """);
    assertEquals(1, exploded.summary.getExceptions());
    assertEquals(0, exploded.summary.getWrong());
    assertTrue(exploded.table.cellRecord(1, 1).actual().contains("trial 1 of 1"));
  }

  @Test
  public void aBangNamesAnOutputAndANoteIsNotSent() throws Exception {
    Opened opened = open("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 1 |
      | seed | 2 |
      | string | return int! | # note |
      | one of:a | 7 | comment |
      """);
    assertEquals(1, count(opened.assertions, "returnInt"));
    assertEquals(0, count(opened.assertions, "set#"));
    assertEquals(1, count(opened.assertions, "setString"));
  }

  @Test
  public void aListPropertyPassesAndABadListShowsTheDraw() throws Exception {
    Ran good = slim("""
      | property:agnesse.slim.test.ListReversal |
      | trials | 5 |
      | seed | 9 |
      | items | reversed twice? |
      | list of int:0..3 size 0..3 | true |
      """);
    assertEquals(1, good.summary.getRight());
    assertEquals(0, good.summary.getWrong());
    settled(good);

    String drawn = CellLanguage.parse("list of int:0..4 size 2..3").draw(new Seed(11).random()).cell();
    Ran bad = slim("""
      | property:agnesse.slim.test.ListReversal |
      | trials | 1 |
      | seed | 11 |
      | items | still empty? |
      | list of int:0..4 size 2..3 | true |
      """);
    assertEquals(1, bad.summary.getWrong());
    assertEquals("[0, 0]", bad.table.originalCell(0, bad.table.getRowCount() - 1));
    assertEquals("[4, 0]", drawn);
    assertTrue(bad.table.cellRecord(1, 1).actual().contains("drew " + drawn));
    assertTrue(bad.table.cellRecord(1, 1).actual().contains("shrunk"));
    assertEquals("false", bad.table.cellRecord(bad.table.getRowCount() - 1, 1).actual());
  }

  @Test
  public void aLongListShrinksToAShortListOfSmallElements() throws Exception {
    Ran ran = slim("""
      | property:agnesse.slim.test.ListReversal |
      | trials | 1 |
      | seed | 4 |
      | items | still empty? |
      | list of int:0..9 size 1..6 | true |
      """);
    assertEquals("[0]", ran.table.originalCell(0, ran.table.getRowCount() - 1));
    assertTrue(ran.table.cellRecord(1, 1).actual().contains("drew [6, 5, 0, 9, 2]"));
    assertTrue(ran.table.cellRecord(1, 1).actual().contains("shrunk"));
    assertEquals(1, ran.summary.getWrong());
    assertEquals(ran.shrinkRounds + 1, ran.calls);
  }

  @Test
  public void aScenarioPassesAsOneAssertionAndABadOneShowsTheDraw() throws Exception {
    Ran good = slim("""
      | script | agnesse.slim.test.TestSlim |

      | scenario | echo back | word |
      | check | echo string | @word | @word |

      | property | echo back |
      | trials | 4 |
      | seed | 6 |
      | word | one of:red,blue |
      """);
    assertEquals(1, good.summary.getRight());
    assertEquals(0, good.summary.getWrong());

    String drawn = CellLanguage.parse("one of:red,blue").draw(new Seed(6).random()).cell();
    Ran bad = slim("""
      | script | agnesse.slim.test.TestSlim |

      | scenario | echo back | word |
      | check | echo string | @word | nope |

      | property | echo back |
      | trials | 3 |
      | seed | 6 |
      | word | one of:red,blue |
      """);
    assertEquals(1, bad.summary.getWrong());
    assertEquals(0, bad.summary.getRight());
    assertEquals("red", bad.table.originalCell(0, bad.table.getRowCount() - 1));
    assertEquals("blue", drawn);
    assertEquals(1, bad.table.childTables().size());
    assertNull(bad.table.cellRecord(1, 1).status());
    assertEquals("fail", bad.table.childTables().get(0).cellRecord(1, 3).status());
    settled(bad);
  }

  @Test
  public void theSeedRowIsWrittenOnceAndAShortFirstInputKeepsItsComma() throws Exception {
    Ran once = slim("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 1 |
      | string | return int? |
      | one of:a | 7 |
      """);
    int seeds = 0;
    for (int row = 0; row < once.table.getRowCount(); row++) {
      if ("seed".equals(once.table.originalCell(0, row))) {
        seeds++;
      }
    }
    assertEquals(1, seeds);
    settled(once);

    Ran pair = slim("""
      | script | agnesse.slim.test.TestSlim |

      | scenario | pair | left | right |
      | check | echo string | @left | nope |

      | property | pair |
      | trials | 1 |
      | seed | 3 |
      | left | right |
      | one of:aa,a | one of:z,zz |
      """);
    assertTrue(pair.table.cellRecord(1, 1).actual(), pair.table.cellRecord(1, 1).actual().contains("a, zz"));
  }

  @Test
  public void oneScenarioExceptionFailsTheProperty() throws Exception {
    Ran ran = slim("""
      | script | agnesse.slim.test.TestSlim |

      | scenario | blows up | word |
      | check | throw normal | true |

      | property | blows up |
      | trials | 1 |
      | seed | 1 |
      | word | one of:a |
      """);
    assertEquals(0, ran.summary.getWrong());
    assertEquals(0, ran.summary.getRight());
    assertEquals(1, ran.summary.getExceptions());
  }

  @Test
  public void aScenarioCanUseBracesAndMustNameEveryInput() throws Exception {
    Ran ran = slim("""
      | script | agnesse.slim.test.TestSlim |

      | scenario | echo back | word |
      | check | echo string | @{word} | @{word} |

      | property:echo back |
      | trials | 1 |
      | seed | 1 |
      | word | one of:hi |
      """);
    assertEquals(1, ran.summary.getRight());

    SyntaxError missing = assertThrows(SyntaxError.class, () -> open("""
      | scenario | echo | word | both | extra |
      | check | echo string | @word | @extra |

      | property | echo both |
      | word | one of:a |
      """));
    assertTrue(missing.getMessage().contains("extra"));
  }

  @Test
  public void rejectedTablesSayWhatIsWrong() {
    assertMessage("trials must be at least 1", """
      | property:agnesse.slim.test.TestSlim |
      | trials | 0 |
      | string | return int? |
      | one of:a | 7 |
      """);
    assertMessage("trials must be a whole number", """
      | property:agnesse.slim.test.TestSlim |
      | trials | many |
      | string | return int? |
      | one of:a | 7 |
      """);
    assertMessage("trials is already set", """
      | property:agnesse.slim.test.TestSlim |
      | trials | 1 |
      | trials | 2 |
      | string | return int? |
      | one of:a | 7 |
      """);
    assertMessage("seed must be a whole number", """
      | property:agnesse.slim.test.TestSlim |
      | seed | no |
      | string | return int? |
      | one of:a | 7 |
      """);
    assertMessage("seed is already set", """
      | property:agnesse.slim.test.TestSlim |
      | seed | 1 |
      | seed | 2 |
      | string | return int? |
      | one of:a | 7 |
      """);
    assertMessage("a property needs a header and one generator row", """
      | property:agnesse.slim.test.TestSlim |
      | string |
      """);
    assertMessage("a property has one generator row", """
      | property:agnesse.slim.test.TestSlim |
      | string | return int? |
      | one of:a | 7 |
      | one of:b | 7 |
      """);
    assertMessage("the generator row must have 2 columns", """
      | property:agnesse.slim.test.TestSlim |
      | string | return int? |
      | one of:a |
      """);
    assertMessage("a column needs a name", """
      | property:agnesse.slim.test.TestSlim |
      | | return int? |
      | one of:a | 7 |
      """);
    assertMessage("a column needs a name", """
      | property:agnesse.slim.test.TestSlim |
      | ? |
      | 7 |
      """);
    assertMessage("a property needs a name", "| property |\n");
    assertMessage("a property needs an input", """
      | property:agnesse.slim.test.TestSlim |
      | return int? |
      | 7 |
      """);
    assertMessage("generator spec is missing", """
      | property:agnesse.slim.test.TestSlim |
      | string | return int? |
      | | 7 |
      """);
    assertMessage("a property needs a generator", """
      | scenario | echo back | word |
      | check | echo string | @word | @word |

      | property | echo back |
      | trials | 1 |
      """);
    assertMessage("nope is not an input to the scenario", """
      | scenario | echo back | word |
      | check | echo string | @word | @word |

      | property | echo back |
      | nope | one of:a |
      """);
    assertMessage("word is already set", """
      | scenario | echo back | word |
      | check | echo string | @word | @word |

      | property | echo back |
      | word | one of:a |
      | word | one of:b |
      """);
  }

  @Test
  public void anIntShrinksToTheSmallestFailure() throws Exception {
    Ran ran = slim("""
      | property:agnesse.slim.test.Bound |
      | trials | 30 |
      | seed | 4 |
      | n | below? |
      | int:0..200 | true |
      """);
    assertEquals(1, ran.summary.getWrong());
    assertEquals(0, ran.summary.getRight());
    assertEquals("50", ran.table.originalCell(0, ran.table.getRowCount() - 1));
    assertTrue(ran.table.cellRecord(1, 1).actual().contains("shrunk"));
    assertTrue(ran.shrinkRounds > 0);
    assertEquals(ran.shrinkRounds + 1, ran.calls);
    settled(ran);
  }

  @Test
  public void theSameSeedShrinksToTheSameRow() throws Exception {
    String page = """
      | property:agnesse.slim.test.ListReversal |
      | trials | 1 |
      | seed | 11 |
      | items | still empty? |
      | list of int:0..4 size 2..3 | true |
      """;
    assertEquals(slim(page).table.originalCell(0, 5), slim(page).table.originalCell(0, 5));
    assertEquals("[0, 0]", slim(page).table.originalCell(0, 5));
  }

  @Test
  public void anExceptionDuringShrinkingIsAFailingCandidate() throws Exception {
    Ran ran = slim("""
      | property:agnesse.slim.test.Bound |
      | trials | 1 |
      | seed | 1 |
      | n | below cap? |
      | int:0..120 | true |
      """);
    assertEquals("40", ran.table.originalCell(0, ran.table.getRowCount() - 1));
    assertEquals(1, ran.summary.getWrong());
    assertEquals(0, ran.summary.getExceptions());
    assertTrue(ran.shrinkRounds > 0);
    assertEquals(ran.shrinkRounds + 1, ran.calls);
  }

  @Test
  public void aStopTestInsideAShrinkRoundSendsNoFurtherRound() throws Exception {
    Ran ran = slim("""
      | property:agnesse.slim.test.Bound |
      | trials | 1 |
      | seed | 5 |
      | n | halt at sixty? |
      | int:58..70 | true |
      """);
    assertEquals("60", ran.table.originalCell(0, ran.table.getRowCount() - 1));
    assertEquals(2, ran.calls);
    assertEquals(1, ran.shrinkRounds);
    assertEquals(1, ran.summary.getWrong());
    assertEquals(0, ran.summary.getExceptions());
  }

  @Test
  public void theShrinkCapIsReported() throws Exception {
    Ran ran = slim("""
      | property:agnesse.slim.test.Bound |
      | trials | 1 |
      | seed | 1 |
      | n | below? |
      | int:200..399 | true |
      """);
    String caption = ran.table.cellRecord(1, 1).actual();
    assertTrue(caption, caption.contains("drew 396; shrink cap"));
    assertEquals("364", ran.table.originalCell(0, ran.table.getRowCount() - 1));
    assertEquals(ShrinkLoop.CAP, ran.shrinkRounds);
    assertEquals(ran.shrinkRounds + 1, ran.calls);
    assertEquals(1, ran.summary.getWrong());
  }

  @Test
  public void aBlankTrialsCellUsesTheDefaultAndANegativeSeedIsKept() throws Exception {
    Opened trials = open("""
      | property:agnesse.slim.test.TestSlim |
      | trials | |
      | string | return int? |
      | int:0..1 | 0 |
      """);
    assertEquals(100, count(trials.assertions, "execute"));
    Opened seed = open("""
      | property:agnesse.slim.test.TestSlim |
      | seed | -3 |
      | trials | 1 |
      | string | return int? |
      | int:1..1 | 7 |
      """);
    SlimAssertion.evaluateExpectations(seed.assertions, answered(seed.assertions, Map.of("returnInt", "7")));
    finish(seed);
    assertEquals("-3", seed.property().cellRecord(1, 1).actual());
    assertEquals(1, seed.context.getTestSummary().getRight());
  }

  @Test
  public void aNarrowSeedCellIsWidenedOnTheResult() throws Exception {
    Opened opened = open("""
      | property:agnesse.slim.test.TestSlim |
      | seed |
      | n |
      | int:1..1 |
      """);
    SlimAssertion.evaluateExpectations(opened.assertions, Map.of());
    finish(opened);
    assertEquals(1, opened.context.getTestSummary().getRight());
    assertEquals(0, opened.context.getTestSummary().getWrong());
    assertTrue(opened.property().cellRecord(1, 1).actual().matches("-?\\d+"));
  }

  @Test
  public void allOfReplacesSamplingWithTheDomain() throws Exception {
    Ran omitted = roundTrip("all of:red,blue", "");
    assertEquals(1, omitted.summary.getRight());
    assertEquals("2 trials", trialCell(omitted));
    Ran blank = roundTrip("all of:red,blue", "| trials | |\n");
    assertEquals("2 trials", trialCell(blank));
    Ran numbered = roundTrip("all of:red,blue", "| trials | 2 |\n");
    assertEquals("2 trials", trialCell(numbered));
    Ran product = roundTrip("all of:a,b x 1,2", "");
    assertEquals("4 trials", trialCell(product));
    Ran columns = slim("""
      | script | agnesse.slim.test.TestSlim |

      | scenario | pair | left | and | right |
      | check | echo string | @left | @left |

      | property | pair and |
      | seed | 4 |
      | left | all of:a,b |
      | right | all of:1,2 |
      """);
    assertEquals(1, columns.summary.getRight());
    assertEquals("4 trials", trialCell(columns));
  }

  @Test
  public void allOfRefusesATrialCountThatIsNotTheDomain() throws Exception {
    assertMessage("cases, not", """
      | script | agnesse.slim.test.TestSlim |

      | scenario | round trip | value |
      | check | echo string | @value | @value |

      | property | round trip |
      | trials | 3 |
      | value | all of:red,blue |
      """);
    assertMessage("replaces sampling", """
      | script | agnesse.slim.test.TestSlim |

      | scenario | pair | left | and | right |
      | check | echo string | @left | @left |

      | property | pair and |
      | left | all of:a,b |
      | right | int:0..1 |
      """);
    assertMessage("limit", """
      | property:agnesse.slim.test.TestSlim |
      | a | b | return int? |
      | all of:%s | all of:%s | 7 |
      """.formatted(choices(20), choices(20)));
    String domain = choices(16);
    Opened full = open("""
      | property:agnesse.slim.test.TestSlim |
      | a | b | return int? |
      | all of:%s | all of:%s | 7 |
      """.formatted(domain, domain));
    assertEquals(256, count(full.assertions, "returnInt"));
  }

  @Test
  public void theFirstFailingCaseDoesNotDependOnTheSeed() throws Exception {
    assertEquals("red", firstFailure(1));
    assertEquals("red", firstFailure(9));
    assertFalse(firstLabel(1).contains("shrunk"));
  }

  @Test
  public void aHostileValueIsEscapedOnTheResultPage() throws Exception {
    String html = failedChoice("one of:<b>bold</b>", 0).table.toHtml();
    assertTrue(html, html.contains("&lt;b&gt;bold&lt;/b&gt;"));
    assertFalse(html, html.contains("<b>"));
    String amp = failedChoice("one of:a&b", 0).table.toHtml();
    assertTrue(amp, amp.contains("a&amp;b"));
    assertFalse(amp, amp.contains("a&b"));
    long seed = scriptSeed();
    String script = failedChoice("one of:ok,<script>alert(1)</script>", seed).table.toHtml();
    assertTrue(script, script.contains("&lt;script&gt;"));
    assertFalse(script, script.contains("<script>"));
  }

  private static Ran failedChoice(String spec, long seed) throws Exception {
    return slim("""
      | property:agnesse.slim.test.TestSlim |
      | trials | 1 |
      | seed | %d |
      | string | return int? |
      | %s | 8 |
      """.formatted(seed, spec));
  }

  private static Ran roundTrip(String spec, String trials) throws Exception {
    return slim("""
      | script | agnesse.slim.test.TestSlim |

      | scenario | round trip | value |
      | check | echo string | @value | @value |

      | property | round trip |
      %s| seed | 1 |
      | value | %s |
      """.formatted(trials, spec));
  }

  private static String firstFailure(long seed) throws Exception {
    Ran ran = slim("""
      | script | agnesse.slim.test.TestSlim |

      | scenario | echo back | word |
      | check | echo string | @word | nope |

      | property | echo back |
      | trials | 2 |
      | seed | %d |
      | word | all of:red,blue |
      """.formatted(seed));
    return ran.table.originalCell(0, ran.table.getRowCount() - 1);
  }

  private static String firstLabel(long seed) throws Exception {
    Ran ran = slim("""
      | script | agnesse.slim.test.TestSlim |

      | scenario | echo back | word |
      | check | echo string | @word | nope |

      | property | echo back |
      | trials | 2 |
      | seed | %d |
      | word | all of:red,blue |
      """.formatted(seed));
    return trialCell(ran);
  }

  private static long scriptSeed() {
    String spec = "one of:ok,<script>alert(1)</script>";
    for (long seed = 0; seed < 20; seed++) {
      if (CellLanguage.parse(spec).draw(new Seed(seed).random()).cell().contains("script")) {
        return seed;
      }
    }
    return 0;
  }

  private static String choices(int count) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        out.append(',');
      }
      out.append('c').append(i);
    }
    return out.toString();
  }

  private static String trialCell(Ran ran) {
    for (int row = 0; row < ran.table.getRowCount(); row++) {
      if ("trials".equals(ran.table.originalCell(0, row))) {
        return ran.table.cellRecord(row, 1).actual();
      }
    }
    return "";
  }

  private static void assertMessage(String message, String markdown) {
    SyntaxError error = assertThrows(SyntaxError.class, () -> open(markdown));
    assertTrue(error.getMessage(), error.getMessage().contains(message));
  }

  private static List<String> drawnSequence(long seed) {
    Random random = new Seed(seed).random();
    List<String> drawn = new ArrayList<>();
    String[] specs = {"int!=0:1..9", "boolean", "string length 1..1", "list of int:3..3 size 1..1", "one of:red,blue"};
    for (int trial = 0; trial < 2; trial++) {
      for (String spec : specs) {
        drawn.add(CellLanguage.parse(spec).draw(random).cell());
      }
    }
    return drawn;
  }

  private static String draw(String spec, Seed seed, int index) {
    Random random = seed.random();
    String value = "";
    for (int i = 0; i <= index; i++) {
      value = CellLanguage.parse(spec).draw(random).cell();
    }
    return value;
  }

  private static List<String> setterArgs(List<SlimAssertion> assertions) {
    List<String> args = new ArrayList<>();
    for (SlimAssertion assertion : assertions) {
      String text = assertion.getInstruction().toString();
      if (text.contains("methodName='set")) {
        int start = text.indexOf("args=") + "args=".length();
        String argsText = text.substring(start, text.lastIndexOf('}'));
        args.add(argsText.substring(1, argsText.length() - 1));
      }
    }
    return args;
  }

  private static int count(List<SlimAssertion> assertions, String method) {
    int found = 0;
    for (SlimAssertion assertion : assertions) {
      if (assertion.getInstruction().toString().contains("methodName='" + method + "'")
        || assertion.getInstruction().toString().contains("'" + method + "'")) {
        found++;
      }
    }
    return found;
  }

  private static Map<String, Object> answered(List<SlimAssertion> assertions, Map<String, String> methods) {
    Map<String, Object> results = new HashMap<>();
    for (SlimAssertion assertion : assertions) {
      String text = assertion.getInstruction().toString();
      String value = "OK";
      for (Map.Entry<String, String> method : methods.entrySet()) {
        if (text.contains("methodName='" + method.getKey() + "'")) {
          value = method.getValue();
        }
      }
      results.put(assertion.getInstruction().getId(), value);
    }
    return results;
  }

  private static Opened open(String markdown) throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("Example"), markdown);
    SlimTestContextImpl context = new SlimTestContextImpl(new WikiTestPage(page));
    MarkdownTableScanner scanner = new MarkdownTableScanner(page.getData().getContent());
    SlimTableFactory factory = new SlimTableFactory();
    List<SlimTable> tables = new ArrayList<>();
    List<SlimAssertion> assertions = new ArrayList<>();
    for (int index = 0; index < scanner.getTableCount(); index++) {
      SlimTable slim = factory.makeSlimTable(scanner.getTable(index), "t" + index, context);
      tables.add(slim);
      assertions.addAll(slim.getAssertions());
    }
    return new Opened(context, tables, assertions);
  }

  private static void settled(Ran ran) throws Exception {
    int rows = ran.table.getRowCount();
    int children = ran.table.childTables().size();
    int wrong = ran.summary.getWrong();
    int right = ran.summary.getRight();
    int exceptions = ran.summary.getExceptions();
    assertTrue(PropertyTable.latest.furtherAssertions().isEmpty());
    assertEquals(rows, ran.table.getRowCount());
    assertEquals(children, ran.table.childTables().size());
    assertEquals(wrong, ran.summary.getWrong());
    assertEquals(right, ran.summary.getRight());
    assertEquals(exceptions, ran.summary.getExceptions());
  }

  private static Ran slim(String markdown) throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    WikiPage page = WikiPageUtil.addPage(root, PathParser.parse("Example"), markdown);
    Calls calls = new Calls(new InProcessSlimClientBuilder(
      new WikiPageDescriptor(page, true, false), PropertyTableTest.class.getClassLoader()).build());
    HtmlSlimTestSystem system = new HtmlSlimTestSystem("slim", calls, new SlimTableFactory());
    Grab grab = new Grab();
    system.addTestSystemListener(grab);
    system.start();
    try {
      system.runTests(new WikiTestPage(page));
    } finally {
      system.bye();
    }
    return new Ran(page, grab.summary, grab.property(), PropertyTable.lastRounds, calls.n);
  }

  private static final class Calls implements SlimClient {
    private final SlimClient inner;
    private int n;

    private Calls(SlimClient inner) {
      this.inner = inner;
    }

    @Override public void start() throws java.io.IOException, SlimVersionMismatch { inner.start(); }
    @Override public void connect() throws java.io.IOException, SlimVersionMismatch { inner.connect(); }
    @Override public void bye() throws java.io.IOException { inner.bye(); }
    @Override public void kill() { inner.kill(); }
    @Override public Map<String, Object> invokeAndGetResponse(List<agnesse.slim.instructions.Instruction> statements) throws SlimCommunicationException {
      n++;
      return inner.invokeAndGetResponse(statements);
    }
  }

  private static void finish(Opened opened) throws Exception {
    PropertyTable property = (PropertyTable) opened.tables.get(opened.tables.size() - 1);
    List<SlimAssertion> more = property.furtherAssertions();
    while (!more.isEmpty()) {
      SlimAssertion.evaluateExpectations(more, Map.of());
      more = property.furtherAssertions();
    }
  }

  private static final class Opened {
    private final SlimTestContextImpl context;
    private final List<SlimTable> tables;
    private final List<SlimAssertion> assertions;

    private Opened(SlimTestContextImpl context, List<SlimTable> tables, List<SlimAssertion> assertions) {
      this.context = context;
      this.tables = tables;
      this.assertions = assertions;
    }

    private MarkdownTable property() {
      return (MarkdownTable) tables.get(tables.size() - 1).getTable();
    }
  }

  private static final class Grab implements TestSystemListener, TableFinished {
    private TestSummary summary;
    private final List<MarkdownTable> seen = new ArrayList<>();

    @Override
    public void testOutputChunk(TestPage testPage, String output) {
    }

    @Override
    public void tableFinished(TestPage page, MarkdownTable table) {
      seen.add(table);
    }

    private MarkdownTable property() {
      for (int i = seen.size() - 1; i >= 0; i--) {
        MarkdownTable table = seen.get(i);
        if (table.getRowCount() > 0 && table.originalCell(0, 0).startsWith("property")) {
          return table;
        }
      }
      return seen.get(seen.size() - 1);
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
      summary = testSummary;
    }
  }

  private static final class Ran {
    private final WikiPage page;
    private final TestSummary summary;
    private final MarkdownTable table;
    private final int shrinkRounds;
    private final int calls;

    private Ran(WikiPage page, TestSummary summary, MarkdownTable table, int shrinkRounds, int calls) {
      this.page = page;
      this.summary = summary;
      this.table = table;
      this.shrinkRounds = shrinkRounds;
      this.calls = calls;
    }

    private MarkdownTableScanner systemTables(Ran ran) {
      return new MarkdownTableScanner(List.of(ran.table));
    }
  }
}
