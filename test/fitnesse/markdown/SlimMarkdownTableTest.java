package fitnesse.markdown;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SlimMarkdownTableTest {

  @Test
  public void theHeaderIsFitNesseRowOneAndTheFirstBodyRowIsRowTwo() {
    SlimMarkdownTable table = SlimMarkdownTable.parse("""
      | decision:Division | | |
      | --- | --- | --- |
      | numerator | denominator | quotient? |
      | 10 | 2 | 5.0 |
      | 5 | 2 | 2.5 |
      """);

    assertEquals("decision", table.type());
    assertEquals("Division", table.name());
    assertNull(table.error());
    assertEquals(List.of("decision:Division", "", ""), table.row(1));
    assertEquals(List.of("", ""), table.constructorArguments());
    assertEquals(List.of("numerator", "denominator", "quotient?"), table.row(2));
    assertEquals(List.of("10", "2", "5.0"), table.row(3));
    assertEquals(List.of("5", "2", "2.5"), table.row(4));
  }

  @Test
  public void aScriptHeaderHoldsConstructorArgumentsAndTheBodyStartsAtTheFirstCommand() {
    SlimMarkdownTable table = SlimMarkdownTable.parse("""
      | script:Login | bob |
      | --- | --- |
      | login | |
      """);

    assertEquals("script", table.type());
    assertEquals("Login", table.name());
    assertEquals(List.of("bob"), table.constructorArguments());
    assertEquals(List.of("login", ""), table.row(2));
  }

  @Test
  public void aScenarioHeaderIsTheNameAndTheBodyIsTheFirstStep() {
    SlimMarkdownTable table = SlimMarkdownTable.parse("""
      | scenario | login as | user |
      | --- | --- | --- |
      | enter | @user | |
      | login | | |
      """);

    assertEquals("scenario", table.type());
    assertEquals("", table.name());
    assertEquals(List.of("scenario", "login as", "user"), table.row(1));
    assertEquals(List.of("enter", "@user", ""), table.row(2));
  }
}
