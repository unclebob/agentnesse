package fitnesse;

import fitnesse.util.ClassUtils;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageFactory;
import fitnesse.wiki.VariableSource;
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
    fitnesse.http.MockRequest request = new fitnesse.http.MockRequest();
    request.parseRequestUri("/fitnesse/FrontPage");

    assertThat(request.getResource(), is("fitnesse/FrontPage"));
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
  public void createsALoggerWhenALogDirectoryIsConfigured() throws IOException {
    FitNesseContext context = ContextConfigurator.empty()
      .withParameter(ConfigurationParameter.LOG_DIRECTORY, "build/test-logs")
      .withPort(9126)
      .makeFitNesseContext();

    assertThat(context.logger, is(notNullValue()));
  }

  @Test
  public void bootstrapIsDefaultTheme() throws IOException {
    String theme = ContextConfigurator
      .empty()
      .makeFitNesseContext()
      .theme;

    assertThat(theme, is("bootstrap"));
  }

  @Test
  public void themeCanBeSetViaProperties() throws IOException {
    String theme = ContextConfigurator
      .empty()
      .withParameter(ConfigurationParameter.THEME, "othertheme")
      .makeFitNesseContext()
      .theme;

    assertThat(theme, is("othertheme"));
  }

}
