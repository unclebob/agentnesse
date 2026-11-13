package fitnesse.testsystems.slim.results;

import org.junit.Test;

import static fitnesse.slim.SlimServer.EXCEPTION_TAG;
import static org.junit.Assert.assertEquals;

public class SlimFailureTest {

  @Test
  public void aMissingClassIsAMissingFixture() {
    SlimFailure failure = SlimFailure.parse(EXCEPTION_TAG + "message:<<NO_CLASS fitnesse.missing.Fixture>>");
    failure.at("Checkout", "decision:pay", 2, 0);
    assertEquals(SlimFailure.Kind.MISSING_FIXTURE, failure.kind());
    assertEquals("NO_CLASS", failure.type());
    assertEquals("fitnesse.missing.Fixture", failure.message());
    assertEquals("Checkout", failure.page());
    assertEquals(Integer.valueOf(2), failure.row());
  }

  @Test
  public void aMissingMethodIsItsOwnKind() {
    SlimFailure failure = SlimFailure.parse(EXCEPTION_TAG + "message:<<NO_METHOD_IN_CLASS pay[1] fitnesse.slim.test.AddUpChange>>");
    assertEquals(SlimFailure.Kind.MISSING_METHOD, failure.kind());
    assertEquals("pay[1] fitnesse.slim.test.AddUpChange", failure.message());
  }

  @Test
  public void aThrownExceptionKeepsItsStack() {
    SlimFailure failure = SlimFailure.parse(EXCEPTION_TAG + "message:<<java.lang.IllegalStateException: boom\n\tat fitnesse.Pay.bill(Pay.java:4)>>");
    assertEquals(SlimFailure.Kind.EXCEPTION, failure.kind());
    assertEquals("java.lang.IllegalStateException", failure.type());
    assertEquals("boom", failure.message());
    assertEquals("at fitnesse.Pay.bill(Pay.java:4)", failure.stack());
  }

  @Test
  public void aServerThatCannotStartIsALaunchFailure() {
    SlimFailure failure = SlimFailure.parse(EXCEPTION_TAG + "message:<<java.net.BindException: Address already in use>>");
    assertEquals(SlimFailure.Kind.LAUNCH_FAILURE, failure.kind());
    assertEquals("java.net.BindException", failure.type());
    assertEquals("Address already in use", failure.message());
  }
}
