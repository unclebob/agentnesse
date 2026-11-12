package fitnesse;

import fitnesse.components.Logger;
import fitnesse.reporting.FormatterFactory;
import fitnesse.testrunner.MultipleTestSystemFactory;
import fitnesse.testsystems.TestSystemListener;
import fitnesse.testsystems.slim.tables.SlimTableFactory;
import fitnesse.util.ClassUtils;
import fitnesse.wiki.SystemVariableSource;
import fitnesse.wiki.WikiPageFactory;
import fitnesse.wiki.fs.FileSystemPageFactory;
import fitnesse.wiki.fs.WikiFiles;
import java.io.IOException;
import java.util.Properties;

import static fitnesse.ConfigurationParameter.COMMAND;
import static fitnesse.ConfigurationParameter.CONFIG_FILE;
import static fitnesse.ConfigurationParameter.LISTEN;
import static fitnesse.ConfigurationParameter.LOG_DIRECTORY;
import static fitnesse.ConfigurationParameter.ROOT_DIRECTORY;
import static fitnesse.ConfigurationParameter.THEME;
/**
 * Set up a context for running a FitNesse Instance.
 *
 * Please call this only once: some features are registered on (static) factories.
 */
public class ContextConfigurator {
  private static final String DEFAULT_PATH = ".";
  public static final String DEFAULT_ROOT = "FitNesseRoot";
  private static final int DEFAULT_COMMAND_PORT = 9123;
  public static final int DEFAULT_PORT = 80;
  public static final String DEFAULT_CONFIG_FILE = "agentnesse.json";
  public static final String DEFAULT_THEME = "bootstrap";
  public static final String DEFAULT_LISTEN = "127.0.0.1";
  public static final int DEFAULT_MAXIMUM_WORKERS = 100;

  /** Some properties are stored in typed fields: */
  private WikiPageFactory wikiPageFactory;
  private Integer port;
  private String rootPath = DEFAULT_PATH;
  private String rootDirectoryName = DEFAULT_ROOT;
  private Integer maximumWorkers = DEFAULT_MAXIMUM_WORKERS;
  private String listenAddress;
  private Logger logger;
  private WikiFiles versionsController;
  /** Others as name-value pairs: */
  private final Properties properties = new Properties();
  private TestSystemListener testSystemListener;
  private ClassLoader classLoader;

  private ContextConfigurator() {
  }

  public static ContextConfigurator empty() {
    return new ContextConfigurator();
  }

  public static ContextConfigurator systemDefaults() {
    return empty()
      .withRootPath(DEFAULT_PATH)
      .withClassLoader(ClassUtils.getClassLoader())
      .withParameter(ROOT_DIRECTORY, DEFAULT_ROOT)
      .withParameter(CONFIG_FILE, DEFAULT_CONFIG_FILE);
  }

  public ContextConfigurator updatedWith(Properties newProperties) {
    for (String key : newProperties.stringPropertyNames()) {
      withParameter(key, newProperties.getProperty(key));
    }
    return this;
  }

  public ContextConfigurator withTestSystemListener(TestSystemListener testSystemListener) {
    this.testSystemListener = testSystemListener;
    return this;
  }

  public FitNesseContext makeFitNesseContext() throws IOException {

    // BIG WARNING: We're setting a static variable here!
    ClassUtils.setClassLoader(classLoader);
    Thread.currentThread().setContextClassLoader(classLoader);

    port = getPort();

    FitNesseVersion version = new FitNesseVersion();

    updateFitNesseProperties(version);

    if (wikiPageFactory == null) {
      wikiPageFactory = new FileSystemPageFactory();
    }

    if (versionsController == null) {
      versionsController = new WikiFiles();
    }

    if (get(LOG_DIRECTORY) != null) {
      logger = new Logger(get(LOG_DIRECTORY));
    }

    SystemVariableSource variableSource = new SystemVariableSource(properties);

    String theme = variableSource.getProperty(THEME.getKey());
    if (theme == null) {
      theme = DEFAULT_THEME;
    }

    SlimTableFactory slimTableFactory = new SlimTableFactory();

    MultipleTestSystemFactory testSystemFactory = new MultipleTestSystemFactory(slimTableFactory, classLoader);

    FormatterFactory formatterFactory = new FormatterFactory();

    FitNesseContext context = new FitNesseContext(version,
          wikiPageFactory,
          rootPath,
          rootDirectoryName,
          maximumWorkers,
          versionsController,
          port,
          logger,
          testSystemFactory,
          testSystemListener,
          formatterFactory,
          properties,
          variableSource,
          theme);

    return context;
  }

  private void updateFitNesseProperties(FitNesseVersion version) {
    // Those variables are defined so they can be looked up for as wiki variables.
    if (rootPath != null) {
      properties.setProperty("FITNESSE_ROOTPATH", rootPath);
    }
    properties.setProperty("FITNESSE_PORT", Integer.toString(port));
    properties.setProperty("FITNESSE_VERSION", version.toString());

    // Some code may still expect the values in the properties file rather than their 'typed'
    // counterparts, so provide that:
    for (ConfigurationParameter parameter : ConfigurationParameter.values()) {
      String value = get(parameter);
      if (value != null) {
        properties.setProperty(parameter.getKey(), value);
      }
    }
  }

  private int getPort() {
    if (port == null) {
      if (get(COMMAND) != null) {
        return DEFAULT_COMMAND_PORT;
      } else {
        return DEFAULT_PORT;
      }
    } else {
      return port;
    }
  }

  public ContextConfigurator withRootPath(String rootPath) {
    this.rootPath = rootPath;
    return this;
  }

  public ContextConfigurator withParameter(String key, String value) {
    ConfigurationParameter parameter = ConfigurationParameter.byKey(key);
    if (parameter == null) {
      properties.put(key, value);
    } else {
      withParameter(parameter, value);
    }
    return this;
  }

  public ContextConfigurator withParameter(ConfigurationParameter parameter, String value) {
    switch (parameter) {
      case ROOT_PATH:
        rootPath = value;
        break;
      case ROOT_DIRECTORY:
        rootDirectoryName = value;
        break;
      case PORT:
        port = Integer.parseInt(value);
        break;
      case MAXIMUM_WORKERS:
        maximumWorkers = Integer.parseInt(value);
        break;
      case LISTEN:
        listenAddress = value;
        break;
      default:
        properties.setProperty(parameter.getKey(), value);
        break;
    }
    return this;
  }

  public ContextConfigurator withRootDirectoryName(String rootDirectoryName) {
    this.rootDirectoryName = rootDirectoryName;
    return this;
  }

  public ContextConfigurator withWikiPageFactory(WikiPageFactory wikiPageFactory) {
    this.wikiPageFactory = wikiPageFactory;
    return this;
  }

  public ContextConfigurator withPort(int port) {
    this.port = port;
    return this;
  }

  public ContextConfigurator withVersionsController(WikiFiles versionsController) {
    this.versionsController = versionsController;
    return this;
  }

  public ContextConfigurator withClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public String get(ConfigurationParameter parameter) {
    switch(parameter) {
      case ROOT_PATH:
        return rootPath;
      case ROOT_DIRECTORY:
        return rootDirectoryName;
      case PORT:
        return String.valueOf(port);
      case MAXIMUM_WORKERS:
        return String.valueOf(maximumWorkers);
      case LISTEN:
        return listenAddress == null ? DEFAULT_LISTEN : listenAddress;
      default:
        return properties.getProperty(parameter.getKey());
    }
  }
}
