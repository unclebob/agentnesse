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

    FitNesseContext context = ContextConfigurator.empty()
      .withWikiPageFactory(factory)
      .withParameter(ConfigurationParameter.ROOT_PATH, ".")
      .makeFitNesseContext();

    assertThat(context, is(instanceOf(FitNesseContext.class)));
  }

  @Test
  public void recordsRootPathAsAWikiVariable() throws IOException {
    FitNesseContext context = ContextConfigurator.empty()
      .withRootPath("/tmp/wiki-root")
      .withPort(9125)
      .makeFitNesseContext();

    assertThat(context.variableSource.getProperty("FITNESSE_ROOTPATH"), is("/tmp/wiki-root"));
  }

  @Test
  public void omitsRootPathVariableWhenRootPathIsNull() throws IOException {
    FitNesseContext context = ContextConfigurator.empty()
      .withRootPath(null)
      .withPort(9125)
      .makeFitNesseContext();

    assertNull(context.variableSource.getProperty("FITNESSE_ROOTPATH"));
  }

  @Test
  public void usesThePortItWasGiven() throws IOException {
    FitNesseContext context = ContextConfigurator.empty()
      .withPort(9124)
      .withParameter(ConfigurationParameter.ROOT_PATH, ".")
      .makeFitNesseContext();

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
    FitNesseContext context = ContextConfigurator.empty()
      .withParameter(ConfigurationParameter.MAXIMUM_WORKERS, "12")
      .withPort(9127)
      .makeFitNesseContext();

    assertEquals(12, (int) context.maximumWorkers);
  }

  @Test
  public void anUnsetVersionsControllerIsCreated() throws IOException {
    FitNesseContext context = ContextConfigurator.empty().makeFitNesseContext();
    assertThat(context.versionsController, is(instanceOf(WikiFiles.class)));
  }

  @Test
  public void anUnknownParameterIsStoredForLookup() throws IOException {
    FitNesseContext context = ContextConfigurator.empty()
      .withParameter("not-a-config-key", "kept")
      .makeFitNesseContext();
    assertEquals("kept", context.variableSource.findVariable("not-a-config-key").orElse(null));
  }

  @Test
  public void aCommandWithoutAPortUsesTheCommandPort() throws IOException {
    FitNesseContext context = ContextConfigurator.empty()
      .withParameter(ConfigurationParameter.COMMAND, "FrontPage")
      .makeFitNesseContext();
    assertEquals(9123, context.port);
    assertThat(context.getRootPage(), notNullValue());
    ContextConfigurator configurator = ContextConfigurator.empty();
    configurator.makeFitNesseContext();
    assertEquals("127.0.0.1", configurator.get(ConfigurationParameter.LISTEN));
  }

}
