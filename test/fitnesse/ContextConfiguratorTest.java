package fitnesse;

import fitnesse.http.MockRequest;
import fitnesse.plugins.PluginException;
import fitnesse.plugins.PluginsLoaderTest;
import fitnesse.responders.FilteringResponder;
import fitnesse.util.ClassUtils;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageFactory;
import fitnesse.wikitext.VariableSource;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
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
  public void warnsWhenWikiPageFactoryCannotRegisterFactories() throws IOException, PluginException {
    WikiPageFactory factory = new WikiPageFactory() {
      @Override
      public WikiPage makePage(File path, String pageName, WikiPage parent, VariableSource variableSource) {
        return null;
      }

      @Override
      public boolean supports(File path) {
        return false;
      }
    };

    FitNesseContext context = ContextConfigurator.empty()
      .withWikiPageFactory(factory)
      .withParameter(ConfigurationParameter.ROOT_PATH, ".")
      .makeFitNesseContext();

    assertThat(context, is(instanceOf(FitNesseContext.class)));
  }

  @Test
  public void recordsRootPathAsAWikiVariable() throws IOException, PluginException {
    FitNesseContext context = ContextConfigurator.empty()
      .withRootPath("/tmp/wiki-root")
      .withPort(9125)
      .makeFitNesseContext();

    assertThat(context.variableSource.getProperty("FITNESSE_ROOTPATH"), is("/tmp/wiki-root"));
  }

  @Test
  public void omitsRootPathVariableWhenRootPathIsNull() throws IOException, PluginException {
    FitNesseContext context = ContextConfigurator.empty()
      .withRootPath(null)
      .withPort(9125)
      .makeFitNesseContext();

    assertNull(context.variableSource.getProperty("FITNESSE_ROOTPATH"));
  }

  @Test
  public void usesThePortItWasGiven() throws IOException, PluginException {
    FitNesseContext context = ContextConfigurator.empty()
      .withPort(9124)
      .withParameter(ConfigurationParameter.ROOT_PATH, ".")
      .makeFitNesseContext();

    assertThat(context.port, is(9124));
  }

  @Test
  public void canSetContextRoot() throws IOException, PluginException {
    ContextConfigurator configurator = ContextConfigurator.empty();

    configurator.withParameter(ConfigurationParameter.ROOT_PATH, ".");
    configurator.withParameter(ConfigurationParameter.CONTEXT_ROOT, "fitnesse/");

    String contextRoot = configurator.makeFitNesseContext().contextRoot;

    assertThat(contextRoot, is("/fitnesse/"));
  }

  @Test
  public void shouldSetTrailingSlashOnContextRoot() throws IOException, PluginException {
    ContextConfigurator configurator = ContextConfigurator.empty();

    configurator.withParameter(ConfigurationParameter.ROOT_PATH, ".");
    configurator.withParameter(ConfigurationParameter.CONTEXT_ROOT, "fitnesse");

    String contextRoot = configurator.makeFitNesseContext().contextRoot;

    assertThat(contextRoot, is("/fitnesse/"));
  }

  @Test
  public void bootstrapIsDefaultTheme() throws IOException, PluginException {
    String theme = ContextConfigurator
      .empty()
      .makeFitNesseContext()
      .theme;

    assertThat(theme, is("bootstrap"));
  }

  @Test
  public void themeCanBeSetViaProperties() throws IOException, PluginException {
    String theme = ContextConfigurator
      .empty()
      .withParameter(ConfigurationParameter.THEME, "othertheme")
      .makeFitNesseContext()
      .theme;

    assertThat(theme, is("othertheme"));
  }

  @Test
  public void themeCanBeSetByPlugin() throws IOException, PluginException {
    String theme = ContextConfigurator
      .empty()
      .withClassLoader(PluginsLoaderTest.createClassLoaderWithTestPlugin())
      .makeFitNesseContext()
      .theme;

    assertThat(theme, is("dummy-theme"));
  }

  @Test
  public void themeInPropertiesBeatsOneInPlugin() throws IOException, PluginException {
    String theme = ContextConfigurator
      .empty()
      .withParameter(ConfigurationParameter.THEME, "othertheme")
      .withClassLoader(PluginsLoaderTest.createClassLoaderWithTestPlugin())
      .makeFitNesseContext()
      .theme;

    assertThat(theme, is("othertheme"));
  }

  @Test
  public void contentFilterIsAppliedWhenSavingPages() throws Exception {
    FitNesseContext context = ContextConfigurator
      .empty()
      .withParameter(ConfigurationParameter.CONTENT_FILTER, PluginsLoaderTest.TestContentFilter.class.getName())
      .withClassLoader(getClass().getClassLoader())
      .makeFitNesseContext();

    MockRequest request = new MockRequest();
    request.addInput("responder", "saveData");

    assertThat(context.responderFactory.makeResponder(request), instanceOf(FilteringResponder.class));
  }

}
