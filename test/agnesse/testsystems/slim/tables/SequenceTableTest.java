package agnesse.testsystems.slim.tables;

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
import agnesse.testsystems.slim.SlimTestContextImpl;
import agnesse.testsystems.slim.SlimVersionMismatch;
import agnesse.testsystems.slim.TableFinished;
import agnesse.testsystems.slim.results.CellRecord;
import agnesse.wiki.PathParser;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageUtil;
import agnesse.wiki.fs.InMemoryPage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SequenceTableTest {
  @Test
  public void aPassingSequenceIsOneAssertionAndKeepsThePage() throws Exception {
    String source = """
        | sequence | agnesse.slim.test.ItemLife |
        | trials | 1 |
        | steps | 3..3 |
        | seed | 2 |
        | state | phase=new; blocked=no |
        | command | when | next | result! | phase? |
        | open | phase=new | phase=open | ok | open |
        | block | phase=open | phase=blocked; blocked=yes | ok | blocked |
        | ping | phase=blocked | | ok | blocked |
        | invariant | when | model | query | expect |
        | | phase=new | phase=new | | kept |
        | | blocked=yes | phase!=released | released! | false |
        """;
    Ran ran = slim(source);
    assertEquals(source, ran.page.getData().getContent());
    assertEquals(1, ran.summary.getRight());
    assertEquals(0, ran.summary.getWrong());
    assertEquals(0, ran.summary.getExceptions());
    assertEquals("1 trials", ran.table.cellRecord(1, 1).actual());
    assertEquals("pass", ran.table.cellRecord(1, 1).status());
    assertEquals("2", ran.table.cellRecord(3, 1).actual());
    assertEquals(lines(source), ran.table.getRowCount());
  }

  @Test
  public void eachTrialBuildsAFreshFixture() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.SequenceCounter |
        | trials | 2 |
        | steps | 2..2 |
        | seed | 1 |
        | state | n=0; one=1 |
        | command | when | next | count? |
        | bump | | n=@n+@one | @n |
        """);
    assertEquals(1, ran.summary.getRight());
    assertEquals(0, ran.summary.getWrong());
    assertEquals("2 trials", trials(ran.table));
  }

  @Test
  public void aConstructorArgumentIsTheCellAfterTheClass() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.SequenceCounter | 3 |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | n=3; one=1 |
        | command | when | next | count? |
        | bump | | n=@n+@one | @n |
        """);
    assertEquals(1, ran.summary.getRight());
    assertEquals(0, ran.summary.getWrong());
  }

  @Test
  public void defaultsFillABlankStepsRowAndAMissingTrialsRow() throws Exception {
    Ran ran = slim("""
        | sequence | agnesse.slim.test.ItemLife |
        | steps | |
        | state | phase=new; blocked=no |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertEquals(1, ran.summary.getRight());
    assertEquals("100 trials", trials(ran.table));
    assertTrue(seed(ran.table), seed(ran.table).matches("-?\\d+"));
  }

  @Test
  public void aBlankTrialsCellUsesTheDefault() throws Exception {
    Opened opened = open("""
        | sequence:agnesse.slim.test.ItemLife |
        | trials | |
        | steps | 1..1 |
        | seed | 1 |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertEquals(200, opened.assertions.size());
  }

  @Test
  public void aBlankSeedIsShownAndNotWrittenBack() throws Exception {
    String source = """
        | sequence:agnesse.slim.test.ItemLife |
        | trials | 1 |
        | steps | 1..1 |
        | seed | |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """;
    Ran ran = slim(source);
    assertEquals(source, ran.page.getData().getContent());
    assertEquals(1, ran.summary.getRight());
    String seed = seed(ran.table);
    assertTrue(seed, seed.matches("-?\\d+"));
  }

  @Test
  public void aBlockedReleaseShrinksToTheForbiddenStep() throws Exception {
    String source = """
        | sequence:agnesse.slim.test.ItemLifeLeak |
        | trials | 8 |
        | steps | 4..4 |
        | seed | 1 |
        | state | phase=new; blocked=no |
        | command | when | next | result? | phase? |
        | open | phase=new | phase=open | ok | open |
        | block | phase=open | phase=blocked; blocked=yes | ok | blocked |
        | ping | phase=blocked | | ok | blocked |
        | release | phase=blocked | | refused | blocked |
        | invariant | when | model | query | expect |
        | | blocked=yes | phase!=released | released? | false |
        """;
    Ran ran = slim(source);
    assertEquals(source, ran.page.getData().getContent());
    assertEquals(0, ran.summary.getRight());
    assertEquals(1, ran.summary.getWrong());
    assertEquals(0, ran.summary.getExceptions());
    assertEquals(List.of("open", "block", "release"), shown(ran.table, source));
    CellRecord failed = failure(ran.table);
    assertEquals("ok", failed.actual());
    assertEquals("refused", failed.expected());
    String caption = trials(ran.table);
    assertTrue(caption, caption.contains("trial 2 of 8"));
    assertTrue(caption, caption.contains("step 3 release"));
    assertTrue(caption, caption.contains("shrunk"));
    assertEquals("1", seed(ran.table));
  }

  @Test
  public void anAuditedLeakShrinksTheAmount() throws Exception {
    String source = """
        | sequence:agnesse.slim.test.AuditedLeak |
        | trials | 1 |
        | steps | 2..2 |
        | seed | 1 |
        | state | phase=open; value=0 |
        | command | when | next | amount | result? | value? |
        | lock | phase=open | phase=locked; lockedValue=@value | | ok | @value |
        | tweak | phase=locked | | int:5..20 | refused | @value |
        """;
    Ran ran = slim(source);
    assertEquals(source, ran.page.getData().getContent());
    assertEquals(1, ran.summary.getWrong());
    assertEquals(List.of("lock", "tweak"), shown(ran.table, source));
    int tweak = resultRow(ran.table, source, "tweak");
    assertEquals("5", ran.table.originalCell(3, tweak));
    assertEquals("ok", failure(ran.table).actual());
    assertEquals("refused", failure(ran.table).expected());
    assertTrue(trials(ran.table), trials(ran.table).contains("shrunk"));
  }

  @Test
  public void aLyingQueryShrinksToTheBlockedItem() throws Exception {
    String source = """
        | sequence:agnesse.slim.test.ItemLifeQueryLeak |
        | trials | 1 |
        | steps | 4..4 |
        | seed | 1 |
        | state | phase=new; blocked=no |
        | command | when | next | result? | phase? |
        | open | phase=new | phase=open | ok | open |
        | block | phase=open | phase=blocked; blocked=yes | ok | blocked |
        | ping | phase=blocked | | ok | blocked |
        | release | phase=blocked | | refused | blocked |
        | invariant | when | model | query | expect |
        | | blocked=yes | phase!=released | released? | false |
        """;
    Ran ran = slim(source);
    assertEquals(1, ran.summary.getWrong());
    assertEquals(List.of("open", "block"), shown(ran.table, source));
    assertEquals("true", failure(ran.table).actual());
    assertEquals("false", failure(ran.table).expected());
    int invariant = resultRow(ran.table, source, "invariant");
    assertEquals("released?", ran.table.originalCell(3, invariant));
    assertEquals("fail", ran.table.cellRecord(invariant, 4).status());
    assertTrue(trials(ran.table), trials(ran.table).contains("drew open, block"));
    assertTrue(trials(ran.table), trials(ran.table).contains("invariant released"));
    assertTrue(trials(ran.table), trials(ran.table).contains("shrunk"));
  }

  @Test
  public void aModelThatReleasesABlockedPhaseFailsTheInvariant() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.ItemLife |
        | trials | 1 |
        | steps | 2..2 |
        | seed | 1 |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        | release | phase=open | phase=released | ok |
        | invariant | when | model | query | expect |
        | | phase=released | phase!=released | | kept |
        """);
    assertEquals(1, ran.summary.getWrong());
    assertEquals("released", failure(ran.table).actual());
    assertEquals("released", failure(ran.table).expected());
    int invariant = resultRow(ran.table, sourceLines(ran), "invariant");
    assertEquals("fail", ran.table.cellRecord(invariant, 2).status());
    assertTrue(trials(ran.table), trials(ran.table).contains("invariant phase"));
  }

  @Test
  public void aModelNameMissingFromThisStepFailsAsBlank() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.ItemLife |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        | mark | phase=open | extra=yes | ok |
        | invariant | when | model | query | expect |
        | | phase=open | extra=yes | | kept |
        """);
    assertEquals(1, ran.summary.getWrong());
    assertEquals("", failure(ran.table).actual());
    assertEquals("yes", failure(ran.table).expected());
    assertTrue(trials(ran.table), trials(ran.table).contains("invariant extra"));
  }

  @Test
  public void aQueryThatThrowsBreaksTheStep() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.ItemLife |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | phase=new |
        | command | when | next | result? | missing? |
        | open | phase=new | phase=open | ok | open |
        """);
    assertEquals(0, ran.summary.getWrong());
    assertEquals(1, ran.summary.getExceptions());
    assertTrue(trials(ran.table), trials(ran.table).contains("step 1 open"));
  }

  @Test
  public void aWrongCountIsTheFailingCell() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.SequenceCounter |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | n=0; one=1 |
        | command | when | next | count? |
        | bump | | n=@n+@one | 0 |
        """);
    assertEquals(1, ran.summary.getWrong());
    assertEquals("1", failure(ran.table).actual());
    assertEquals("0", failure(ran.table).expected());
    assertEquals(2, ran.table.getColumnCountInRow(trialsRow(ran.table)));
    assertFalse(trials(ran.table).contains("shrunk"));
  }

  @Test
  public void aResultMismatchStandsWhenTheModelAlsoFails() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.ItemLife |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | no |
        | invariant | when | model | query | expect |
        | | | phase!=open | | kept |
        """);
    assertEquals(1, ran.summary.getWrong());
    assertEquals("ok", failure(ran.table).actual());
    assertEquals("no", failure(ran.table).expected());
    assertTrue(trials(ran.table), trials(ran.table).contains("step 1 open"));
  }

  @Test
  public void aLaterFailureStillShowsTheEarlierModel() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.SequenceCounter |
        | trials | 1 |
        | steps | 2..2 |
        | seed | 1 |
        | state | n=0; one=1 |
        | command | when | next | count? |
        | bump | | n=@n+@one | @n |
        | invariant | when | model | query | expect |
        | | | n!=2 | | kept |
        """);
    int first = resultRow(ran.table, sourceLines(ran), "bump");
    int second = resultRow(ran.table, first + 1, "bump");
    assertEquals("1", ran.table.originalCell(3, first));
    assertEquals("2", ran.table.originalCell(3, second));
    assertEquals("2", failure(ran.table).actual());
    assertEquals("2", failure(ran.table).expected());
    assertTrue(trials(ran.table), trials(ran.table).contains("invariant n"));
  }

  @Test
  public void shrinkingStopsAtTheCap() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.SequenceNote |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 5 |
        | command | when | next | text | result? |
        | note | | | string length 1..120 | yes |
        """);
    assertEquals(1, ran.summary.getWrong());
    String text = ran.table.originalCell(3, resultRow(ran.table, sourceLines(ran), "note"));
    assertEquals(trials(ran.table), 49, text.length());
    assertTrue(trials(ran.table), trials(ran.table).contains("shrink cap"));
  }

  @Test
  public void aStopTestEndsTheShrink() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.SequenceHalt |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | command | when | next | result? |
        | halt | | | ok |
        """);
    assertEquals(1, ran.summary.getWrong());
    assertEquals(0, ran.summary.getExceptions());
    assertEquals(1, ran.calls);
    assertTrue(trials(ran.table), trials(ran.table).contains("step 1 halt"));
    assertFalse(trials(ran.table).contains("shrunk"));
  }

  @Test
  public void aStopSuiteEndsTheShrink() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.SequenceHalt |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | command | when | next | result? |
        | suite | | | ok |
        """);
    assertEquals(1, ran.summary.getWrong());
    assertEquals(0, ran.summary.getExceptions());
    assertEquals(1, ran.calls);
    assertTrue(trials(ran.table), trials(ran.table).contains("step 1 suite"));
  }

  @Test
  public void aStopInsideAShrinkRoundIsTheFailure() throws Exception {
    String source = """
        | sequence:agnesse.slim.test.SequenceStopOnOne |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 3 |
        | command | when | next | amount | result? |
        | take | | | int:1..3 | yes |
        """;
    Ran ran = slim(source);
    assertEquals(1, ran.summary.getWrong());
    assertEquals(0, ran.summary.getExceptions());
    assertEquals(2, ran.calls);
    assertEquals("1", ran.table.originalCell(3, resultRow(ran.table, source, "take")));
  }

  @Test
  public void anOrdinaryExceptionIsOneError() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.SequenceBoom |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | |
        | command | when | next | result? |
        | boom | | | ok |
        """);
    assertEquals(0, ran.summary.getWrong());
    assertEquals(1, ran.summary.getExceptions());
    assertTrue(trials(ran.table), trials(ran.table).contains("step 1 boom"));
  }

  @Test
  public void aMissingClassBreaksTheMake() throws Exception {
    Ran ran = slim("""
        | sequence | no.such.Fixture |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | command | when | next | result? |
        | ping | | | ok |
        """);
    assertEquals(1, ran.summary.getExceptions());
    assertEquals(0, ran.summary.getWrong());
    assertTrue(trials(ran.table), trials(ran.table).contains("make"));
  }

  @Test
  public void aMissingSlimResultBreaksTheTrial() throws Exception {
    Opened make = open("""
        | sequence:agnesse.slim.test.SequenceCounter |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | n=0 |
        | command | when | next | one | flag | count? |
        | bump | | n=@n+@one | 1 | on | @n |
        """);
    make.assertions.get(0).getExpectation().evaluateExpectation(null);
    SequenceTable.latest.finishNow();
    assertEquals(1, make.context.getTestSummary().getExceptions());

    Opened call = open("""
        | sequence:agnesse.slim.test.SequenceCounter |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | n=0 |
        | command | when | next | result? |
        | bump | | | ok |
        """);
    call.assertions.get(1).getExpectation().evaluateExpectation(null);
    SequenceTable.latest.finishNow();
    assertEquals(1, call.context.getTestSummary().getExceptions());

    Opened query = open("""
        | sequence:agnesse.slim.test.SequenceCounter |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | n=0 |
        | command | when | next | count? |
        | bump | | | 0 |
        """);
    query.assertions.get(2).getExpectation().evaluateExpectation(null);
    SequenceTable.latest.finishNow();
    assertEquals(1, query.context.getTestSummary().getExceptions());
  }

  @Test
  public void finishNowBeforeAFailureLeavesTheTableUnpainted() throws Exception {
    Opened opened = open("""
        | sequence:agnesse.slim.test.SequenceCounter |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | n=0 |
        | command | when | next | result? |
        | bump | | | ok |
        """);
    int rows = opened.table().getRowCount();
    SequenceTable.latest.finishNow();
    assertEquals(0, opened.context.getTestSummary().getRight());
    assertEquals(rows, opened.table().getRowCount());
    assertTrue(SequenceTable.latest.furtherAssertions().isEmpty());
    assertEquals(1, opened.context.getTestSummary().getRight());
    SequenceTable.latest.finishNow();
    assertEquals(1, opened.context.getTestSummary().getRight());
  }

  @Test
  public void rejectedTablesSayWhatIsWrong() {
    assertMessage("a sequence needs a name", """
        | sequence |
        | trials | 1 |
        """);
    assertMessage("a sequence needs a name", """
        | sequence | | kept |
        | trials | 1 |
        """);
    assertMessage("a sequence needs a command", """
        | sequence:agnesse.slim.test.ItemLife |
        | trials | 1 |
        """);
    assertMessage("unknown sequence setting colour", """
        | sequence:agnesse.slim.test.ItemLife |
        | colour | red |
        """);
    assertMessage("a command starts with command, when, and next", """
        | sequence:agnesse.slim.test.ItemLife |
        | go | when | next | result? |
        """);
    assertMessage("a command starts with command, when, and next", """
        | sequence:agnesse.slim.test.ItemLife |
        | command |
        """);
    assertMessage("a command starts with command, when, and next", """
        | sequence:agnesse.slim.test.ItemLife |
        | command | phase | next |
        """);
    assertMessage("a command starts with command, when, and next", """
        | sequence:agnesse.slim.test.ItemLife |
        | command | when | then |
        """);
    assertMessage("trials must be at least 1", """
        | sequence:agnesse.slim.test.ItemLife |
        | trials | 0 |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("trials must be a whole number", """
        | sequence:agnesse.slim.test.ItemLife |
        | trials | many |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("trials is already set", """
        | sequence:agnesse.slim.test.ItemLife |
        | trials | 1 |
        | trials | 2 |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("steps is already set", """
        | sequence:agnesse.slim.test.ItemLife |
        | steps | 1..2 |
        | steps | 1..3 |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("steps must be low..high from 1 through 32", """
        | sequence:agnesse.slim.test.ItemLife |
        | steps | 1..33 |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("steps must be low..high from 1 through 32", """
        | sequence:agnesse.slim.test.ItemLife |
        | steps | many |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("seed must be a whole number", """
        | sequence:agnesse.slim.test.ItemLife |
        | seed | no |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("seed is already set", """
        | sequence:agnesse.slim.test.ItemLife |
        | seed | 1 |
        | seed | 2 |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("state is already set", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | state | phase=open |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("phase is already set", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new; phase=open |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("state must be name=value", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("state must be name=value", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase =new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("state must be name=value", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | 1phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("state must be name=value", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase= |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("a command needs a name", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | | phase=new | phase=open | ok |
        """);
    assertMessage("a command row must have 4 columns", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open |
        """);
    assertMessage("a column needs a name", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | ? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("a column needs a name", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | | result? |
        | open | phase=new | phase=open | 1 | ok |
        """);
    assertMessage("a check needs an expected value", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? | phase? |
        | open | phase=new | phase=open | | open |
        """);
    assertMessage("amount is already set", """
        | sequence:agnesse.slim.test.ItemLife |
        | command | when | next | amount | amount | result? |
        | open | | | 1 | 2 | ok |
        """);
    assertMessage("all of replaces sampling", """
        | sequence:agnesse.slim.test.ItemLife |
        | command | when | next | amount | result? |
        | open | | | all of:a,b | ok |
        """);
    assertMessage("a sequence argument cannot draw a time", """
        | sequence:agnesse.slim.test.ItemLife |
        | command | when | next | when | result? |
        | open | | | date:2020-01-01 | ok |
        """);
    assertMessage("a sequence argument cannot draw a time", """
        | sequence:agnesse.slim.test.ItemLife |
        | command | when | next | days | result? |
        | open | | | list of date:2020-01-01..2020-01-02 size 1..1 | ok |
        """);
    assertMessage("inverted", """
        | sequence:agnesse.slim.test.ItemLife |
        | command | when | next | amount | result? |
        | open | | | int:10..1 | ok |
        """);
    assertMessage("invariant columns are when, model, query, and expect", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        | invariant | when | model |
        """);
    assertMessage("invariant columns are when, model, query, and expect", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        | invariant | state | model | query | expect |
        """);
    assertMessage("invariant columns are when, model, query, and expect", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        | invariant | when | check | query | expect |
        """);
    assertMessage("invariant columns are when, model, query, and expect", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        | invariant | when | model | ask | expect |
        """);
    assertMessage("invariant columns are when, model, query, and expect", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        | invariant | when | model | query | wanted |
        """);
    assertMessage("invariant columns are when, model, query, and expect", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        | invariant | when | model | query | expect |
        | | phase=new | phase=new | released? |
        """);
    assertMessage("an invariant needs a model or a query", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        | invariant | when | model | query | expect |
        | | | | | x |
        """);
    assertMessage("a sequence needs a check", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next |
        | open | phase=new | phase=open |
        """);
    assertMessage("unsatisfiable precondition", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=open; phase=blocked | phase=open | ok |
        """);
    assertMessage("no command is possible at the start", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=open | phase=open | ok |
        """);
    assertMessage("a sequence stops before 2 steps", """
        | sequence:agnesse.slim.test.ItemLife |
        | steps | 2..2 |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("unknown reference", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new | other=@missing | ok |
        """);
    assertMessage("blocked has no initial value", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | phase=new |
        | command | when | next | result? |
        | open | phase=new; blocked=no | phase=open | ok |
        """);
  }

  private static void assertMessage(String message, String markdown) {
    SyntaxError error = assertThrows(SyntaxError.class, () -> open(markdown));
    assertTrue(error.getMessage(), error.getMessage().contains(message));
  }

  private static List<String> shown(MarkdownTable table, String source) {
    List<String> names = new ArrayList<>();
    for (int row = lines(source); row < table.getRowCount(); row++) {
      String name = table.originalCell(0, row);
      if (name.equals("invariant") || name.equals("seed") || name.equals("trials")) {
        break;
      }
      names.add(name);
    }
    return names;
  }

  private static int resultRow(MarkdownTable table, String source, String command) {
    for (int row = lines(source); row < table.getRowCount(); row++) {
      if (command.equals(table.originalCell(0, row))) {
        return row;
      }
    }
    return -1;
  }

  private static int resultRow(MarkdownTable table, int from, String command) {
    for (int row = from; row < table.getRowCount(); row++) {
      if (command.equals(table.originalCell(0, row))) {
        return row;
      }
    }
    return -1;
  }

  private static int sourceLines(Ran ran) {
    return lines(ran.page.getData().getContent());
  }

  private static CellRecord failure(MarkdownTable table) {
    for (int row = 0; row < table.getRowCount(); row++) {
      for (int column = 0; column < table.getColumnCountInRow(row); column++) {
        CellRecord record = table.cellRecord(row, column);
        if ("fail".equals(record.status())) {
          return record;
        }
      }
    }
    return null;
  }

  private static int trialsRow(MarkdownTable table) {
    for (int row = 0; row < table.getRowCount(); row++) {
      if ("trials".equals(table.originalCell(0, row))) {
        return row;
      }
    }
    return -1;
  }

  private static String trials(MarkdownTable table) {
    for (int row = 0; row < table.getRowCount(); row++) {
      if ("trials".equals(table.originalCell(0, row))) {
        return table.cellRecord(row, 1).actual();
      }
    }
    return "";
  }

  private static String seed(MarkdownTable table) {
    for (int row = 0; row < table.getRowCount(); row++) {
      if ("seed".equals(table.originalCell(0, row))) {
        return table.cellRecord(row, 1).actual();
      }
    }
    return "";
  }

  private static int lines(String source) {
    int count = 0;
    for (String line : source.strip().split("\n")) {
      if (line.trim().startsWith("|")) {
        count++;
      }
    }
    return count;
  }

  private static Opened open(String markdown) throws Exception {
    WikiPage page = page(markdown);
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

  private static Ran slim(String markdown) throws Exception {
    WikiPage page = page(markdown);
    Calls calls = new Calls(new InProcessSlimClientBuilder(
        new WikiPageDescriptor(page, true, false), SequenceTableTest.class.getClassLoader()).build());
    HtmlSlimTestSystem system = new HtmlSlimTestSystem("slim", calls, new SlimTableFactory());
    Grab grab = new Grab();
    system.addTestSystemListener(grab);
    system.start();
    try {
      system.runTests(new WikiTestPage(page));
    } finally {
      system.bye();
    }
    int rows = grab.sequence().getRowCount();
    int wrong = grab.summary.getWrong();
    int right = grab.summary.getRight();
    int exceptions = grab.summary.getExceptions();
    assertTrue(SequenceTable.latest.furtherAssertions().isEmpty());
    SequenceTable.latest.finishNow();
    assertEquals(rows, grab.sequence().getRowCount());
    assertEquals(wrong, grab.summary.getWrong());
    assertEquals(right, grab.summary.getRight());
    assertEquals(exceptions, grab.summary.getExceptions());
    return new Ran(page, grab.summary, grab.sequence(), calls.n);
  }

  private static WikiPage page(String markdown) throws Exception {
    WikiPage root = InMemoryPage.makeRoot("RooT");
    return WikiPageUtil.addPage(root, PathParser.parse("Example"), markdown);
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
    @Override public java.util.Map<String, Object> invokeAndGetResponse(List<agnesse.slim.instructions.Instruction> statements) throws SlimCommunicationException {
      n++;
      return inner.invokeAndGetResponse(statements);
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

    private MarkdownTable table() {
      return (MarkdownTable) tables.get(tables.size() - 1).getTable();
    }
  }

  private static final class Grab implements TestSystemListener, TableFinished {
    private TestSummary summary;
    private final List<MarkdownTable> seen = new ArrayList<>();

    @Override public void testOutputChunk(TestPage testPage, String output) { }

    @Override
    public void tableFinished(TestPage page, MarkdownTable table) {
      seen.add(table);
    }

    private MarkdownTable sequence() {
      for (int i = seen.size() - 1; i >= 0; i--) {
        MarkdownTable table = seen.get(i);
        if (table.getRowCount() > 0 && table.originalCell(0, 0).startsWith("sequence")) {
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
    private final int calls;

    private Ran(WikiPage page, TestSummary summary, MarkdownTable table, int calls) {
      this.page = page;
      this.summary = summary;
      this.table = table;
      this.calls = calls;
    }
  }
}
