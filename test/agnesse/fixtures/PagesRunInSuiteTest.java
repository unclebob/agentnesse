package agnesse.fixtures;

import agnesse.http.MockResponseSender;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PagesRunInSuiteTest {
  @Test
  public void jsonEventsAreGroupedByTestSystem() throws Exception {
    String content = ""
      + "{\"event\":\"page-started\",\"path\":\"SuiteParent.SuiteChildOne.SuiteSetUp\",\"system\":\"slim:A:agnesse.slim.SlimService\"}\n"
      + "{\"event\":\"page-started\",\"path\":\"Other.Page\",\"system\":\"slim:A:agnesse.slim.SlimService\"}\n"
      + "{\"event\":\"page-started\",\"path\":\"SuiteParent.SuiteChildOne.TestOneThree\",\"system\":\"B\"}\n"
      + "{\"event\":\"page-started\",\"path\":\"SuiteParent.SuiteChildOne.SuiteTearDown\",\"system\":\"slim:B\"}\n";
    assertEquals(List.of("SuiteChildOne.SuiteSetUp", "SuiteChildOne"), names(content + "{\"event\":\"page-started\",\"path\":\"SuiteChildOne\",\"system\":\"A\"}\n", "SuiteChildOne", "A"));
    assertEquals(List.of("SuiteChildOne.TestOneThree", "SuiteChildOne.SuiteTearDown"),
      names(content, "SuiteChildOne", "B"));
  }

  @Test
  public void aBrokenEventAddsNoPage() throws Exception {
    assertEquals(List.of(), names("\"event\":\"page-started\" without a brace", "Suite", "A"));
    assertEquals(List.of(), names("{\"event\":\"page-started\",\"system\":\"A}", "Suite", "A"));
    assertEquals(List.of(), names("{\"event\":\"page-started\",\"path\":\"Suite.Open\"", "Suite", "A"));
  }

  @Test
  public void anInterruptedRunIsRejected() throws Exception {
    send("Testing was interrupted and results are incomplete.");
    try {
      new PagesRunInSuite("Suite", "A").query();
      fail("interrupted run should be rejected");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("interrupted"));
    }
  }

  @Test
  public void theLegacyHtmlLogStillListsNamedPages() throws Exception {
    String content = "slim:SuiteChild preamble <a class=\"test_name\">SuiteChild.One</a>";
    assertEquals(List.of("SuiteChild.One"), names(content, "Suite", "S"));
  }

  private static List<String> names(String content, String prefix, String system) throws Exception {
    send(content);
    List<Object> rows = new PagesRunInSuite(prefix, system).query();
    return rows.stream().map(row -> {
      @SuppressWarnings("unchecked")
      List<Object> columns = (List<Object>) row;
      @SuppressWarnings("unchecked")
      List<String> cell = (List<String>) columns.get(0);
      return cell.get(1);
    }).toList();
  }

  private static void send(String content) throws Exception {
    FitnesseFixtureContext.sender = new MockResponseSender();
    FitnesseFixtureContext.sender.send(content.getBytes());
  }
}
