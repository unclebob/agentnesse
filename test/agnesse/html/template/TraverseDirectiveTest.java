package agnesse.html.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import agnesse.wiki.*;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import agnesse.AgNesseContext;
import agnesse.components.TraversalListener;
import agnesse.components.Traverser;
import agnesse.testutil.AgNesseUtil;
import agnesse.wiki.fs.InMemoryPage;

public class TraverseDirectiveTest {

  private AgNesseContext context;
  
  @Before
  public void setUp() {
    context = AgNesseUtil.makeTestContext();
    context.pageFactory.getVelocityEngine().loadDirective(TraverseDirective.class.getName());
  }
  
  @Test
  public void testRender() {
    
    VelocityContext velocityContext = new VelocityContext();
    
    velocityContext.put("resultResponder", new MockTraverser());
    
    String tmpl = context.pageFactory.render(velocityContext, "searchResults.vm");
    
    assertTrue(tmpl.contains("<a href=\"PageOne\">PageOne</a>"));
  }

  @Test
  public void anOutputFailureWhileTraversingFailsTheRender() throws Exception {
    Node node = mock(Node.class);
    when(node.render(any(), any())).thenThrow(new IOException("disk"));
    InternalContextAdapter adapter = mock(InternalContextAdapter.class);
    TraverseDirectiveTraverser traverser = new TraverseDirectiveTraverser(adapter, new StringWriter(), node);
    try {
      traverser.process("a page");
      fail("expected the write failure to escape");
    } catch (RuntimeException e) {
      assertTrue(e.getCause() instanceof IOException);
      assertEquals("disk", e.getCause().getMessage());
    }
    verify(adapter).put("result", "a page");
  }

  public static class MockTraverser implements Traverser<WikiPage> {

    @Override
    public void traverse(TraversalListener<WikiPage> observer) {
      WikiPage root = InMemoryPage.makeRoot("root");
      observer.process(WikiPageUtil.addPage(root, PathParser.parse("PageOne"), "PageOne"));
      observer.process(WikiPageUtil.addPage(root, PathParser.parse("PageTwo"), "PageOne"));
      observer.process(WikiPageUtil.addPage(root, PathParser.parse("ChildPage"), ".PageOne"));
    }
  }
}
