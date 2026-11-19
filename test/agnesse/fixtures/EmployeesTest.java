package agnesse.fixtures;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class EmployeesTest {
  @Test
  public void queryReturnsBobHiredOn10Oct1974() {
    assertEquals(
      List.of(List.of(List.of("name", "Bob"), List.of("hired", "10-Oct-1974"))),
      new Employees().query());
  }
}
