package agnesse.slim.test;

import agnesse.slim.SystemUnderTest;

public class MySystemUnderTestDriver {

  @SystemUnderTest
  public MySystemUnderTest systemUnderTest = new MySystemUnderTest();

  private boolean called;

  public void foo() {
    called = true;
  }

  public boolean driverCalled() {
    return called;
  }
}
