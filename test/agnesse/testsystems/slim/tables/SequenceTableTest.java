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

import agnesse.slim.JavaSlimFactory;
import agnesse.slim.SlimFactory;
import agnesse.slim.SlimServer;
import agnesse.slim.SlimService;
import agnesse.slim.instructions.Instruction;
import agnesse.slim.instructions.LanesInstruction;
import agnesse.socketservice.PlainClientSocketFactory;
import agnesse.testsystems.CompositeExecutionLogListener;
import agnesse.testsystems.MockCommandRunner;
import agnesse.testsystems.slim.SlimCommandRunningClient;
import agnesse.testsystems.slim.property.Seed;
import agnesse.testsystems.slim.sequence.CallBroke;
import agnesse.testsystems.slim.sequence.History;
import agnesse.testsystems.slim.sequence.Lane;
import agnesse.testsystems.slim.sequence.Race;
import agnesse.testsystems.slim.sequence.Schedule;
import agnesse.testsystems.slim.sequence.Trace;
import agnesse.testsystems.slim.sequence.Waves;

import org.junit.Test;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  public void aWaitlistedReserveIsAPassingTrial() throws Exception {
    assertEquals(1, reserve("held").summary.getRight());
    Ran wait = reserve("");
    assertEquals(0, wait.summary.getWrong());
    assertEquals(1, wait.summary.getRight());
  }

  @Test
  public void theShownRowIsTheCandidateThatMatched() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.Waitlist | wait |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | phase=open |
        | command | when | next | result? |
        | reserve | phase=open | phase=held | held |
        | reserve | phase=open | phase=wait | wait |
        | invariant | when | model | query | expect |
        | | phase=wait | phase=held | | kept |
        """);
    assertEquals(1, ran.summary.getWrong());
    assertEquals("wait", failure(ran.table).actual());
    assertEquals("held", failure(ran.table).expected());
    int row = resultRow(ran.table, sourceLines(ran), "reserve");
    assertEquals("phase=wait", ran.table.cellRecord(row, 2).actual());
    assertTrue(trials(ran.table), trials(ran.table).contains("invariant phase"));
  }

  @Test
  public void anUnexplainedResultNamesEveryCandidate() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.Waitlist | nope |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | phase=open |
        | command | when | next | result? |
        | reserve | phase=open | phase=held | held |
        | reserve | phase=open | phase=wait | wait |
        """);
    assertEquals(1, ran.summary.getWrong());
    assertEquals("nope", failure(ran.table).actual());
    assertEquals("held or wait", failure(ran.table).expected());
    assertTrue(trials(ran.table), trials(ran.table).contains("saw nope"));
    assertTrue(trials(ran.table), trials(ran.table).contains("step 1 reserve"));
  }

  @Test
  public void aCommandLegalInOnlyOneCandidateIsNotRun() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.Waitlist | wait |
        | trials | 1 |
        | steps | 2..2 |
        | seed | 2 |
        | state | phase=open |
        | command | when | next | result? |
        | reserve | phase=open | phase=held | held |
        | reserve | phase=open | phase=wait | wait |
        | confirm | phase=held | phase=done | ok |
        | ping | | | ok |
        """);
    assertEquals(0, ran.summary.getWrong());
    assertEquals(1, ran.summary.getRight());
  }

  @Test
  public void sixtyFiveCandidatesIsABadTable() {
    StringBuilder markdown = new StringBuilder("""
        | sequence:agnesse.slim.test.ItemLife |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | n=0 |
        | command | when | next | result? |
        """);
    for (int i = 0; i < 65; i++) {
      markdown.append("| go | | n=").append(i).append(" | ok |\n");
    }
    assertMessage("model too ambiguous", markdown.toString());
  }

  private static Ran reserve(String answer) throws Exception {
    String fixture = answer.isEmpty()
        ? "| sequence:agnesse.slim.test.Waitlist |"
        : "| sequence:agnesse.slim.test.Waitlist | " + answer + " |";
    return slim("""
        %s
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | phase=open |
        | command | when | next | result? | phase? |
        | reserve | phase=open | phase=held | held | held |
        | reserve | phase=open | phase=wait | wait | wait |
        | invariant | when | model | query | expect |
        | | phase=held | phase=held | | kept |
        | | phase=wait | phase=wait | | kept |
        """.formatted(fixture));
  }

  @Test
  public void aOneLetterStateNameIsKept() throws Exception {
    Ran ran = slim("""
        | sequence:agnesse.slim.test.ItemLife |
        | trials | 1 |
        | steps | 1..1 |
        | seed | 1 |
        | state | a=b |
        | command | when | next | result? |
        | open | a=b | a=c | ok |
        """);
    assertEquals(1, ran.summary.getRight());
    assertEquals(0, ran.summary.getWrong());
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
    int release = resultRow(ran.table, source, "release");
    assertEquals("release", ran.table.cellRecord(release, 0).actual());
    assertEquals("phase=blocked", ran.table.cellRecord(release, 1).actual());
    assertEquals("blocked", ran.table.cellRecord(release, 4).actual());
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
    assertEquals("5", ran.table.cellRecord(tweak, 3).actual());
    assertEquals("tweak", ran.table.cellRecord(tweak, 0).actual());
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
    assertEquals("invariant", ran.table.cellRecord(invariant, 0).actual());
    assertEquals("released?", ran.table.cellRecord(invariant, 3).actual());
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
    assertMessage("state must be name=value", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | =open |
        | command | when | next | result? |
        | open | phase=new | phase=open | ok |
        """);
    assertMessage("state must be name=value", """
        | sequence:agnesse.slim.test.ItemLife |
        | state | a=b; |
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
        | steps | 2..4 |
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
    assertMessage("threads must be low..high from 2 through 32", """
        | sequence:agnesse.slim.test.LastSeat |
        | threads | 1..2 |
        | command | when | next | result? |
        | book | seats=1 | seats=0 | booked |
        """);
    assertMessage("threads must be low..high from 2 through 32", """
        | sequence:agnesse.slim.test.LastSeat |
        | threads | many |
        | command | when | next | result? |
        | book | seats=1 | seats=0 | booked |
        """);
    assertMessage("threads is already set", """
        | sequence:agnesse.slim.test.LastSeat |
        | threads | 2..2 |
        | threads | 2..3 |
        | command | when | next | result? |
        | book | seats=1 | seats=0 | booked |
        """);
    assertMessage("prefix must be low..high from 0 through 32", """
        | sequence:agnesse.slim.test.LastSeat |
        | threads | 2..2 |
        | prefix | 1..33 |
        | command | when | next | result? |
        | book | seats=1 | seats=0 | booked |
        """);
    assertMessage("prefix needs a threads row", """
        | sequence:agnesse.slim.test.LastSeat |
        | prefix | 0..0 |
        | command | when | next | result? |
        | book | seats=1 | seats=0 | booked |
        """);
    assertMessage("lanes instruction", """
        | sequence:agnesse.slim.test.LastSeat |
        | threads | 2..2 |
        | steps | 1..1 |
        | seed | 1 |
        | state | seats=1 |
        | command | when | next | result? |
        | book | seats=1 | seats=0 | booked |
        | book | seats=0 | | full |
        """);
  }

  @Test
  public void aPipeClientIsRefusedBeforeItConnects() throws Exception {
    SequenceSheet sheet = sheet(seat("2..2", "1..1"));
    SlimCommandRunningClient pipe = new SlimCommandRunningClient(
        new MockCommandRunner(new CompositeExecutionLogListener()),
        "localhost", 1, 2, SlimCommandRunningClient.MINIMUM_REQUIRED_SLIM_VERSION, new PlainClientSocketFactory());
    SyntaxError error = assertThrows(SyntaxError.class, () -> SlimLanes.open(pipe, sheet));
    assertTrue(error.getMessage(), error.getMessage().contains("lanes instruction"));
  }

  @Test
  public void aServerWithoutLanesIsNamed() {
    assertTrue(SlimLanes.NEED.contains("lanes instruction"));
  }

  @Test
  public void aShrunkRacePaintsTheBranchesAndTheSeed() throws Exception {
    String source = seat("2..2", "2..2");
    MarkdownTable table = (MarkdownTable) new MarkdownTableScanner(source).getTable(0);
    SequenceSheet sheet = SequenceSheet.read(table, "agnesse.slim.test.LastSeat");
    Race.Result result = Race.check(sheet.plan, Race.draw(sheet.plan, new Seed(sheet.seed).random(), sheet.trials,
        sheet.threadsLow, sheet.threadsHigh, sheet.prefixLow, sheet.prefixHigh),
        alwaysBooked(), "agnesse.slim.test.LastSeat", new String[0], Waves.DIRECT);
    SlimTestContextImpl context = new SlimTestContextImpl(new WikiTestPage(page(source)));
    RaceView.paint(table, sheet, result, context);
    assertEquals(1, context.getTestSummary().getWrong());
    assertEquals(0, context.getTestSummary().getRight());
    assertTrue(trials(table), trials(table).contains("no serial order"));
    assertTrue(trials(table), trials(table).contains("shrunk"));
    assertTrue(trials(table), trials(table).contains("t1 [book book]"));
    assertEquals("1", seed(table));
    assertEquals("t1 book", added(table, source, 0, 0));
    assertEquals("booked", added(table, source, 0, 4));
    assertTrue(names(table, source).contains("t1 book"));
    assertTrue(names(table, source).contains("t2 book"));
    assertTrue(names(table, source), painted(table, source).contains("5"));
  }

  @Test
  public void aPassingRacePaintsOnlyTheTrialsCell() throws Exception {
    String source = seat("2..2", "1..1");
    MarkdownTable table = (MarkdownTable) new MarkdownTableScanner(source).getTable(0);
    SequenceSheet sheet = SequenceSheet.read(table, "agnesse.slim.test.LastSeatSafe");
    Race.Result result = Race.check(sheet.plan, Race.draw(sheet.plan, new Seed(sheet.seed).random(), sheet.trials,
        sheet.threadsLow, sheet.threadsHigh, sheet.prefixLow, sheet.prefixHigh),
        bookedThenFull(), "agnesse.slim.test.LastSeatSafe", new String[0], Waves.DIRECT);
    int rows = table.getRowCount();
    SlimTestContextImpl context = new SlimTestContextImpl(new WikiTestPage(page(source)));
    RaceView.paint(table, sheet, result, context);
    assertTrue(result.passed);
    assertEquals(1, context.getTestSummary().getRight());
    assertEquals("1 trials", table.cellRecord(1, 1).actual());
    assertEquals(rows, table.getRowCount());
  }

  @Test
  public void aCappedOrBrokenRaceNamesTheReason() throws Exception {
    String source = seat("2..2", "1..1");
    MarkdownTable table = (MarkdownTable) new MarkdownTableScanner(source).getTable(0);
    SequenceSheet sheet = SequenceSheet.read(table, "agnesse.slim.test.LastSeat");
    Schedule schedule = new Schedule(new Trace(List.of()), List.of(new Trace(List.of()), new Trace(List.of())));
    History history = History.ok(List.of(), List.of(List.of(), List.of()));
    SlimTestContextImpl context = new SlimTestContextImpl(new WikiTestPage(page(source)));
    RaceView.paint(table, sheet, Race.Result.fail(1, 1, schedule, schedule, history, "no serial order; saw ", true, true), context);
    assertTrue(trials(table), trials(table).contains("shrink cap"));
    assertFalse(trials(table), trials(table).contains("shrunk"));
    assertEquals(1, context.getTestSummary().getWrong());

    MarkdownTable broken = (MarkdownTable) new MarkdownTableScanner(source).getTable(0);
    SlimTestContextImpl errors = new SlimTestContextImpl(new WikiTestPage(page(source)));
    RaceView.paint(broken, sheet, Race.Result.fail(1, 1, schedule, schedule, History.broke("boom"), "boom", false, false), errors);
    assertEquals(1, errors.getTestSummary().getExceptions());
    assertTrue(trials(broken), trials(broken).contains("boom"));
  }

  @Test
  public void oneConnectionRunsTwoLanes() throws Exception {
    SlimService.Options options = SlimService.parseCommandLine(new String[]{"0"});
    try (ServerSocket socket = new ServerSocket(0)) {
      int port = socket.getLocalPort();
      SlimFactory factory = JavaSlimFactory.createJavaSlimFactory(options);
      SlimService service = new SlimService(factory.getSlimServer(), socket);
      Thread server = new Thread(() -> {
        try {
          service.accept();
        } catch (Exception ignored) {
          // Closing the socket ends the single accept.
        }
      });
      server.setDaemon(true);
      server.start();
      SlimCommandRunningClient client = new SlimCommandRunningClient(
          new MockCommandRunner(new CompositeExecutionLogListener()),
          "localhost", port, 2, SlimCommandRunningClient.MINIMUM_REQUIRED_SLIM_VERSION, new PlainClientSocketFactory());
      connect(client);
      MarkdownTable markdown = (MarkdownTable) new MarkdownTableScanner(seat("2..2", "1..1")).getTable(0);
      SequenceSheet sheet = SequenceSheet.read(markdown, "agnesse.slim.test.Waitlist");
      SlimDesk desk = SlimLanes.open(client, sheet);
      try {
        desk.make("seat", "agnesse.slim.test.Waitlist", new String[0]);
        java.util.List<agnesse.testsystems.slim.sequence.Lane.Stamp> hits = desk.runLanes(java.util.List.of(
            new agnesse.testsystems.slim.sequence.Lane.Group("t1",
                java.util.List.of(new agnesse.testsystems.slim.sequence.Lane.Call("a", "seat", "ping", java.util.List.of()))),
            new agnesse.testsystems.slim.sequence.Lane.Group("t2",
                java.util.List.of(new agnesse.testsystems.slim.sequence.Lane.Call("b", "seat", "ping", java.util.List.of())))));
        assertEquals(2, hits.size());
        assertEquals("ok", hits.get(0).result());
        assertEquals("ok", hits.get(1).result());
      } finally {
        client.bye();
        client.kill();
        socket.close();
        server.join(5000);
      }
      assertFalse(server.isAlive());
    }
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

  private static String seat(String threads, String steps) {
    return """
        | sequence:agnesse.slim.test.LastSeat |
        | trials | 1 |
        | threads | %s |
        | steps | %s |
        | seed | 1 |
        | state | seats=1 |
        | command | when | next | n | result? |
        | book | seats=1 | seats=0 | int:5..5 | booked |
        | book | seats=0 | | int:5..5 | full |
        """.formatted(threads, steps);
  }

  private static SequenceSheet sheet(String source) throws Exception {
    return SequenceSheet.read((MarkdownTable) new MarkdownTableScanner(source).getTable(0), "agnesse.slim.test.LastSeat");
  }

  private static agnesse.testsystems.slim.sequence.Desk alwaysBooked() {
    return new agnesse.testsystems.slim.sequence.Desk() {
      public void make(String instance, String fixture, String[] arguments) {
      }
      public String call(int lane, String instance, String method, List<String> arguments) {
        return "booked";
      }
    };
  }

  private static agnesse.testsystems.slim.sequence.Desk bookedThenFull() {
    return new agnesse.testsystems.slim.sequence.Desk() {
      private int calls;
      public void make(String instance, String fixture, String[] arguments) {
        calls = 0;
      }
      public String call(int lane, String instance, String method, List<String> arguments) {
        calls++;
        return calls == 1 ? "booked" : "full";
      }
    };
  }

  private static void connect(SlimCommandRunningClient client) {
    long deadline = System.nanoTime() + 5_000_000_000L;
    while (!client.isConnected() && System.nanoTime() < deadline) {
      try {
        client.connect();
      } catch (Exception ignored) {
        Thread.onSpinWait();
      }
    }
    assertTrue("the Slim server did not accept a connection", client.isConnected());
  }

  private static String added(MarkdownTable table, String source, int rowOffset, int column) {
    return table.cellRecord(lines(source) + rowOffset, column).actual();
  }

  private static String painted(MarkdownTable table, String source) {
    StringBuilder out = new StringBuilder();
    for (int row = lines(source); row < table.getRowCount(); row++) {
      for (int column = 0; column < table.getColumnCountInRow(row); column++) {
        out.append(table.originalCell(column, row)).append('\n');
      }
    }
    return out.toString();
  }

  private static String names(MarkdownTable table, String source) {
    StringBuilder out = new StringBuilder();
    for (int row = lines(source); row < table.getRowCount(); row++) {
      out.append(table.originalCell(0, row)).append('\n');
    }
    return out.toString();
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

  @Test
  public void laneStampsKeepTheirTagAndABadMakeIsReported() {
    SlimDesk desk = new SlimDesk(new Scripted(List.of(List.of("t1", "a", "1", "2", "world"))));
    assertTrue(desk.lanes());
    assertTrue(desk.probes());
    List<Lane.Stamp> hits = desk.runLanes(List.of(new Lane.Group("t1", List.of())));
    assertEquals("t1", hits.get(0).tag());
    assertEquals("a", hits.get(0).id());
    assertEquals(1L, hits.get(0).started());
    assertEquals(2L, hits.get(0).finished());
    assertEquals("world", hits.get(0).result());
    assertEquals("world", new SlimDesk(new Scripted("world")).call(0, "seat", "hello", List.of()));
    CallBroke broke = assertThrows(CallBroke.class,
        () -> new SlimDesk(new Scripted("bad")).make("seat", "LastSeat", new String[0]));
    assertEquals("bad", broke.getMessage());
    assertThrows(CallBroke.class, () -> new SlimDesk(new Scripted(null)).make("seat", "LastSeat", new String[0]));
    assertFalse(new SlimDesk(new Scripted("no")).probes());
    assertFalse(new SlimDesk(new Scripted(null)).probes());
    assertThrows(CallBroke.class,
        () -> new SlimDesk(new Scripted(SlimServer.EXCEPTION_TAG + "down")).call(0, "seat", "hello", List.of()));
    assertThrows(CallBroke.class, () -> new SlimDesk(new Scripted(null)).call(0, "seat", "hello", List.of()));
    CallBroke missing = assertThrows(CallBroke.class, () -> new SlimDesk(new Scripted(null)).runLanes(List.of()));
    assertEquals("no result", missing.getMessage());
    CallBroke text = assertThrows(CallBroke.class, () -> new SlimDesk(new Scripted("nope")).runLanes(List.of()));
    assertEquals("nope", text.getMessage());
    assertThrows(CallBroke.class, () -> new SlimDesk(new Scripted(List.of("short"))).runLanes(List.of()));
  }

  private static final class Scripted implements SlimClient {
    private final Object reply;

    private Scripted(Object reply) {
      this.reply = reply;
    }

    @Override
    public Map<String, Object> invokeAndGetResponse(List<Instruction> statements) {
      Instruction instruction = statements.get(0);
      Map<String, Object> result = new HashMap<>();
      if (instruction instanceof LanesInstruction lanes && lanes.statement().size() <= 2) {
        result.put(instruction.getId(), reply instanceof List ? "lanes" : reply);
        return result;
      }
      result.put(instruction.getId(), reply);
      return result;
    }

    @Override public void start() { }
    @Override public void connect() { }
    @Override public void bye() { }
    @Override public void kill() { }
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
