package agnesse.testutil;

public class ErrorWritingFixture {
  public void write(String value) {
    System.err.println(value);
  }
}
