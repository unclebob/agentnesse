package agnesse.slim.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SequenceFixturesTest {
  @Test
  public void anItemMovesOnlyThroughTheOpenTransitions() {
    ItemLife item = new ItemLife();
    assertEquals("new", item.phase());
    assertEquals("false", item.released());
    assertEquals("ok", item.ping());
    assertEquals("refused", item.block());
    assertEquals("refused", item.release());
    assertEquals("new", item.phase());

    assertEquals("ok", item.open());
    assertEquals("open", item.phase());
    assertEquals("refused", item.open());
    assertEquals("open", item.phase());
    assertEquals("ok", item.block());
    assertEquals("blocked", item.phase());
    assertEquals("refused", item.block());
    assertEquals("refused", item.release());
    assertEquals("blocked", item.phase());
    assertEquals("false", item.released());
    assertEquals("ok", item.ping());

    ItemLife released = new ItemLife();
    released.open();
    assertEquals("ok", released.release());
    assertEquals("released", released.phase());
    assertEquals("true", released.released());
    assertEquals("refused", released.release());
  }

  @Test
  public void aLeakReleasesFromAnyPhase() {
    ItemLifeLeak blocked = new ItemLifeLeak();
    blocked.open();
    blocked.block();
    assertEquals("ok", blocked.release());
    assertEquals("released", blocked.phase());
    assertEquals("true", blocked.released());

    ItemLifeLeak fresh = new ItemLifeLeak();
    assertEquals("ok", fresh.release());
    assertEquals("released", fresh.phase());
    assertEquals("true", new ItemLifeQueryLeak().released());
  }

  @Test
  public void aRecordChangesOnlyThroughAnAuditedAdjustmentOnceLocked() {
    AuditedRecord open = new AuditedRecord();
    assertEquals("open", open.phase());
    assertEquals("0", open.value());
    assertFalse(open.audited());
    assertEquals("refused", open.adjust("1", "AUD-01"));
    assertEquals("ok", open.set("4"));
    assertEquals("4", open.value());
    assertEquals("ok", open.lock());
    assertEquals("locked", open.phase());
    assertEquals("refused", open.lock());
    assertEquals("refused", open.set("9"));
    assertEquals("4", open.value());
    assertEquals("refused", open.adjust("1", " "));
    assertEquals("refused", open.adjust("1", null));
    assertEquals("refused", open.tweak("3"));
    assertEquals("4", open.value());
    assertFalse(open.audited());
    assertEquals("ok", open.adjust("2", "AUD-10"));
    assertEquals("6", open.value());
    assertTrue(open.audited());
  }

  @Test
  public void aLeakAdjustsWithoutAnAudit() {
    AuditedLeak leak = new AuditedLeak();
    assertEquals("ok", leak.lock());
    assertEquals("ok", leak.tweak("3"));
    assertEquals("3", leak.value());
    assertFalse(leak.audited());
  }

  @Test
  public void countersStartFreshAndCanStartFromAConstructorArgument() {
    SequenceCounter fresh = new SequenceCounter();
    assertEquals(0, fresh.count());
    fresh.bump();
    fresh.bump();
    assertEquals(2, fresh.count());
    SequenceCounter started = new SequenceCounter("4");
    assertEquals(4, started.count());
    started.bump();
    assertEquals(5, started.count());
  }

  @Test
  public void theShrinkCapNoteAndTheAbortingCommandsDoWhatTheySay() {
    SequenceNote note = new SequenceNote();
    assertEquals("yes", note.note("y".repeat(47)));
    assertEquals("no", note.note("n".repeat(48)));
    assertEquals("no", new SequenceStopOnOne().take("2"));
    assertThrows(SequenceStopOnOne.StopTestOnOne.class, () -> new SequenceStopOnOne().take("1"));
    assertThrows(SequenceHalt.HaltStopTest.class, () -> new SequenceHalt().halt());
    assertThrows(SequenceHalt.HaltStopSuite.class, () -> new SequenceHalt().suite());
    assertThrows(IllegalStateException.class, () -> new SequenceBoom().boom());
  }
}
