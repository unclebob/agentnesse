package agnesse;

import agnesse.util.ClassUtils;
import agnesse.wiki.WikiPage;
import agnesse.wiki.WikiPageFactory;
import agnesse.wiki.VariableSource;
import agnesse.wiki.fs.WikiFiles;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ContextConfiguratorTest {
  private final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

  @After
  public void tearDown() {
    ClassUtils.setClassLoader(null);
    Thread.currentThread().setContextClassLoader(originalClassLoader);
  }

  @Test
  public void warnsWhenWikiPageFactoryCannotRegisterFactories() throws IOException {
    WikiPageFactory factory = new WikiPageFactory() {
      @Override
      public WikiPage makePage(File path, String pageName, WikiPage parent, VariableSource variableSource) {
        return null;
      }
    };

    AgNesseContext context = ContextConfigurator.empty()
      .withWikiPageFactory(factory)
      .withParameter(ConfigurationParameter.ROOT_PATH, ".")
      .makeAgNesseContext();

    assertThat(context, is(instanceOf(AgNesseContext.class)));
  }

  @Test
  public void recordsRootPathAsAWikiVariable() throws IOException {
    AgNesseContext context = ContextConfigurator.empty()
      .withRootPath("/tmp/wiki-root")
      .withPort(9125)
      .makeAgNesseContext();

    assertThat(context.variableSource.getProperty("AGNESSE_ROOTPATH"), is("/tmp/wiki-root"));
  }

  @Test
  public void omitsRootPathVariableWhenRootPathIsNull() throws IOException {
    AgNesseContext context = ContextConfigurator.empty()
      .withRootPath(null)
      .withPort(9125)
      .makeAgNesseContext();

    assertNull(context.variableSource.getProperty("AGNESSE_ROOTPATH"));
  }

  @Test
  public void usesThePortItWasGiven() throws IOException {
    AgNesseContext context = ContextConfigurator.empty()
      .withPort(9124)
      .withParameter(ConfigurationParameter.ROOT_PATH, ".")
      .makeAgNesseContext();

    assertThat(context.port, is(9124));
  }

  @Test
  public void aFormerMountPathStaysPartOfThePageName() throws Exception {
    agnesse.http.MockRequest request = new agnesse.http.MockRequest();
    request.parseRequestUri("/agnesse/FrontPage");

    assertThat(request.getResource(), is("agnesse/FrontPage"));
  }

  @Test
  public void maximumWorkersCanBeSetFromProperties() throws IOException {
    AgNesseContext context = ContextConfigurator.empty()
      .withParameter(ConfigurationParameter.MAXIMUM_WORKERS, "12")
      .withPort(9127)
      .makeAgNesseContext();

    assertEquals(12, (int) context.maximumWorkers);
  }

  @Test
  public void anUnsetVersionsControllerIsCreated() throws IOException {
    AgNesseContext context = ContextConfigurator.empty().makeAgNesseContext();
    assertThat(context.versionsController, is(instanceOf(WikiFiles.class)));
  }

  @Test
  public void anUnknownParameterIsStoredForLookup() throws IOException {
    AgNesseContext context = ContextConfigurator.empty()
      .withParameter("not-a-config-key", "kept")
      .makeAgNesseContext();
    assertEquals("kept", context.variableSource.findVariable("not-a-config-key").orElse(null));
  }

  @Test
  public void aCommandWithoutAPortUsesTheCommandPort() throws IOException {
    AgNesseContext context = ContextConfigurator.empty()
      .withParameter(ConfigurationParameter.COMMAND, "FrontPage")
      .makeAgNesseContext();
    assertEquals(9123, context.port);
    assertThat(context.getRootPage(), notNullValue());
    ContextConfigurator configurator = ContextConfigurator.empty();
    configurator.makeAgNesseContext();
    assertEquals("127.0.0.1", configurator.get(ConfigurationParameter.LISTEN));
  }

}
