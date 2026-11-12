package fitnesse.fixtures;

import fitnesse.wiki.WikiPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PageBuilderTest {

  @Before
  public void setUp() throws Exception {
    new SetUp();
  }

  @After
  public void tearDown() throws Exception {
    new TearDown();
  }

  @Test
  public void linesAndAttributesBecomeThePage() throws Exception {
    PageBuilder builder = new PageBuilder();
    builder.line("hello");
    builder.line(null);
    builder.line("\\kept");
    builder.line("a &bar; b &bang; c &dollar;d");
    builder.attributes("Help=a!COMMA!b,Prune=true");
    builder.page("BuiltPage");

    WikiPage page = FitnesseFixtureContext.context.getRootPage().getChildPage("BuiltPage");
    assertEquals("hello\n\nkept\na | b ! c $d\n", page.getData().getContent());
    assertEquals("a,b", page.getData().getAttribute("Help"));
    assertTrue(page.getData().hasAttribute("Prune"));
  }

  @Test
  public void anAttributeWithoutAnEqualsIsRejected() throws Exception {
    PageBuilder builder = new PageBuilder();
    builder.line("x");
    builder.attributes("Help");
    try {
      builder.page("BadPage");
      fail("expected a bad attribute");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("name=value"));
    }
  }
}
