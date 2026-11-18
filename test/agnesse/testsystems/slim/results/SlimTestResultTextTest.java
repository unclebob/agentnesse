package agnesse.testsystems.slim.results;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SlimTestResultTextTest {
  @Test
  public void aPassWithNothingElseNamesTheResult() {
    assertEquals("pass()", SlimTestResult.pass().toString());
    assertEquals("pass(kept)", SlimTestResult.pass().toString("kept"));
  }

  @Test
  public void valuesAreLabeledInOrder() {
    assertEquals("fail(a=2;e=1;nope)", SlimTestResult.fail("2", "1", "nope").toString());
    assertEquals("fail(a=2;nope)", SlimTestResult.fail("2", null, "nope").toString());
    assertEquals("fail(e=1;nope)", new SlimTestResult(null, "1", "nope", agnesse.testsystems.ExecutionResult.FAIL).toString());
    assertEquals("pass(a=2;e=1)", SlimTestResult.pass("2", "1").toString());
  }

  @Test
  public void anUnrunTestCountsOnlyWhenAsked() {
    assertFalse(SlimTestResult.testNotRun().doesCount());
    assertTrue(SlimTestResult.countTestNotRun().doesCount());
    assertEquals("ignore(Test not run)", SlimTestResult.countTestNotRun().toString());
  }

  @Test
  public void negationSwapsPassAndFailOnly() {
    SlimTestResult pass = (SlimTestResult) SlimTestResult.pass("2", "1");

    assertEquals("fail(a=2;e=1)", pass.negateTestResult().toString());
    assertEquals("pass(a=2;e=1)", SlimTestResult.fail("2", "1").negateTestResult().toString());
    assertEquals("ignore()", SlimTestResult.ignore().negateTestResult().toString());
  }

  @Test
  public void anActualWithoutAnExpectedDoesNotKeepTheOriginalText() {
    assertEquals("fail(a=2)", SlimTestResult.fail("2", null).toString("orig"));
  }

  @Test
  public void anOkResultPassesWithoutCounting() {
    assertFalse(SlimTestResult.ok("fine").doesCount());
    assertEquals("pass(fine)", SlimTestResult.ok("fine").toString());
  }

  @Test
  public void aPlainResultHasNoParentheses() {
    assertEquals("just text", SlimTestResult.plain().toString("just text"));
    assertEquals("note", SlimTestResult.plain("note").toString("ignored"));
  }
}
