package agnesse.markdown;

import org.commonmark.node.Link;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageLinkRendererTest {
  @Test
  public void aLinkWithoutAPageLookupKeepsItsDestination() {
    StringWriter out = new StringWriter();
    HtmlNodeRendererContext context = mock(HtmlNodeRendererContext.class);
    when(context.getWriter()).thenReturn(new HtmlWriter(out));
    PageLinkRenderer renderer = new PageLinkRenderer(context, null);
    Link link = new Link("PageName", null);

    renderer.render(link);

    assertTrue(out.toString().contains("href=\"PageName\""));
  }

  @Test
  public void aLinkWithoutADestinationHasAnEmptyHref() {
    StringWriter out = new StringWriter();
    HtmlNodeRendererContext context = mock(HtmlNodeRendererContext.class);
    when(context.getWriter()).thenReturn(new HtmlWriter(out));
    PageLinkRenderer renderer = new PageLinkRenderer(context, null);
    Link link = new Link(null, null);

    renderer.render(link);

    assertTrue(out.toString().contains("href=\"\""));
  }
}
