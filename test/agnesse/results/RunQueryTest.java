package agnesse.results;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RunQueryTest {

  @Test
  public void aCommandWithoutAQueryIsLeftAlone() {
    assertEquals("Page", RunQuery.json("Page", null));
  }

  @Test
  public void jsonFormatIsAddedWhenTheCommandHasAQuery() {
    assertEquals("Page?test&format=json", RunQuery.json("Page?test", null));
  }

  @Test
  public void anExistingFormatIsKept() {
    assertEquals("Page?test&format=xml", RunQuery.json("Page?test&format=xml", null));
  }

  @Test
  public void rerunFailuresAreEncodedOntoTheCommand() {
    assertEquals(
      "Suite?suite&format=json&rerunFailures=last+failures.json",
      RunQuery.json("Suite?suite", "last failures.json"));
    assertEquals(
      "Suite?suite&format=json&rerunFailures=a%3Ab",
      RunQuery.json("Suite?suite", "a:b"));
  }

  @Test
  public void rerunFailuresAreAppendedEvenWhenThereIsNoQuery() {
    assertEquals("Page&rerunFailures=a%3Ab", RunQuery.json("Page", "a:b"));
  }
}
